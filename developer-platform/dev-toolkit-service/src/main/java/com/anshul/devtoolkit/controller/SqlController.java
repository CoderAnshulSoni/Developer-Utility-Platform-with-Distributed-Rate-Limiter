package com.anshul.devtoolkit.controller;

import com.anshul.devtoolkit.service.ExplainService;
import com.anshul.devtoolkit.service.ExplainService.ExplainResult;
import com.anshul.devtoolkit.service.SqlAnalyserService;
import com.anshul.devtoolkit.service.SqlAnalyserService.AnalysisResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/sql")
@RequiredArgsConstructor
public class SqlController {

    private final SqlAnalyserService sqlAnalyserService;
    private final ExplainService explainService;

    @PostMapping("/analyse")
    public AnalysisResult analyseQuery(@RequestBody SqlRequest request) {
        if (request == null || request.getSql() == null || request.getSql().trim().isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "SQL query cannot be empty"
            );
        }
        return sqlAnalyserService.analyse(request.getSql());
    }

    @PostMapping("/explain")
    public ExplainResult explainQuery(@RequestBody SqlRequest request) {
        return explainService.explainQuery(request.getSql());
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Void> handleResponseStatusException(org.springframework.web.server.ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode()).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleException() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlRequest {
        private String sql;
    }
}
