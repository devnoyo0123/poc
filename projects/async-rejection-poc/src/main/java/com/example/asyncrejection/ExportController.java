package com.example.asyncrejection;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ExportController {

    private final ExportService exportService;

    ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/api/v1/half-yearly-distributions/excel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> requestExport(@RequestParam(defaultValue = "3000") long sleepMillis) {
        String requestThread = Thread.currentThread().getName();
        UUID jobId = UUID.randomUUID();

        exportService.exportAsync(jobId, sleepMillis);

        return Map.of(
                "jobId", jobId.toString(),
                "requestThread", requestThread,
                "message", "submit succeeded");
    }
}
