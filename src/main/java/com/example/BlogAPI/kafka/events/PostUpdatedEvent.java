package com.example.BlogAPI.kafka.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostUpdatedEvent extends BaseEvent {
    private Long postId;
    private String name;
    private String content;
    private Long updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public PostUpdatedEvent(Long postId, String name, String content, Long updatedBy, LocalDateTime updatedAt) {
        super(UUID.randomUUID().toString(), LocalDateTime.now(), "POST_UPDATED");
        this.postId = postId;
        this.name = name;
        this.content = content;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}
