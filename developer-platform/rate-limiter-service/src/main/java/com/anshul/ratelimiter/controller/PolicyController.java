package com.anshul.ratelimiter.controller;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.model.RateLimitResult;
import com.anshul.ratelimiter.service.PolicyService;
import com.anshul.ratelimiter.service.RateLimiterService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/api/policies")
    public ResponseEntity<Void> savePolicy(@RequestBody Policy policy) {
        if (policy.getEndpoint() != null && !policy.getEndpoint().startsWith("/")) {
            policy.setEndpoint("/" + policy.getEndpoint());
        }
        policyService.savePolicy(policy);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/policies/{*endpoint}")
    public ResponseEntity<Policy> getPolicy(@PathVariable String endpoint) {
        String normalized = normalize(endpoint);
        Policy policy = policyService.getPolicy(normalized);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    @GetMapping("/api/policies")
    public ResponseEntity<List<Policy>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @DeleteMapping("/api/policies/{*endpoint}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String endpoint) {
        String normalized = normalize(endpoint);
        policyService.deletePolicy(normalized);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/validate")
    public ResponseEntity<RateLimitResult> validate(@RequestBody ValidateRequest request) {
        RateLimitResult result = rateLimiterService.evaluate(request.getClientId(), request.getEndpoint());
        if (result.isAllowed()) {
            return ResponseEntity.ok()
                    .header("X-RateLimit-Remaining", String.valueOf(result.getRemainingRequests()))
                    .header("X-RateLimit-Reset", String.valueOf(result.getResetInSeconds()))
                    .body(result);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Reset", String.valueOf(result.getResetInSeconds()))
                    .body(result);
        }
    }

    private String normalize(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        if (!endpoint.startsWith("/")) {
            return "/" + endpoint;
        }
        return endpoint;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateRequest {
        private String clientId;
        private String endpoint;
    }
}
