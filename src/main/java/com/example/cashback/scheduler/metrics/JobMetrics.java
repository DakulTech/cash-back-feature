package com.example.cashback.scheduler.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class JobMetrics {

    private final MeterRegistry registry;

    public JobMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordJobExecution(String jobName) {
        registry.counter("jobs.executed", "job", jobName).increment();
    }
}
