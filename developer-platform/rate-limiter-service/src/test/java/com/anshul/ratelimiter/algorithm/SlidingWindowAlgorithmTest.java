package com.anshul.ratelimiter.algorithm;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SlidingWindowAlgorithmTest {

    @InjectMocks
    private SlidingWindowAlgorithm slidingWindowAlgorithm;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

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
                .algorithm("SLIDING_WINDOW")
                .build();
    }

    @Test
    public void firstRequest_ShouldBeAllowed() {
        // Setup: ZSet returns count = 0 (empty window)
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // Execute
        RateLimitResult result = slidingWindowAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertTrue(result.isAllowed());
        assertEquals(policy.getMaxRequests() - 1, result.getRemainingRequests());
        verify(zSetOperations, times(1)).add(anyString(), anyString(), anyDouble());
    }

    @Test
    public void requestUnderLimit_ShouldBeAllowed() {
        // Setup: ZSet count = 5
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(5L);

        // Execute
        RateLimitResult result = slidingWindowAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertTrue(result.isAllowed());
        assertEquals(4, result.getRemainingRequests());
        verify(zSetOperations, times(1)).add(anyString(), anyString(), anyDouble());
    }

    @Test
    public void requestAtExactLimit_ShouldBeBlocked() {
        // Setup: ZSet count = maxRequests (10)
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(10L);

        // Mock oldest tuple score for calculation of resetInSeconds
        @SuppressWarnings("unchecked")
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getScore()).thenReturn((double) (System.currentTimeMillis() - 30000));
        when(zSetOperations.rangeWithScores(anyString(), eq(0L), eq(0L))).thenReturn(Collections.singleton(tuple));

        // Execute
        RateLimitResult result = slidingWindowAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemainingRequests());
    }

    @Test
    public void expiredEntries_ShouldBeRemovedBeforeCounting() {
        // Setup: standard allowed state
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);

        // Execute
        slidingWindowAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Verify: removeRangeByScore() is called with correct windowStart timestamp
        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOperations, times(1)).removeRangeByScore(anyString(), eq(0.0), scoreCaptor.capture());
        
        double capturedScore = scoreCaptor.getValue();
        long expectedWindowStart = System.currentTimeMillis() - (policy.getWindowSeconds() * 1000L);
        // Allow a 1000 ms threshold for runtime execution differences
        assertTrue(Math.abs(capturedScore - expectedWindowStart) < 1000);
    }

    @Test
    public void requestOverLimit_ShouldReturnRetryAfter() {
        // Setup: ZSet count = maxRequests (10)
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(10L);

        // Mock oldest tuple score (e.g. created 30 seconds ago, in a 60 second window)
        @SuppressWarnings("unchecked")
        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getScore()).thenReturn((double) (System.currentTimeMillis() - 30000));
        when(zSetOperations.rangeWithScores(anyString(), eq(0L), eq(0L))).thenReturn(Collections.singleton(tuple));

        // Execute
        RateLimitResult result = slidingWindowAlgorithm.evaluate(clientId, endpoint, policy, redisTemplate);

        // Assert
        assertFalse(result.isAllowed());
        assertTrue(result.getResetInSeconds() > 0);
        // Expecting around 30 seconds
        assertEquals(30, result.getResetInSeconds());
    }
}
