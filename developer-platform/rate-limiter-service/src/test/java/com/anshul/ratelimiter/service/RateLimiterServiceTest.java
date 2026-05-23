package com.anshul.ratelimiter.service;

import com.anshul.ratelimiter.algorithm.SlidingWindowAlgorithm;
import com.anshul.ratelimiter.algorithm.TokenBucketAlgorithm;
import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimiterServiceTest {

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @Mock
    private PolicyService policyService;

    @Mock
    private TokenBucketAlgorithm tokenBucketAlgorithm;

    @Mock
    private SlidingWindowAlgorithm slidingWindowAlgorithm;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private Policy tokenBucketPolicy;
    private Policy slidingWindowPolicy;

    @BeforeEach
    public void setUp() {
        tokenBucketPolicy = Policy.builder()
                .endpoint("/api/test")
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("TOKEN_BUCKET")
                .build();

        slidingWindowPolicy = Policy.builder()
                .endpoint("/api/test")
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("SLIDING_WINDOW")
                .build();
    }

    @Test
    public void evaluate_WhenNoPolicyExists_ShouldAllowRequest() {
        when(policyService.getPolicy(anyString())).thenReturn(null);

        RateLimitResult result = rateLimiterService.evaluate("client1", "/api/test");

        assertTrue(result.isAllowed());
        verify(policyService, times(1)).getPolicy("/api/test");
        verifyNoInteractions(tokenBucketAlgorithm);
        verifyNoInteractions(slidingWindowAlgorithm);
    }

    @Test
    public void evaluate_WithTokenBucketPolicy_ShouldUseTokenBucket() {
        when(policyService.getPolicy("/api/test")).thenReturn(tokenBucketPolicy);
        
        RateLimitResult expectedResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(9)
                .resetInSeconds(60)
                .build();
                
        when(tokenBucketAlgorithm.evaluate(eq("client1"), eq("/api/test"), eq(tokenBucketPolicy), eq(redisTemplate)))
                .thenReturn(expectedResult);

        RateLimitResult result = rateLimiterService.evaluate("client1", "/api/test");

        assertNotNull(result);
        assertTrue(result.isAllowed());
        verify(tokenBucketAlgorithm, times(1)).evaluate(eq("client1"), eq("/api/test"), eq(tokenBucketPolicy), eq(redisTemplate));
        verifyNoInteractions(slidingWindowAlgorithm);
    }

    @Test
    public void evaluate_WithSlidingWindowPolicy_ShouldUseSlidingWindow() {
        when(policyService.getPolicy("/api/test")).thenReturn(slidingWindowPolicy);

        RateLimitResult expectedResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(9)
                .resetInSeconds(60)
                .build();

        when(slidingWindowAlgorithm.evaluate(eq("client1"), eq("/api/test"), eq(slidingWindowPolicy), eq(redisTemplate)))
                .thenReturn(expectedResult);

        RateLimitResult result = rateLimiterService.evaluate("client1", "/api/test");

        assertNotNull(result);
        assertTrue(result.isAllowed());
        verify(slidingWindowAlgorithm, times(1)).evaluate(eq("client1"), eq("/api/test"), eq(slidingWindowPolicy), eq(redisTemplate));
        verifyNoInteractions(tokenBucketAlgorithm);
    }

    @Test
    public void evaluate_WhenBlocked_ShouldReturnBlockedResult() {
        when(policyService.getPolicy("/api/test")).thenReturn(tokenBucketPolicy);

        RateLimitResult blockedResult = RateLimitResult.builder()
                .allowed(false)
                .remainingRequests(0)
                .resetInSeconds(45)
                .build();

        when(tokenBucketAlgorithm.evaluate(eq("client1"), eq("/api/test"), eq(tokenBucketPolicy), eq(redisTemplate)))
                .thenReturn(blockedResult);

        RateLimitResult result = rateLimiterService.evaluate("client1", "/api/test");

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemainingRequests());
    }

    @Test
    public void evaluate_WhenAllowed_ShouldReturnRemainingCount() {
        when(policyService.getPolicy("/api/test")).thenReturn(tokenBucketPolicy);

        RateLimitResult allowedResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(7)
                .resetInSeconds(30)
                .build();

        when(tokenBucketAlgorithm.evaluate(eq("client1"), eq("/api/test"), eq(tokenBucketPolicy), eq(redisTemplate)))
                .thenReturn(allowedResult);

        RateLimitResult result = rateLimiterService.evaluate("client1", "/api/test");

        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(7, result.getRemainingRequests());
    }
}
