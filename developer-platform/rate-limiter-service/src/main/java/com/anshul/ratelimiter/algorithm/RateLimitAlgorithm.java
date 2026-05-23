package com.anshul.ratelimiter.algorithm;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import org.springframework.data.redis.core.RedisTemplate;

public interface RateLimitAlgorithm {
    RateLimitResult evaluate(String clientId, String endpoint, Policy policy, RedisTemplate<String, String> redisTemplate);
}
