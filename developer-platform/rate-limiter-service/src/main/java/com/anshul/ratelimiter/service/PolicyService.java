package com.anshul.ratelimiter.service;

import com.anshul.ratelimiter.model.Policy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void savePolicy(Policy policy) {
        String key = "policy:" + policy.getEndpoint();
        try {
            String json = objectMapper.writeValueAsString(policy);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Policy for endpoint: {}", policy.getEndpoint(), e);
            throw new RuntimeException("Error saving policy", e);
        }
    }

    public Policy getPolicy(String endpoint) {
        String key = "policy:" + endpoint;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Policy.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize Policy for endpoint: {}", endpoint, e);
            return null;
        }
    }

    public List<Policy> getAllPolicies() {
        List<Policy> policies = new ArrayList<>();
        Set<String> keys = redisTemplate.keys("policy:*");
        if (keys != null) {
            for (String key : keys) {
                String json = redisTemplate.opsForValue().get(key);
                if (json != null) {
                    try {
                        Policy policy = objectMapper.readValue(json, Policy.class);
                        policies.add(policy);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize Policy for key: {}", key, e);
                    }
                }
            }
        }
        return policies;
    }

    public void deletePolicy(String endpoint) {
        String key = "policy:" + endpoint;
        redisTemplate.delete(key);
    }
}
