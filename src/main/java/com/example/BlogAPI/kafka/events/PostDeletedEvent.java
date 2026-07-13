package com.example.BlogAPI.kafka.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostDeletedEvent extends BaseEvent {
    private Long postId;

    public PostDeletedEvent(Long postId) {
        super(UUID.randomUUID().toString(), LocalDateTime.now(), "POST_DELETED");
        this.postId = postId;
    }
}
