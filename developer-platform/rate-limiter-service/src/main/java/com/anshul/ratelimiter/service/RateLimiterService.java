package com.anshul.ratelimiter.service;

import com.anshul.ratelimiter.algorithm.SlidingWindowAlgorithm;
import com.anshul.ratelimiter.algorithm.TokenBucketAlgorithm;
import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final PolicyService policyService;
    private final TokenBucketAlgorithm tokenBucketAlgorithm;
    private final SlidingWindowAlgorithm slidingWindowAlgorithm;
    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitResult evaluate(String clientId, String endpoint) {
        Policy policy = policyService.getPolicy(endpoint);
        if (policy == null) {
            return RateLimitResult.builder()
                    .allowed(true)
                    .remainingRequests(-1)
                    .resetInSeconds(0)
                    .message("No limit policy defined for this endpoint")
                    .build();
        }

        if ("TOKEN_BUCKET".equalsIgnoreCase(policy.getAlgorithm())) {
            return tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);
        } else if ("SLIDING_WINDOW".equalsIgnoreCase(policy.getAlgorithm())) {
            return slidingWindowAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);
        }

        return RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(-1)
                .resetInSeconds(0)
                .message("Unsupported rate limiting algorithm: " + policy.getAlgorithm())
                .build();
    }
}
