package com.example.BlogAPI.kafka.consumer;

import com.example.BlogAPI.kafka.events.CommentCreatedEvent;
import com.example.BlogAPI.kafka.events.CommentDeletedEvent;
import com.example.BlogAPI.kafka.events.CommentUpdatedEvent;
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
public class CommentEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.comment-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleCommentCreated(
            @Payload CommentCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received COMMENT_CREATED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: commentId={}, postId={}, author={}",
                event.getCommentId(), event.getPostId(), event.getUsername());
    }

    @KafkaListener(
            topics = "${kafka.topics.comment-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleCommentUpdated(
            @Payload CommentUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received COMMENT_UPDATED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: commentId={}, postId={}",
                event.getCommentId(), event.getPostId());
    }

    @KafkaListener(
            topics = "${kafka.topics.comment-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleCommentDeleted(
            @Payload CommentDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received COMMENT_DELETED from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);
        log.info("Event details: commentId={}", event.getCommentId());
    }
}
