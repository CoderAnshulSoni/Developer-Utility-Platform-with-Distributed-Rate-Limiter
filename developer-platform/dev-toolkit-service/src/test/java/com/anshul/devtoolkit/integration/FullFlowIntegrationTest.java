package com.anshul.devtoolkit.integration;

import com.anshul.devtoolkit.client.RateLimiterClient;
import com.anshul.devtoolkit.client.RateLimiterClient.RateLimitResult;
import com.anshul.devtoolkit.entity.AuditLog;
import com.anshul.devtoolkit.repository.AuditLogRepository;
import com.anshul.devtoolkit.repository.RequestHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = com.anshul.devtoolkitservice.DevToolkitApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.sql.init.mode=never",
        "rate-limiter.enabled=true"
    }
)
@AutoConfigureMockMvc
public class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RequestHistoryRepository requestHistoryRepository;

    @MockBean
    private RateLimiterClient rateLimiterClient;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        auditLogRepository.deleteAll();
        requestHistoryRepository.deleteAll();

        RateLimitResult defaultResult = RateLimitResult.builder()
                .allowed(true)
                .remainingRequests(10)
                .retryAfter(0L)
                .build();
        lenient().when(rateLimiterClient.checkRateLimit(anyString(), anyString())).thenReturn(defaultResult);

        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);
        lenient().when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(responseEntity);
    }

    @Test
    public void sqlAnalyse_FullFlow_ShouldReturnAnalysisWithScore() throws Exception {
        mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT * FROM orders\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(lessThan(100)))
                .andExpect(jsonPath("$.warnings", not(empty())));
    }

    @Test
    public void sandboxRequest_FullFlow_ShouldSaveHistory() throws Exception {
        mockMvc.perform(post("/api/sandbox/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\":\"client1\",\"method\":\"GET\",\"url\":\"http://example.com\",\"headers\":{},\"body\":null}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sandbox/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].clientId").value("client1"));
    }

    @Test
    public void auditLog_FullFlow_ShouldBeSearchableByCorrelationId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT id FROM orders\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse().getHeader("X-Correlation-ID");
        assertNotNull(correlationId, "Correlation ID header should not be null");

        mockMvc.perform(get("/api/audit/logs").param("correlationId", correlationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].correlationId").value(correlationId))
                .andExpect(jsonPath("$[0].requestBody", containsString("SELECT id FROM orders")));
    }

    @Test
    public void rateLimitFail_Open_WhenLimiterDown() throws Exception {
        when(rateLimiterClient.checkRateLimit(anyString(), anyString()))
                .thenThrow(new RuntimeException("Rate Limiter is down"));

        mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT id FROM orders\"}"))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        assertFalse(logs.isEmpty(), "Audit log should have been saved");
        AuditLog savedLog = logs.get(logs.size() - 1);
        assertEquals("LIMITER_DOWN", savedLog.getRateLimitStatus());
    }

    @Test
    public void multipleRequests_ShouldAllBeAudited() throws Exception {
        mockMvc.perform(post("/api/sql/analyse").contentType(MediaType.APPLICATION_JSON).content("{\"sql\":\"SELECT id FROM orders\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/sql/analyse").contentType(MediaType.APPLICATION_JSON).content("{\"sql\":\"SELECT name FROM users\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/sandbox/history")).andExpect(status().isOk());
        mockMvc.perform(get("/api/audit/logs")).andExpect(status().isOk());
        mockMvc.perform(post("/api/sql/analyse").contentType(MediaType.APPLICATION_JSON).content("{\"sql\":\"SELECT * FROM products\"}")).andExpect(status().isOk());

        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));
    }
}
