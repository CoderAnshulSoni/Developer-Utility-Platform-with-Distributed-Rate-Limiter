package com.anshul.ratelimiter.algorithm;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class TokenBucketAlgorithm implements RateLimitAlgorithm {

    @Override
    public RateLimitResult evaluate(String clientId, String endpoint, Policy policy, RedisTemplate<String, String> redisTemplate) {
        String key = "ratelimit:tokenbucket:" + clientId + ":" + endpoint;
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);

        double currentTokens;
        long lastRefillTime;
        long now = System.currentTimeMillis();

        String tokensVal = (String) fields.get("tokens");
        String lastRefillTimeVal = (String) fields.get("lastRefillTime");

        if (tokensVal == null || lastRefillTimeVal == null) {
            // Initial state: full bucket
            currentTokens = policy.getMaxRequests();
            lastRefillTime = now;
        } else {
            try {
                currentTokens = Double.parseDouble(tokensVal);
                lastRefillTime = Long.parseLong(lastRefillTimeVal);
            } catch (NumberFormatException e) {
                currentTokens = policy.getMaxRequests();
                lastRefillTime = now;
            }
        }

        // Calculate elapsed time and refilled tokens
        long elapsed = Math.max(0, now - lastRefillTime);
        double refillRate = (double) policy.getMaxRequests() / (policy.getWindowSeconds() * 1000.0);
        double newTokens = Math.min(policy.getMaxRequests(), currentTokens + (elapsed * refillRate));

        boolean allowed;
        int remainingRequests;
        long resetInSeconds = 0;

        if (newTokens >= 1.0) {
            newTokens -= 1.0;
            allowed = true;
            remainingRequests = (int) Math.floor(newTokens);

            // Update Redis hash
            Map<String, String> updateMap = new HashMap<>();
            updateMap.put("tokens", String.valueOf(newTokens));
            updateMap.put("lastRefillTime", String.valueOf(now));

            redisTemplate.opsForHash().putAll(key, updateMap);
            redisTemplate.expire(key, Duration.ofSeconds(policy.getWindowSeconds() * 2L));
        } else {
            allowed = false;
            remainingRequests = (int) Math.floor(newTokens);
            double tokensNeeded = 1.0 - newTokens;
            double refillRatePerSec = (double) policy.getMaxRequests() / policy.getWindowSeconds();
            resetInSeconds = (long) Math.ceil(tokensNeeded / refillRatePerSec);
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
