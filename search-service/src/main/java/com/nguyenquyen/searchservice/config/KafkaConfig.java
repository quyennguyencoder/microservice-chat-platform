package com.nguyenquyen.searchservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.chat-events:chat-events}")
    private String chatEventsTopic;

    @Bean
    public RecordMessageConverter converter() {
        return new JsonMessageConverter();
    }

    @Value("${kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    @Bean
    public NewTopic chatEventsTopic() {
        return TopicBuilder.name(chatEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(userEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}