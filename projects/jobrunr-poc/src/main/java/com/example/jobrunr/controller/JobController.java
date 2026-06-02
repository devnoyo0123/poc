package com.example.jobrunr.controller;

import com.example.jobrunr.jobs.FailingJob;
import com.example.jobrunr.jobs.SampleJob;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobScheduler jobScheduler;
    private final SampleJob sampleJob;
    private final FailingJob failingJob;

    @PostMapping("/enqueue")
    public String enqueueJob(@RequestParam String message) {
        jobScheduler.enqueue(() -> sampleJob.execute(message));
        return "Job enqueued successfully!";
    }

    @PostMapping("/schedule")
    public String scheduleJob(@RequestParam String message) {
        jobScheduler.schedule(java.time.Instant.now().plusSeconds(60), () -> sampleJob.execute(message));
        return "Job scheduled successfully!";
    }

    @PostMapping("/failing")
    public String enqueueFailingJob(@RequestParam String message) {
        jobScheduler.enqueue(() -> failingJob.executePermanent(message));
        return "Permanently failing job enqueued! Retry from Dashboard.";
    }
}
