package com.example.BlogAPI.kafka.consumer;

import com.example.BlogAPI.kafka.events.UserDeletedEvent;
import com.example.BlogAPI.kafka.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleUserRegistered(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received USER_REGISTERED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: userId={}, username={}", event.getUserId(), event.getUsername());

    }

    @KafkaListener(
            topics = "${kafka.topics.user-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleUserUpdated(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received USER_UPDATED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: userId={}, username={}", event.getUserId(), event.getUsername());

    }

    @KafkaListener(
            topics = "${kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleUserDeleted(
            @Payload UserDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received USER_DELETED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: userId={}", event.getUserId());
    }
}