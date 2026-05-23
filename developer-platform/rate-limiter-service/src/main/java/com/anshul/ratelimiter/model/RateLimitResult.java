package com.anshul.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RateLimitResult {
    private boolean allowed;
    private int remainingRequests;
    private long resetInSeconds;
    private String message;
}
