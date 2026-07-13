package com.example.BlogAPI.kafka.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDeletedEvent extends BaseEvent {
    private Long userId;

    public UserDeletedEvent(Long userId) {
        super(UUID.randomUUID().toString(), LocalDateTime.now(), "USER_DELETED");
        this.userId = userId;
    }
}
