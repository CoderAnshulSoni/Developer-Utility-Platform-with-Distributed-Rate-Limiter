package com.anshul.ratelimiter.controller;

import com.anshul.ratelimiter.model.Policy;
import com.anshul.ratelimiter.service.PolicyService;
import com.anshul.ratelimiter.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyController.class)
@ContextConfiguration(classes = PolicyControllerTest.TestConfig.class)
public class PolicyControllerTest {

    @SpringBootConfiguration
    @ComponentScan(basePackages = "com.anshul.ratelimiter.controller")
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    public void createPolicy_ShouldReturn201() throws Exception {
        Policy policy = Policy.builder()
                .endpoint("/api/sql/analyse")
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("TOKEN_BUCKET")
                .build();

        mockMvc.perform(post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(policy)))
                .andExpect(status().isCreated());

        verify(policyService, times(1)).savePolicy(any(Policy.class));
    }

    @Test
    public void getPolicy_WhenExists_ShouldReturn200WithPolicy() throws Exception {
        Policy policy = Policy.builder()
                .endpoint("/test-endpoint")
                .maxRequests(10)
                .windowSeconds(60)
                .algorithm("TOKEN_BUCKET")
                .build();

        when(policyService.getPolicy(eq("/test-endpoint"))).thenReturn(policy);

        mockMvc.perform(get("/api/policies/test-endpoint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("/test-endpoint"))
                .andExpect(jsonPath("$.maxRequests").value(10))
                .andExpect(jsonPath("$.windowSeconds").value(60))
                .andExpect(jsonPath("$.algorithm").value("TOKEN_BUCKET"));
    }

    @Test
    public void getPolicy_WhenNotExists_ShouldReturn404() throws Exception {
        when(policyService.getPolicy(eq("/unknown"))).thenReturn(null);

        mockMvc.perform(get("/api/policies/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getAllPolicies_ShouldReturnList() throws Exception {
        Policy p1 = Policy.builder().endpoint("/api/1").maxRequests(5).build();
        Policy p2 = Policy.builder().endpoint("/api/2").maxRequests(10).build();

        when(policyService.getAllPolicies()).thenReturn(Arrays.asList(p1, p2));

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].endpoint").value("/api/1"))
                .andExpect(jsonPath("$[1].endpoint").value("/api/2"));
    }

    @Test
    public void deletePolicy_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/policies/test-endpoint"))
                .andExpect(status().isNoContent());

        verify(policyService, times(1)).deletePolicy(eq("/test-endpoint"));
    }
}
