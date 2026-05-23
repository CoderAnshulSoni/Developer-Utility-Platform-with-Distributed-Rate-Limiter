package com.anshul.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    private String endpoint;
    private String clientId;
    private int maxRequests;
    private int windowSeconds;
    private String algorithm; // TOKEN_BUCKET or SLIDING_WINDOW
}
