package com.example.tomcathikaritimeout;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SlowController {

    private static final Logger log = LoggerFactory.getLogger(SlowController.class);

    private final SlowService slowService;

    SlowController(SlowService slowService) {
        this.slowService = slowService;
    }

    @PostMapping("/api/v1/slow")
    Map<String, Object> slow(@RequestParam(defaultValue = "5") long sleepSeconds) {
        String requestThread = Thread.currentThread().getName();
        log.info("[ENTER] thread={} sleepSeconds={} ts={}", requestThread, sleepSeconds, Instant.now());

        slowService.slowQuery(sleepSeconds);

        return Map.of(
                "status", "ok",
                "workerThread", requestThread,
                "sleptSeconds", sleepSeconds);
    }
}
