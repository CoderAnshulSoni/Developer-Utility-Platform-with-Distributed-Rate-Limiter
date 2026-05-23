package com.anshul.devtoolkit.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class RateLimiterClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean enabled;

    public RateLimiterClient(
            RestTemplate restTemplate,
            @Value("${rate-limiter.base-url}") String baseUrl,
            @Value("${rate-limiter.enabled}") boolean enabled) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.enabled = enabled;
    }

    public RateLimitResult checkRateLimit(String clientId, String endpoint) {
        if (!enabled) {
            return RateLimitResult.builder()
                    .allowed(true)
                    .remainingRequests(-1)
                    .retryAfter(0L)
                    .build();
        }

        String url = baseUrl + endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Client-Id", clientId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String remainingStr = response.getHeaders().getFirst("X-RateLimit-Remaining");
            String resetStr = response.getHeaders().getFirst("X-RateLimit-Reset");

            int remaining = remainingStr != null ? Integer.parseInt(remainingStr) : -1;
            long reset = resetStr != null ? Long.parseLong(resetStr) : 0L;

            return RateLimitResult.builder()
                    .allowed(true)
                    .remainingRequests(remaining)
                    .retryAfter(reset)
                    .build();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 429) {
                String resetStr = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("X-RateLimit-Reset") : null;
                long reset = 0;
                if (resetStr != null) {
                    try {
                        reset = Long.parseLong(resetStr);
                    } catch (NumberFormatException ignored) {}
                }

                if (reset <= 0) {
                    String body = e.getResponseBodyAsString();
                    if (body.contains("\"retryAfter\":")) {
                        int index = body.indexOf("\"retryAfter\":");
                        String sub = body.substring(index + 13).trim();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < sub.length(); i++) {
                            char c = sub.charAt(i);
                            if (Character.isDigit(c)) {
                                sb.append(c);
                            } else {
                                break;
                            }
                        }
                        if (sb.length() > 0) {
                            reset = Long.parseLong(sb.toString());
                        }
                    }
                }

                return RateLimitResult.builder()
                        .allowed(false)
                        .remainingRequests(0)
                        .retryAfter(reset)
                        .build();
            }

            // If it is another HTTP code (like 404), but contains the rate-limit headers:
            String remainingStr = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("X-RateLimit-Remaining") : null;
            String resetStr = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("X-RateLimit-Reset") : null;
            if (remainingStr != null) {
                return RateLimitResult.builder()
                        .allowed(true)
                        .remainingRequests(Integer.parseInt(remainingStr))
                        .retryAfter(resetStr != null ? Long.parseLong(resetStr) : 0L)
                        .build();
            }

            throw e;
        } catch (Exception e) {
            log.warn("Rate limiter unavailable - failing open: {}", e.getMessage());
            return RateLimitResult.builder()
                    .allowed(true)
                    .remainingRequests(-1)
                    .retryAfter(0L)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RateLimitResult {
        private boolean allowed;
        private int remainingRequests;
        private long retryAfter;
    }
}
