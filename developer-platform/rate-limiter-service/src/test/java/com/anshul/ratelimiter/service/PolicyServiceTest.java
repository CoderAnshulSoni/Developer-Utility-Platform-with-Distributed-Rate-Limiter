package com.anshul.ratelimiter.service;

import com.anshul.ratelimiter.model.Policy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PolicyServiceTest {

    @InjectMocks
    private PolicyService policyService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    private Policy testPolicy;

    @BeforeEach
    public void setUp() {
        testPolicy = Policy.builder()
                .endpoint("/api/sql/analyse")
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("TOKEN_BUCKET")
                .build();
    }

    @Test
    public void savePolicy_ShouldStoreCorrectKeyInRedis() throws Exception {
        String expectedJson = "{\"endpoint\":\"/api/sql/analyse\",\"maxRequests\":10,\"windowSeconds\":60,\"algorithm\":\"TOKEN_BUCKET\"}";
        when(objectMapper.writeValueAsString(any(Policy.class))).thenReturn(expectedJson);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        policyService.savePolicy(testPolicy);

        verify(valueOperations, times(1)).set(eq("policy:/api/sql/analyse"), eq(expectedJson));
    }

    @Test
    public void getPolicy_WhenExists_ShouldReturnPolicy() throws Exception {
        String json = "{\"endpoint\":\"/api/sql/analyse\",\"maxRequests\":10,\"windowSeconds\":60,\"algorithm\":\"TOKEN_BUCKET\"}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq("policy:/api/sql/analyse"))).thenReturn(json);
        when(objectMapper.readValue(eq(json), eq(Policy.class))).thenReturn(testPolicy);

        Policy result = policyService.getPolicy("/api/sql/analyse");

        assertNotNull(result);
        assertEquals(testPolicy.getEndpoint(), result.getEndpoint());
        assertEquals(testPolicy.getMaxRequests(), result.getMaxRequests());
        assertEquals(testPolicy.getWindowSeconds(), result.getWindowSeconds());
        assertEquals(testPolicy.getAlgorithm(), result.getAlgorithm());
    }

    @Test
    public void getPolicy_WhenNotExists_ShouldReturnNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(eq("policy:/api/unknown"))).thenReturn(null);

        Policy result = policyService.getPolicy("/api/unknown");

        assertNull(result);
    }

    @Test
    public void savePolicy_WithNullEndpoint_ShouldThrowException() {
        Policy policyWithNullEndpoint = Policy.builder()
                .endpoint(null)
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("TOKEN_BUCKET")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalArgumentException("Endpoint cannot be null"))
                .when(valueOperations).set(eq("policy:null"), any());

        assertThrows(IllegalArgumentException.class, () -> {
            policyService.savePolicy(policyWithNullEndpoint);
        });
    }

    @Test
    public void getAllPolicies_ShouldReturnAllStoredPolicies() throws Exception {
        Set<String> keys = Set.of("policy:/api/1", "policy:/api/2", "policy:/api/3");
        when(redisTemplate.keys(eq("policy:*"))).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(eq("policy:/api/1"))).thenReturn("json1");
        when(valueOperations.get(eq("policy:/api/2"))).thenReturn("json2");
        when(valueOperations.get(eq("policy:/api/3"))).thenReturn("json3");

        Policy p1 = Policy.builder().endpoint("/api/1").build();
        Policy p2 = Policy.builder().endpoint("/api/2").build();
        Policy p3 = Policy.builder().endpoint("/api/3").build();

        when(objectMapper.readValue(eq("json1"), eq(Policy.class))).thenReturn(p1);
        when(objectMapper.readValue(eq("json2"), eq(Policy.class))).thenReturn(p2);
        when(objectMapper.readValue(eq("json3"), eq(Policy.class))).thenReturn(p3);

        List<Policy> result = policyService.getAllPolicies();

        assertNotNull(result);
        assertEquals(3, result.size());
    }
}
