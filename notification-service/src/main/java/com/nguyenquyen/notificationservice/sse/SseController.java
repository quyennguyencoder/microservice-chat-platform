package com.nguyenquyen.notificationservice.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.nguyenquyen.common.util.SecurityUtils;

@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseEmitterManager emitterManager;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        String userId = SecurityUtils.requireCurrentUserId();
        log.info("SSE connection request from user: {}", userId);
        return emitterManager.createEmitter(userId);
    }
}
