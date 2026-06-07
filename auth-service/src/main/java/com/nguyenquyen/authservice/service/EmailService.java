package com.nguyenquyen.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setTo(toEmail);
            message.setSubject("ZChat — Your Verification Code");
            message.setText(buildOtpEmailBody(otp));

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);

        } catch (Exception e) {
            // Log but don't throw — email failure shouldn't crash the request
            // User can request resend
            log.error("Failed to send OTP email to: {}. Error: {}", toEmail, e.getMessage());
        }
    }


    private String buildOtpEmailBody(String otp) {
        return """
            Dear User,

            Thank you for choosing ZChat.

            Your One-Time Password (OTP) for verification is:

                %s

            This OTP is valid for the next 5 minutes. For your security, please do not share this code with anyone.

            If you did not request this verification, you can safely ignore this email. No further action is required.

            For any assistance, please contact our support team.

            Warm regards,
            ZChat Support Team \s
            Secure. Fast. Connected.
           \s""".formatted(otp);
    }
}
