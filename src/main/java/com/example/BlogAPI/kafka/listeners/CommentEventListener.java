package com.example.BlogAPI.kafka.listeners;

import com.example.BlogAPI.kafka.events.CommentCreatedEvent;
import com.example.BlogAPI.kafka.events.CommentDeletedEvent;
import com.example.BlogAPI.kafka.events.CommentUpdatedEvent;
import com.example.BlogAPI.kafka.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommentEventListener {

    private final KafkaProducerService kafkaProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("TX committed -> sending COMMENT_CREATED to Kafka: commentId={}", event.getCommentId());
        kafkaProducer.sendCommentCreatedEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentUpdated(CommentUpdatedEvent event) {
        log.info("TX committed -> sending COMMENT_UPDATED to Kafka: commentId={}", event.getCommentId());
        kafkaProducer.sendCommentUpdatedEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentDeleted(CommentDeletedEvent event) {
        log.info("TX committed -> sending COMMENT_DELETED to Kafka: commentId={}", event.getCommentId());
        kafkaProducer.sendCommentDeletedEvent(event);
    }
}
