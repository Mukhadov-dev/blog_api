package com.example.BlogAPI.kafka.listeners;

import com.example.BlogAPI.kafka.events.PostCreatedEvent;
import com.example.BlogAPI.kafka.events.PostDeletedEvent;
import com.example.BlogAPI.kafka.events.PostUpdatedEvent;
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
public class PostEventListener {

    private final KafkaProducerService kafkaProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("TX committed -> sending POST_CREATED to Kafka: postId={}", event.getPostId());
        kafkaProducer.sendPostCreatedEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostUpdated(PostUpdatedEvent event) {
        log.info("TX committed -> sending POST_UPDATED to Kafka: postId={}", event.getPostId());
        kafkaProducer.sendPostUpdatedEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostDeleted(PostDeletedEvent event) {
        log.info("TX commited -> sending POST_DELETED to Kafka: postId={}", event.getPostId());
        kafkaProducer.sendPostDeletedEvent(event);
    }
}
