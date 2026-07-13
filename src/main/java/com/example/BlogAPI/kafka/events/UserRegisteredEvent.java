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
public class UserRegisteredEvent extends BaseEvent {
    private Long userId;
    private String username;
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public UserRegisteredEvent(Long userId, String username, String email, LocalDateTime createdAt) {
        super(UUID.randomUUID().toString(), LocalDateTime.now(), "USER_REGISTERED");
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }
}