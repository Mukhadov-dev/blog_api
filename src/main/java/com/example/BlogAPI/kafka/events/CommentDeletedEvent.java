package com.example.BlogAPI.kafka.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommentDeletedEvent extends BaseEvent {
    private Long commentId;

    public CommentDeletedEvent(Long commentId) {
        super(UUID.randomUUID().toString(), LocalDateTime.now(), "COMMENT_DELETED");
        this.commentId = commentId;
    }
}
