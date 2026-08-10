package com.example.tomcathikaritimeout;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SlowService {

    private static final Logger log = LoggerFactory.getLogger(SlowService.class);

    private final JdbcTemplate jdbcTemplate;

    public SlowService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void slowQuery(long sleepSeconds) {
        String workerThread = Thread.currentThread().getName();
        log.info("[Hikari-acquire-start] thread={} ts={}", workerThread, Instant.now());

        // pg_sleep(double precision) returns void; use ResultSetExtractor returning null so no type-binding is required.
        jdbcTemplate.query("SELECT pg_sleep(?)", rs -> null, sleepSeconds);

        log.info("[Hikari-acquire-done] thread={} ts={}", workerThread, Instant.now());
    }
}
