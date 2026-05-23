package com.anshul.devtoolkit.service;

import com.anshul.devtoolkit.entity.RequestHistory;
import com.anshul.devtoolkit.repository.RequestHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SandboxServiceTest {

    @InjectMocks
    private SandboxService sandboxService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RequestHistoryRepository requestHistoryRepository;

    @Test
    public void executeRequest_SuccessfulGet_ShouldReturnResponse() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        SandboxService.SandboxResponse response = sandboxService.executeRequest(
                "client1", "GET", "http://example.com", Collections.emptyMap(), null
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getResponseBody());
        assertTrue(response.getLatencyMs() >= 0);
    }

    @Test
    public void executeRequest_ShouldSaveToRequestHistory() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("OK");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        sandboxService.executeRequest(
                "client1", "GET", "http://example.com", Collections.emptyMap(), "some-body"
        );

        ArgumentCaptor<RequestHistory> historyCaptor = ArgumentCaptor.forClass(RequestHistory.class);
        verify(requestHistoryRepository, times(1)).save(historyCaptor.capture());

        RequestHistory savedHistory = historyCaptor.getValue();
        assertNotNull(savedHistory);
        assertEquals("client1", savedHistory.getClientId());
        assertEquals("GET", savedHistory.getMethod());
        assertEquals("http://example.com", savedHistory.getUrl());
        assertEquals("some-body", savedHistory.getRequestBody());
        assertEquals(200, savedHistory.getResponseStatus());
        assertEquals("OK", savedHistory.getResponseBody());
    }

    @Test
    public void executeRequest_ShouldMeasureLatency() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(5);
                    return ResponseEntity.ok("OK");
                });

        SandboxService.SandboxResponse response = sandboxService.executeRequest(
                "client1", "GET", "http://example.com", Collections.emptyMap(), null
        );

        assertNotNull(response);
        assertTrue(response.getLatencyMs() > 0, "Expected positive latency: " + response.getLatencyMs());
    }

    @Test
    public void executeRequest_OnRestTemplateError_ShouldReturn500() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        SandboxService.SandboxResponse response = sandboxService.executeRequest(
                "client1", "GET", "http://example.com", Collections.emptyMap(), null
        );

        assertNotNull(response);
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getResponseBody().contains("Connection refused"));
    }

    @Test
    public void executeRequest_WithPostMethod_ShouldSendBody() {
        ResponseEntity<String> responseEntity = ResponseEntity.ok("POST_OK");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        SandboxService.SandboxResponse response = sandboxService.executeRequest(
                "client1", "POST", "http://example.com", Collections.emptyMap(), "{\"key\":\"value\"}"
        );

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("POST_OK", response.getResponseBody());

        verify(restTemplate, times(1)).exchange(
                eq("http://example.com"),
                eq(HttpMethod.POST),
                argThat(entity -> entity != null && "{\"key\":\"value\"}".equals(entity.getBody())),
                eq(String.class)
        );
    }
}
