package com.nguyenquyen.conversationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    // ─── Topics ──────────────────────────────────────────────────────────────

    @Bean
    public NewTopic chatEventsTopic() {
        return TopicBuilder.name("chat-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic groupEventsTopic() {
        return TopicBuilder.name("group-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ─── String Consumer Factory (for chat-events, raw JSON) ─────────────────

}
