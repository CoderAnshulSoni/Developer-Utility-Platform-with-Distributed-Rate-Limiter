package com.anshul.devtoolkit.service;

import com.anshul.devtoolkit.entity.RequestHistory;
import com.anshul.devtoolkit.repository.RequestHistoryRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxService {

    private final RestTemplate restTemplate;
    private final RequestHistoryRepository requestHistoryRepository;

    public SandboxResponse executeRequest(String clientId, String method, String url, Map<String, String> headers, String body) {
        long startTime = System.currentTimeMillis();

        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
        HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);

        int statusCode = 500;
        String responseBody = "";
        Map<String, String> responseHeaders = new HashMap<>();
        long latencyMs = 0;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method.toUpperCase()),
                    entity,
                    String.class
            );

            latencyMs = System.currentTimeMillis() - startTime;
            statusCode = response.getStatusCode().value();
            responseBody = response.getBody();

            response.getHeaders().forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    responseHeaders.put(k, v.get(0));
                }
            });

        } catch (HttpStatusCodeException e) {
            latencyMs = System.currentTimeMillis() - startTime;
            statusCode = e.getStatusCode().value();
            responseBody = e.getResponseBodyAsString();

            if (e.getResponseHeaders() != null) {
                e.getResponseHeaders().forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        responseHeaders.put(k, v.get(0));
                    }
                });
            }
        } catch (Exception e) {
            latencyMs = System.currentTimeMillis() - startTime;
            statusCode = 500;
            responseBody = "Error executing request: " + e.getMessage();
            log.error("Request execution failed", e);
        }

        // Save execution audit log in request_history table
        try {
            RequestHistory history = RequestHistory.builder()
                    .clientId(clientId)
                    .method(method)
                    .url(url)
                    .requestBody(body)
                    .responseStatus(statusCode)
                    .responseBody(responseBody)
                    .latencyMs(latencyMs)
                    .createdAt(LocalDateTime.now())
                    .build();
            requestHistoryRepository.save(history);
        } catch (Exception dbEx) {
            log.error("Failed to save request history to database", dbEx);
        }

        return SandboxResponse.builder()
                .statusCode(statusCode)
                .responseBody(responseBody)
                .responseHeaders(responseHeaders)
                .latencyMs(latencyMs)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SandboxResponse {
        private int statusCode;
        private String responseBody;
        private Map<String, String> responseHeaders;
        private long latencyMs;
    }
}
