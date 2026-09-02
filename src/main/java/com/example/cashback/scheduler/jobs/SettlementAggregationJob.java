package com.example.cashback.scheduler.jobs;

import com.example.cashback.scheduler.metrics.JobMetrics;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class SettlementAggregationJob implements Job {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);

        String sql = """
                INSERT INTO financial_book (week_start, week_end, merchant_id, total_settlement)
                SELECT :weekStart, :weekEnd, source AS merchant_id, SUM(amount) AS total_settlement
                FROM audit_events
                WHERE timestamp >= :weekStart AND timestamp < :weekEnd
                GROUP BY source
                ON CONFLICT (week_start, week_end, merchant_id)
                DO UPDATE SET total_settlement = EXCLUDED.total_settlement
                """;

        Map<String, Object> params = Map.of(
                "weekStart", weekStart,
                "weekEnd", weekEnd);

        jdbcTemplate.update(sql, params);

        System.out.println("SettlementAggregationJob: Aggregated settlements for week " + weekStart);

        // Record Prometheus metric
        jobMetrics.recordJobExecution("SettlementAggregationJob");
    }
}
