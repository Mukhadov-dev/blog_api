package com.example.BlogAPI.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
    @Value("${kafka.topics.post-created}")
    private String postCreatedTopic;

    @Value("${kafka.topics.post-updated}")
    private String postUpdatedTopic;

    @Value("${kafka.topics.post-deleted}")
    private String postDeletedTopic;

    @Value("${kafka.topics.comment-created}")
    private String commentCreatedTopic;

    @Value("${kafka.topics.comment-updated}")
    private String commentUpdatedTopic;

    @Value("${kafka.topics.comment-deleted}")
    private String commentDeletedTopic;

    @Value("${kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${kafka.topics.user-deleted}")
    private String userDeletedTopic;

    @Bean
    public NewTopic postCreatedTopic() {
        return TopicBuilder.name(postCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic postUpdatedTopic() {
        return TopicBuilder.name(postUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic postDeletedTopic() {
        return TopicBuilder.name(postDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic commentCreatedTopic() {
        return TopicBuilder.name(commentCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic commentUpdatedTopic() {
        return TopicBuilder.name(commentUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic commentDeletedTopic() {
        return TopicBuilder.name(commentDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(userRegisteredTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return TopicBuilder.name(userUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder.name(userDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(1000L, 3L)
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
