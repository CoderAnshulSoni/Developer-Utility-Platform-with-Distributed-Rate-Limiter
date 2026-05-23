package com.anshul.ratelimiter.algorithm;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
public class SlidingWindowAlgorithm implements RateLimitAlgorithm {

    @Override
    public RateLimitResult evaluate(String clientId, String endpoint, Policy policy, RedisTemplate<String, String> redisTemplate) {
        String key = "ratelimit:slidingwindow:" + clientId + ":" + endpoint;
        long now = System.currentTimeMillis();
        long windowStart = now - (policy.getWindowSeconds() * 1000L);

        // Remove all ZSet entries older than the sliding window start
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, (double) windowStart);

        // Count remaining entries in the sliding window
        Long currentRequestsLong = redisTemplate.opsForZSet().zCard(key);
        int currentRequests = currentRequestsLong != null ? currentRequestsLong.intValue() : 0;

        boolean allowed;
        int remainingRequests;
        long resetInSeconds = 0;

        if (currentRequests < policy.getMaxRequests()) {
            allowed = true;
            remainingRequests = policy.getMaxRequests() - currentRequests - 1;

            // Add the current request as a unique member with timestamp score
            String member = now + ":" + UUID.randomUUID().toString();
            redisTemplate.opsForZSet().add(key, member, (double) now);
            redisTemplate.expire(key, Duration.ofSeconds(policy.getWindowSeconds()));
        } else {
            allowed = false;
            remainingRequests = 0;

            // Retrieve the oldest request in the window to calculate resetInSeconds
            Set<ZSetOperations.TypedTuple<String>> oldestRange = redisTemplate.opsForZSet().rangeWithScores(key, 0, 0);
            if (oldestRange != null && !oldestRange.isEmpty()) {
                ZSetOperations.TypedTuple<String> oldestTuple = oldestRange.iterator().next();
                Double oldestScore = oldestTuple.getScore();
                if (oldestScore != null) {
                    long oldestTime = oldestScore.longValue();
                    long timeRemainingMs = (oldestTime + (policy.getWindowSeconds() * 1000L)) - now;
                    resetInSeconds = (long) Math.ceil(timeRemainingMs / 1000.0);
                }
            }
            if (resetInSeconds <= 0) {
                resetInSeconds = 1;
            }
        }

        return RateLimitResult.builder()
                .allowed(allowed)
                .remainingRequests(remainingRequests)
                .resetInSeconds(resetInSeconds)
                .message(allowed ? "Request allowed" : "Rate limit exceeded. Try again in " + resetInSeconds + " seconds.")
                .build();
    }
}
