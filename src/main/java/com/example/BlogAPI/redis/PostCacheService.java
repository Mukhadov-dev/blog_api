package com.example.BlogAPI.redis;

import com.example.BlogAPI.post.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "post:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public void put(PostResponse post) {
        String key = PREFIX + post.getId();
        redisTemplate.opsForValue().set(key, post, TTL);
        log.info("Cache PUT: {}", key);
    }

    public PostResponse get(Long postId) {
        String key = PREFIX + postId;
        PostResponse post = (PostResponse) redisTemplate.opsForValue().get(key);
        if (post != null) {
            log.info("Cache HIT: {}", key);
        } else {
            log.info("Cache MISS: {}", key);
        }
        return post;
    }

    public void evict(Long postId) {
        String key = PREFIX + postId;
        redisTemplate.delete(key);
        log.info("Cache EVICT: {}", key);
    }
}