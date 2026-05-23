package com.anshul.devtoolkit.aspect;

import com.anshul.devtoolkit.client.RateLimiterClient;
import com.anshul.devtoolkit.client.RateLimiterClient.RateLimitResult;
import com.anshul.devtoolkit.entity.AuditLog;
import com.anshul.devtoolkit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final RateLimiterClient rateLimiterClient;

    @Around("execution(* com.anshul.devtoolkit.controller..*(..))")
    public Object auditCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = UUID.randomUUID().toString();
        String endpointName = joinPoint.getSignature().toString();
        long startTime = System.currentTimeMillis();

        String httpMethod = "UNKNOWN";
        String requestBodyStr = "";
        String rateLimitEndpoint = endpointName;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                httpMethod = request.getMethod();
                rateLimitEndpoint = request.getRequestURI();
                if (attributes.getResponse() != null) {
                    attributes.getResponse().setHeader("X-Correlation-ID", correlationId);
                }
            }
        } catch (Exception ignored) {}

        if ("UNKNOWN".equals(httpMethod)) {
            httpMethod = "AOP";
        }

        // Capture arguments as request body payload
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            requestBodyStr = String.valueOf(args[0]);
        }

        // Call rate limiter
        RateLimitResult rateLimitResult;
        try {
            rateLimitResult = rateLimiterClient.checkRateLimit("internal", rateLimitEndpoint);
        } catch (Exception e) {
            log.error("Rate limiter client failed, failing open", e);
            rateLimitResult = RateLimitResult.builder()
                    .allowed(true)
                    .remainingRequests(-1)
                    .retryAfter(0L)
                    .build();
        }

        String rateLimitStatus;
        int remaining = rateLimitResult.getRemainingRequests();

        if (!rateLimitResult.isAllowed()) {
            rateLimitStatus = "BLOCKED";
            long latency = System.currentTimeMillis() - startTime;

            // Save log for blocked request
            saveAuditLog(correlationId, endpointName, httpMethod, requestBodyStr, 429, latency, remaining, rateLimitStatus);

            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again in " + rateLimitResult.getRetryAfter() + " seconds.");
        }

        // Determine limit status
        if (remaining == -1) {
            rateLimitStatus = "LIMITER_DOWN";
        } else {
            rateLimitStatus = "ALLOWED";
        }

        Object result;
        int responseStatus = 200;
        try {
            result = joinPoint.proceed();

            // Extract HTTP status code from response context
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null && attributes.getResponse() != null) {
                    responseStatus = attributes.getResponse().getStatus();
                }
            } catch (Exception ignored) {}

        } catch (Throwable t) {
            long latency = System.currentTimeMillis() - startTime;
            if (t instanceof ResponseStatusException) {
                responseStatus = ((ResponseStatusException) t).getStatusCode().value();
            } else {
                responseStatus = 500;
            }
            saveAuditLog(correlationId, endpointName, httpMethod, requestBodyStr, responseStatus, latency, remaining, rateLimitStatus);
            throw t;
        }

        long latency = System.currentTimeMillis() - startTime;
        saveAuditLog(correlationId, endpointName, httpMethod, requestBodyStr, responseStatus, latency, remaining, rateLimitStatus);

        return result;
    }

    private void saveAuditLog(String correlationId, String endpoint, String method, String requestBody,
                              int responseStatus, long latencyMs, int rateLimitRemaining, String rateLimitStatus) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .correlationId(correlationId)
                    .endpoint(endpoint)
                    .method(method)
                    .requestBody(requestBody)
                    .responseStatus(responseStatus)
                    .latencyMs(latencyMs)
                    .rateLimitRemaining(rateLimitRemaining)
                    .rateLimitStatus(rateLimitStatus)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save audit log entry to database", e);
        }
    }
}
