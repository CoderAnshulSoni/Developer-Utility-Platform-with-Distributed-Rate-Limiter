package com.anshul.ratelimiter.interceptor;

import com.anshul.ratelimiter.model.RateLimitResult;
import com.anshul.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = "anonymous";
        }

        String endpoint = request.getRequestURI();

        RateLimitResult result = rateLimiterService.evaluate(clientId, endpoint);

        if (result.isAllowed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingRequests()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetInSeconds()));
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = String.format("{\"error\":\"Rate limit exceeded\",\"retryAfter\":%d}",
                    result.getResetInSeconds());

            try (PrintWriter writer = response.getWriter()) {
                writer.print(jsonResponse);
                writer.flush();
            }
            return false;
        }
    }
}
