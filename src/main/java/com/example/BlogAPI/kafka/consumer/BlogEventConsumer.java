package com.example.BlogAPI.kafka.consumer;

import com.example.BlogAPI.kafka.events.*;
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
public class BlogEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.post-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
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

        processPostCreated(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.post-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handlePostUpdated(@Payload PostUpdatedEvent event) {
        log.info("Received POST_UPDATED event: postId={}, title={}",
                event.getPostId(), event.getName());

        processPostUpdated(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.comment-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleCommentCreated(@Payload CommentCreatedEvent event) {
        log.info("Received COMMENT_CREATED event: commentId={}, postId={}, author={}",
                event.getCommentId(), event.getPostId(), event.getUsername());

        processCommentCreated(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.comment-updated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleCommentUpdated(@Payload CommentUpdatedEvent event) {
        log.info("Received COMMENT_UPDATED event: commentId={}, postId={}",
                event.getCommentId(), event.getPostId());

        processCommentUpdated(event);
    }

    @KafkaListener(
            topics = "${kafka.topics.user-registered}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleUserRegistered(@Payload UserRegisteredEvent event) {
        log.info("Received USER_REGISTERED event: userId={}, username={}, email={}",
                event.getUserId(), event.getUsername(), event.getEmail());

        processUserRegistered(event);
    }

    // Бизнес-логика обработки событий

    private void processPostCreated(PostCreatedEvent event) {
        log.info("Processing: Sending notifications to subscribers about new post '{}'",
                event.getName());

        // TODO: Отправка уведомлений подписчикам
        // notificationService.notifySubscribers(event);

        log.info("Processing: Indexing post {} in search engine", event.getPostId());

        // TODO: Индексация в Elasticsearch
        // searchIndexService.indexPost(event);
    }

    private void processPostUpdated(PostUpdatedEvent event) {
        log.info("Processing: Invalidating cache for post {}", event.getPostId());

        // TODO: Инвалидация кеша
        // cacheService.invalidate("post:" + event.getPostId());

        log.info("Processing: Updating search index for post {}", event.getPostId());

        // TODO: Обновление индекса
        // searchIndexService.updatePost(event);
    }

    private void processCommentCreated(CommentCreatedEvent event) {
        log.info("Processing: Notifying post author about new comment on '{}'",
                event.getPostName());

        // TODO: Уведомление автора поста
        // notificationService.notifyPostAuthor(event);

        log.info("Processing: Running content moderation for comment {}",
                event.getCommentId());

        // TODO: Модерация контента
        // moderationService.moderate(event);
    }

    private void processCommentUpdated(CommentUpdatedEvent event) {
        log.info("Processing: Invalidating cache for comment {}", event.getCommentId());

        // TODO: Инвалидация кеша
        // cacheService.invalidate("post:" + event.getPostId());
    }

    private void processUserRegistered(UserRegisteredEvent event) {
        log.info("Processing: Sending welcome email to {}", event.getEmail());

        // TODO: Отправка welcome email
        // emailService.sendWelcomeEmail(event.getEmail(), event.getUsername());

        log.info("Processing: Creating user profile for {}", event.getUsername());

        // TODO: Создание профиля в других сервисах
        // profileService.createProfile(event);
    }
}
