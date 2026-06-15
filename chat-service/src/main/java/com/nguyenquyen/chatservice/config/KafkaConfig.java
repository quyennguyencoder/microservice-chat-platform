package com.nguyenquyen.chatservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.chat-events:chat-events}")
    private String chatEventsTopic;

    @Bean
    public NewTopic chatEventsTopic() {
        return TopicBuilder.name(chatEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}