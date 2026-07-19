package com.example.BlogAPI.post;

import com.example.BlogAPI.user.User;
import com.example.BlogAPI.user.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostStatsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostsRepository postsRepository;
    private final UsersRepository usersRepository;

    private static final String VIEWS_KEY = "post:views:";
    private static final String LIKES_KEY = "post:likes:";

    public Long incrementViews(Long postId) {
        String key = VIEWS_KEY + postId;
        Long views = redisTemplate.opsForValue().increment(key);

        if (views != null && views == 1) {
            redisTemplate.expire(key, Duration.ofDays(30));
        }

        return views != null ? views : 0L;
    }

    public Long getViews(Long postId) {
        Object value = redisTemplate.opsForValue().get(VIEWS_KEY + postId);
        if (value == null) return 0L;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid views value for post {}: {}", postId, value);
            return 0L;
        }
    }

    public Map<String, Object> like(Long postId, Authentication authentication) {
        User user = usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String key = LIKES_KEY + postId;
        Long result = redisTemplate.opsForSet().add(key, user.getId().toString());
        boolean added = result != null && result > 0;

        log.info("User {} liked post {}: {}", user.getUsername(), postId, added);

        return Map.of("liked", added, "likes", getLikes(postId));
    }

    public Map<String, Object> unlike(Long postId, Authentication authentication) {
        User user = usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String key = LIKES_KEY + postId;

        Long result = redisTemplate.opsForSet().remove(key, user.getId().toString());
        boolean removed = result != null && result > 0;

        log.info("User {} unliked post {}: {}", user.getUsername(), postId, removed);

        return Map.of("unliked", removed, "likes", getLikes(postId));
    }

    public Long getLikes(Long postId) {
        Long count = redisTemplate.opsForSet().size(LIKES_KEY + postId);
        return count != null ? count : 0L;
    }

    public boolean isLikedByUser(Long postId, Authentication authentication) {
        User user = usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User with username " + authentication.getName() + " not found"));

        Boolean isMember = redisTemplate.opsForSet().isMember(LIKES_KEY + postId, user.getId().toString());

        return Boolean.TRUE.equals(isMember);
    }

    @Transactional
    @Scheduled(fixedDelay = 10000)
    public void syncStatsToDatabase() {
        log.info("Syncing post stats to database...");

        try {
            Set<String> viewKeys = redisTemplate.keys(VIEWS_KEY + "*");
            if (viewKeys == null || viewKeys.isEmpty()) {
                log.info("No stats to sync");
                return;
            }

            int synced = 0;
            for (String key : viewKeys) {
                Long postId = extractIdFromKey(key, VIEWS_KEY);
                if (postId == null) continue;
                postsRepository.updateStats(postId, getViews(postId), getLikes(postId));
                synced++;
            }

            log.info("Stats sync complete: {} posts updated", synced);
        } catch (Exception e) {
            log.error("Stats sync failed: {}", e.getMessage(), e);
        }
    }

    private Long extractIdFromKey(String key, String prefix) {
        try {
            return Long.parseLong(key.replace(prefix, ""));
        } catch (NumberFormatException e) {
            log.warn("Invalid key format: {}", key);
            return null;
        }
    }
}
