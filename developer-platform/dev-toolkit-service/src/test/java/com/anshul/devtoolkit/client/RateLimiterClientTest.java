package com.anshul.devtoolkit.client;

import com.anshul.devtoolkit.client.RateLimiterClient.RateLimitResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = RateLimiterClient.class)
@TestPropertySource(properties = {
    "rate-limiter.enabled=true",
    "rate-limiter.base-url=http://localhost:8081"
})
public class RateLimiterClientTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private RestTemplate restTemplate;

    @Autowired
    private RateLimiterClient rateLimiterClient;

    @Test
    public void checkRateLimit_WhenAllowed_ShouldReturnAllowedTrue() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Remaining", "9");
        headers.set("X-RateLimit-Reset", "60");
        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", headers, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        RateLimitResult result = rateLimiterClient.checkRateLimit("client1", "/api/test");

        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(9, result.getRemainingRequests());
        assertEquals(60L, result.getRetryAfter());
    }

    @Test
    public void checkRateLimit_WhenBlocked_ShouldReturnAllowedFalse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Reset", "30");
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers,
                "{\"retryAfter\":30}".getBytes(),
                null
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        RateLimitResult result = rateLimiterClient.checkRateLimit("client1", "/api/test");

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemainingRequests());
        assertEquals(30L, result.getRetryAfter());
    }

    @Test
    public void checkRateLimit_WhenRateLimiterDown_ShouldFailOpen() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        RateLimitResult result = rateLimiterClient.checkRateLimit("client1", "/api/test");

        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(-1, result.getRemainingRequests());
        assertEquals(0L, result.getRetryAfter());
    }

    @Test
    public void checkRateLimit_ShouldTimeout_After200ms() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(50); // Simulate latency within the 200ms limit
                    throw new ResourceAccessException("Read timed out");
                });

        assertTimeoutPreemptively(Duration.ofMillis(250), () -> {
            RateLimitResult result = rateLimiterClient.checkRateLimit("client1", "/api/test");
            assertNotNull(result);
            assertTrue(result.isAllowed());
            assertEquals(-1, result.getRemainingRequests());
        });
    }

    @Nested
    @SpringBootTest(classes = RateLimiterClient.class)
    @TestPropertySource(properties = {
        "rate-limiter.enabled=false",
        "rate-limiter.base-url=http://localhost:8081"
    })
    class DisabledRateLimiterTest {

        @Autowired
        private RateLimiterClient disabledRateLimiterClient;

        @Autowired
        private RestTemplate restTemplate;

        @Test
        public void checkRateLimit_WhenDisabled_ShouldAlwaysReturnAllowed() {
            reset(restTemplate); // Ensure restTemplate mock state is fresh

            RateLimitResult result = disabledRateLimiterClient.checkRateLimit("client1", "/api/test");

            assertTrue(result.isAllowed());
            assertEquals(-1, result.getRemainingRequests());
            assertEquals(0L, result.getRetryAfter());
            
            // Verify RestTemplate is never called
            verifyNoInteractions(restTemplate);
        }
    }
}
