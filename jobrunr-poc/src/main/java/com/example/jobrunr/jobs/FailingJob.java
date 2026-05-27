package com.example.jobrunr.jobs;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FailingJob {

    @Job(name = "Permanently Failing Job", retries = 0)
    public void executePermanent(String message) {
        log.info("Permanently Failing Job with message: {}", message);
        throw new RuntimeException("This job always fails! Retry from Dashboard.");
    }
}
