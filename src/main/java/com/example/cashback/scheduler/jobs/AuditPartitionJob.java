package com.example.cashback.scheduler.jobs;

import com.example.cashback.scheduler.metrics.JobMetrics;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditPartitionJob implements Job {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        LocalDate now = LocalDate.now();
        LocalDate nextMonth = now.plusMonths(1);

        String partitionName = String.format("audit_events_%s_%02d",
                nextMonth.getYear(),
                nextMonth.getMonthValue());

        String startDate = nextMonth.withDayOfMonth(1).toString();
        String endDate = nextMonth.plusMonths(1).withDayOfMonth(1).toString();

        // Use Map<String,Object> to avoid type safety warning
        Map<String, Object> params = Map.of(
                "startDate", startDate,
                "endDate", endDate);

        // Create partition
        String createPartitionSql = "CREATE TABLE IF NOT EXISTS " + partitionName +
                " PARTITION OF audit_events " +
                "FOR VALUES FROM (:startDate) TO (:endDate);";

        jdbcTemplate.update(createPartitionSql, params);

        // Indexes for compliance queries
        jdbcTemplate.update(
                "CREATE INDEX IF NOT EXISTS idx_" + partitionName + "_event_type " +
                        "ON " + partitionName + "(event_type);",
                Map.of());

        jdbcTemplate.update(
                "CREATE INDEX IF NOT EXISTS idx_" + partitionName + "_source " +
                        "ON " + partitionName + "(source);",
                Map.of());

        jdbcTemplate.update(
                "CREATE INDEX IF NOT EXISTS idx_" + partitionName + "_type_timestamp " +
                        "ON " + partitionName + "(event_type, timestamp);",
                Map.of());

        System.out.println("AuditPartitionJob: Created partition " + partitionName);

        // Record Prometheus metric
        jobMetrics.recordJobExecution("AuditPartitionJob");
    }
}
