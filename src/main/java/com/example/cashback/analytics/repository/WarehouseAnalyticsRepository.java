package com.example.cashback.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WarehouseAnalyticsRepository {

    @Qualifier("warehouseJdbcTemplate")
    private final NamedParameterJdbcTemplate warehouseJdbcTemplate;

    public List<Object[]> totalAmountsPerUser() {
        return warehouseJdbcTemplate.query("""
                SELECT user_id, COUNT(*), SUM(amount), SUM(cashback_amount)
                FROM fact_transactions GROUP BY user_id
                """, Map.of(), (rs, rowNum) -> new Object[] {
                UUID.fromString(rs.getString(1)), rs.getLong(2), rs.getBigDecimal(3), rs.getBigDecimal(4)
        });
    }

    public List<Object[]> totalRewardsPerUser() {
        return warehouseJdbcTemplate.query("""
                SELECT user_id, COUNT(*), SUM(amount)
                FROM fact_rewards GROUP BY user_id
                """, Map.of(), (rs, rowNum) -> new Object[] {
                UUID.fromString(rs.getString(1)), rs.getLong(2), rs.getBigDecimal(3)
        });
    }

    public List<Object[]> offerAnalytics() {
        return warehouseJdbcTemplate.query("""
                SELECT o.offer_id, COUNT(t.transaction_id),
                       COALESCE(SUM(t.cashback_amount), 0), COALESCE(AVG(t.amount), 0)
                FROM dim_merchant_offers o
                LEFT JOIN fact_transactions t ON t.offer_id = o.offer_id
                GROUP BY o.offer_id
                """, Map.of(), (rs, rowNum) -> new Object[] {
                UUID.fromString(rs.getString(1)), rs.getLong(2), rs.getBigDecimal(3), rs.getBigDecimal(4)
        });
    }
}