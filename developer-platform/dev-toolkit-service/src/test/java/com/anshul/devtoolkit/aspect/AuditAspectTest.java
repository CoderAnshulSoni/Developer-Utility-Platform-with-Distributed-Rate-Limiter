package com.anshul.devtoolkit.aspect;

import com.anshul.devtoolkitservice.DevToolkitApplication;
import com.anshul.devtoolkit.client.RateLimiterClient;
import com.anshul.devtoolkit.client.RateLimiterClient.RateLimitResult;
import com.anshul.devtoolkit.entity.AuditLog;
import com.anshul.devtoolkit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DevToolkitApplication.class)
@AutoConfigureMockMvc
public class AuditAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private RateLimiterClient rateLimiterClient;

    @BeforeEach
    public void setUp() {
        RateLimitResult defaultResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(10)
                .retryAfter(0L)
                .build();
        lenient().when(rateLimiterClient.checkRateLimit(anyString(), anyString())).thenReturn(defaultResult);
        lenient().when(auditLogRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(new java.util.ArrayList<>()));
    }

    @Test
    public void apiCall_ShouldCreateAuditLogEntry() throws Exception {
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk());

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertNotNull(savedLog.getCorrelationId());
    }

    @Test
    public void auditLog_ShouldRecordLatency() throws Exception {
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk());

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertTrue(savedLog.getLatencyMs() >= 0);
    }

    @Test
    public void auditLog_WhenRateLimiterAllows_ShouldLogAllowedStatus() throws Exception {
        RateLimitResult allowedResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(8)
                .retryAfter(0L)
                .build();
        when(rateLimiterClient.checkRateLimit(anyString(), anyString())).thenReturn(allowedResult);

        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk());

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertEquals("ALLOWED", savedLog.getRateLimitStatus());
        assertEquals(8, savedLog.getRateLimitRemaining());
    }

    @Test
    public void auditLog_WhenRateLimiterDown_ShouldLogLimiterDownStatus() throws Exception {
        // When the rate limiter is down/throws an exception, the client returns a fail-open result with remaining = -1
        RateLimitResult downResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(-1)
                .retryAfter(0L)
                .build();
        when(rateLimiterClient.checkRateLimit(anyString(), anyString())).thenReturn(downResult);

        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk());

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertEquals("LIMITER_DOWN", savedLog.getRateLimitStatus());
    }

    @Test
    public void auditLog_ShouldContainEndpointName() throws Exception {
        mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT * FROM users\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertTrue(savedLog.getEndpoint().contains("SqlController"));
    }
}
