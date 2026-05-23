package com.anshul.devtoolkit.controller;

import com.anshul.devtoolkit.entity.RequestHistory;
import com.anshul.devtoolkit.repository.RequestHistoryRepository;
import com.anshul.devtoolkit.service.SandboxService;
import com.anshul.devtoolkit.service.SandboxService.SandboxResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxService sandboxService;
    private final RequestHistoryRepository requestHistoryRepository;

    @PostMapping("/request")
    public SandboxResponse executeRequest(@RequestBody SandboxRequest request) {
        return sandboxService.executeRequest(
                request.getClientId(),
                request.getMethod(),
                request.getUrl(),
                request.getHeaders(),
                request.getBody()
        );
    }

    @GetMapping("/history")
    public List<RequestHistory> getHistory() {
        return requestHistoryRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SandboxRequest {
        private String clientId;
        private String method;
        private String url;
        private Map<String, String> headers;
        private String body;
    }
}
