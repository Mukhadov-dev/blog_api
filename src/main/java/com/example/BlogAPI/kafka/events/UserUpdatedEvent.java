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
public class UserUpdatedEvent extends BaseEvent {
    private Long userId;
    private String username;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public UserUpdatedEvent(Long userId, String username, LocalDateTime updatedAt) {
        super(UUID.randomUUID().toString(), LocalDateTime.now(), "USER_UPDATED");
        this.userId = userId;
        this.username = username;
        this.updatedAt = updatedAt;
    }
}
