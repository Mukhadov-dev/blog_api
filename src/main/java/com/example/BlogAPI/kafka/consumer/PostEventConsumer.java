package com.example.BlogAPI.kafka.consumer;

import com.example.BlogAPI.kafka.events.PostCreatedEvent;
import com.example.BlogAPI.kafka.events.PostDeletedEvent;
import com.example.BlogAPI.kafka.events.PostUpdatedEvent;
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
public class PostEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.post-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePostCreated(
            @Payload PostCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received POST_CREATED event from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: postId={}, title={}, author={}",
                event.getPostId(), event.getName(), event.getUsername());
    }

    @KafkaListener(
            topics = "${kafka.topics.post-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePostUpdated(
            @Payload PostUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received POST_UPDATED event from topic: {}, partition: {}, offset{}",
                topic, partition, offset);
        log.info("Event details: postId={}, title={}", event.getPostId(), event.getName());
    }

    @KafkaListener(
            topics = "${kafka.topics.post-deleted}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePostDeleted(
            @Payload PostDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received POST_DELETED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: postId={}", event.getPostId());
    }
}
