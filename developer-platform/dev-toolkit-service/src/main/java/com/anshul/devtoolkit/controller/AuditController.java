package com.anshul.devtoolkit.controller;

import com.anshul.devtoolkit.entity.AuditLog;
import com.anshul.devtoolkit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    public List<AuditLog> getLogs(@RequestParam(required = false) String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            return auditLogRepository.findByCorrelationId(correlationId);
        } else {
            return auditLogRepository.findAll(
                    PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        }
    }

    @DeleteMapping("/logs")
    public void deleteLogs() {
        auditLogRepository.deleteAll();
    }
}
