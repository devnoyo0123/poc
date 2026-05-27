package com.example.jobrunr.jobs;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SampleJob {

    @Job(name = "Sample Job")
    public void execute(String message) {
        log.info("Executing job with message: {}", message);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Job completed successfully!");
    }
}
