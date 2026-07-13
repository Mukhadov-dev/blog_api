package com.example.BlogAPI.kafka.listeners;

import com.example.BlogAPI.kafka.events.UserDeletedEvent;
import com.example.BlogAPI.kafka.events.UserRegisteredEvent;
import com.example.BlogAPI.kafka.events.UserUpdatedEvent;
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
public class UserEventListener {

    private final KafkaProducerService kafkaProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("TX committed -> sending USER_REGISTERED to Kafka: userId={}", event.getUserId());
        kafkaProducer.sendUserRegisteredEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("TX committed -> sending USER_UPDATED to Kafka: userId={}", event.getUserId());
        kafkaProducer.sendUserUpdatedEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeleted(UserDeletedEvent event) {
        log.info("TX committed -> sending USER_DELETED to Kafka: userId={}", event.getUserId());
        kafkaProducer.sendUserDeletedEvent(event);
    }
}
