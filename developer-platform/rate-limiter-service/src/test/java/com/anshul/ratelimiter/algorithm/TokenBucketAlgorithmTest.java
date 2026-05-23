package com.anshul.ratelimiter.algorithm;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenBucketAlgorithmTest {

    @InjectMocks
    private TokenBucketAlgorithm tokenBucketAlgorithm;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private Policy policy;
    private String clientId;
    private String endpoint;

    @BeforeEach
    public void setUp() {
        clientId = "test-client";
        endpoint = "/api/test";
        policy = Policy.builder()
                .clientId(clientId)
                .endpoint(endpoint)
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("TOKEN_BUCKET")
                .build();
    }

    @Test
    public void firstRequest_ShouldBeAllowed() {
        // Setup: Redis returns empty map (no existing state)
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(new HashMap<>());

        // Execute
        RateLimitResult result = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertTrue(result.isAllowed());
        assertEquals(policy.getMaxRequests() - 1, result.getRemainingRequests());
        verify(hashOperations, times(1)).putAll(anyString(), anyMap());
    }

    @Test
    public void requestWithFullBucket_ShouldBeAllowed() {
        // Setup: Redis returns tokens = maxRequests, lastRefillTime = current time
        long now = System.currentTimeMillis();
        Map<Object, Object> fields = new HashMap<>();
        fields.put("tokens", String.valueOf(policy.getMaxRequests()));
        fields.put("lastRefillTime", String.valueOf(now));

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(fields);

        // Execute
        RateLimitResult result = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertTrue(result.isAllowed());
    }

    @Test
    public void requestWithEmptyBucket_ShouldBeBlocked() {
        // Setup: Redis returns tokens = 0, lastRefillTime = current time
        long now = System.currentTimeMillis();
        Map<Object, Object> fields = new HashMap<>();
        fields.put("tokens", "0");
        fields.put("lastRefillTime", String.valueOf(now));

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(fields);

        // Execute
        RateLimitResult result = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemainingRequests());
    }

    @Test
    public void requestAfterTimeElapsed_ShouldRefillTokens() {
        // Setup: Redis returns tokens = 0, lastRefillTime = 60 seconds ago
        // maxRequests = 10, windowSeconds = 60
        long sixtySecondsAgo = System.currentTimeMillis() - 60000;
        Map<Object, Object> fields = new HashMap<>();
        fields.put("tokens", "0");
        fields.put("lastRefillTime", String.valueOf(sixtySecondsAgo));

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString())).thenReturn(fields);

        // Execute
        RateLimitResult result = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertTrue(result.isAllowed());
        // Verify refilled tokens: 0 + (60000 * (10 / 60000.0)) = 10 tokens
        // After decrementing 1 token for the request: 9 remaining
        assertEquals(9, result.getRemainingRequests());
    }

    @Test
    public void multipleRequests_ShouldDecrementTokens() {
        // Setup: Call evaluate() 3 times in sequence
        // Each time Redis returns one less token: 2, then 1, then 0
        long now = System.currentTimeMillis();

        Map<Object, Object> firstCall = new HashMap<>();
        firstCall.put("tokens", "2");
        firstCall.put("lastRefillTime", String.valueOf(now));

        Map<Object, Object> secondCall = new HashMap<>();
        secondCall.put("tokens", "1");
        secondCall.put("lastRefillTime", String.valueOf(now));

        Map<Object, Object> thirdCall = new HashMap<>();
        thirdCall.put("tokens", "0");
        thirdCall.put("lastRefillTime", String.valueOf(now));

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(anyString()))
                .thenReturn(firstCall)
                .thenReturn(secondCall)
                .thenReturn(thirdCall);

        // Call 1: tokens = 2
        RateLimitResult result1 = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);
        assertTrue(result1.isAllowed());
        assertEquals(1, result1.getRemainingRequests());

        // Call 2: tokens = 1
        RateLimitResult result2 = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);
        assertTrue(result2.isAllowed());
        assertEquals(0, result2.getRemainingRequests());

        // Call 3: tokens = 0
        RateLimitResult result3 = tokenBucketAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);
        assertFalse(result3.isAllowed());
        assertEquals(0, result3.getRemainingRequests());
    }
}
