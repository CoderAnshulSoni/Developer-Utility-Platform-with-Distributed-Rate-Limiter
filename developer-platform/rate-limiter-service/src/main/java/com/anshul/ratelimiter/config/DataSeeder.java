package com.anshul.ratelimiter.config;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final PolicyService policyService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Checking and seeding default rate limiting policies...");

        seedPolicy("/api/sql/analyse", "SLIDING_WINDOW", 20, 60);
        seedPolicy("/api/sql/explain", "TOKEN_BUCKET", 5, 60);
        seedPolicy("/api/sandbox/request", "SLIDING_WINDOW", 10, 60);
        seedPolicy("/api/audit/logs", "SLIDING_WINDOW", 30, 60);

        log.info("Rate limiting policies seeding complete.");
    }

    private void seedPolicy(String endpoint, String algorithm, int maxRequests, int windowSeconds) {
        Policy existing = policyService.getPolicy(endpoint);
        if (existing == null) {
            Policy policy = Policy.builder()
                    .endpoint(endpoint)
                    .algorithm(algorithm)
                    .maxRequests(maxRequests)
                    .windowSeconds(windowSeconds)
                    .build();
            policyService.savePolicy(policy);
            log.info("Seeded policy for endpoint: {} [Algorithm: {}, MaxRequests: {}, WindowSeconds: {}]", 
                    endpoint, algorithm, maxRequests, windowSeconds);
        } else {
            log.info("Policy already exists for endpoint: {}, skipping.", endpoint);
        }
    }
}
