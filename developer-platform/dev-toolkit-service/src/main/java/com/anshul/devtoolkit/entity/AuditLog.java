package com.anshul.devtoolkit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "endpoint", length = 200)
    private String endpoint;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "rate_limit_remaining")
    private Integer rateLimitRemaining;

    @Column(name = "rate_limit_status", length = 20)
    private String rateLimitStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
