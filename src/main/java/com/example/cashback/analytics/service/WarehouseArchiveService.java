package com.example.cashback.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class WarehouseArchiveService {

        private final NamedParameterJdbcTemplate jdbcTemplate;

        @Qualifier("warehouseJdbcTemplate")
        private final NamedParameterJdbcTemplate warehouseJdbcTemplate;

        @Transactional
        public int archiveBefore(LocalDate cutoffDate) {
                ensureArchiveSchema();
                int archived = archiveRewards(cutoffDate);
                archived += archiveTransactions(cutoffDate);
                archived += archiveReferrals(cutoffDate);
                archived += archiveMerchants(cutoffDate);
                return archived;
        }

        private int archiveTransactions(LocalDate cutoffDate) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT id, user_id, merchant_id, amount, currency, status, created_at "
                                                + "FROM transactions WHERE created_at < :cutoffDate",
                                Map.of("cutoffDate", cutoffDate));
                int copied = warehouseJdbcTemplate.batchUpdate(
                                """
                                                MERGE INTO archive_transactions AS target
                                                USING (SELECT CAST(:id AS VARCHAR(36)) AS id, CAST(:userId AS VARCHAR(36)) AS user_id,
                                                              CAST(:merchantId AS VARCHAR(36)) AS merchant_id, :amount AS amount,
                                                              :currency AS currency, :status AS status, :createdAt AS created_at) AS source
                                                ON target.id = source.id
                                                WHEN NOT MATCHED THEN INSERT (id, user_id, merchant_id, amount, currency, status, created_at)
                                                VALUES (source.id, source.user_id, source.merchant_id, source.amount, source.currency,
                                                        source.status, source.created_at)
                                                """,
                                toParameterSources(rows, row -> new MapSqlParameterSource()
                                                .addValue("id", row.get("id"))
                                                .addValue("userId", row.get("user_id"))
                                                .addValue("merchantId", row.get("merchant_id"))
                                                .addValue("amount", row.get("amount"))
                                                .addValue("currency", row.get("currency"))
                                                .addValue("status", row.get("status"))
                                                .addValue("createdAt", row.get("created_at")))).length;
                deleteIfCopied("transactions", rows, copied);
                return rows.size();
        }

        private int archiveRewards(LocalDate cutoffDate) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT id, user_id, transaction_id, reward_points, reward_type, created_at "
                                                + "FROM rewards WHERE created_at < :cutoffDate",
                                Map.of("cutoffDate", cutoffDate));
                int copied = warehouseJdbcTemplate.batchUpdate(
                                """
                                                MERGE INTO archive_rewards AS target
                                                USING (SELECT CAST(:id AS VARCHAR(36)) AS id, CAST(:userId AS VARCHAR(36)) AS user_id,
                                                              CAST(:transactionId AS VARCHAR(36)) AS transaction_id, :rewardPoints AS reward_points,
                                                              :rewardType AS reward_type, :createdAt AS created_at) AS source
                                                ON target.id = source.id
                                                WHEN NOT MATCHED THEN INSERT (id, user_id, transaction_id, reward_points, reward_type, created_at)
                                                VALUES (source.id, source.user_id, source.transaction_id, source.reward_points,
                                                        source.reward_type, source.created_at)
                                                """,
                                toParameterSources(rows, row -> new MapSqlParameterSource()
                                                .addValue("id", row.get("id")).addValue("userId", row.get("user_id"))
                                                .addValue("transactionId", row.get("transaction_id"))
                                                .addValue("rewardPoints", row.get("reward_points"))
                                                .addValue("rewardType", row.get("reward_type"))
                                                .addValue("createdAt", row.get("created_at")))).length;
                deleteIfCopied("rewards", rows, copied);
                return rows.size();
        }

        private int archiveReferrals(LocalDate cutoffDate) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT id, referrer_id, referee_id, reward_points, status, created_at "
                                                + "FROM referral WHERE created_at < :cutoffDate",
                                Map.of("cutoffDate", cutoffDate));
                int copied = warehouseJdbcTemplate.batchUpdate(
                                """
                                                MERGE INTO archive_referrals AS target
                                                USING (SELECT CAST(:id AS VARCHAR(36)) AS id, CAST(:referrerId AS VARCHAR(36)) AS referrer_id,
                                                              CAST(:refereeId AS VARCHAR(36)) AS referee_id, :rewardPoints AS reward_points,
                                                              :status AS status, :createdAt AS created_at) AS source
                                                ON target.id = source.id
                                                WHEN NOT MATCHED THEN INSERT (id, referrer_id, referee_id, reward_points, status, created_at)
                                                VALUES (source.id, source.referrer_id, source.referee_id, source.reward_points,
                                                        source.status, source.created_at)
                                                """,
                                toParameterSources(rows, row -> new MapSqlParameterSource()
                                                .addValue("id", row.get("id"))
                                                .addValue("referrerId", row.get("referrer_id"))
                                                .addValue("refereeId", row.get("referee_id"))
                                                .addValue("rewardPoints", row.get("reward_points"))
                                                .addValue("status", row.get("status"))
                                                .addValue("createdAt", row.get("created_at")))).length;
                deleteIfCopied("referral", rows, copied);
                return rows.size();
        }

        private int archiveMerchants(LocalDate cutoffDate) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT id, name, category, country, created_at FROM merchant "
                                                + "WHERE created_at < :cutoffDate AND NOT EXISTS "
                                                + "(SELECT 1 FROM transactions t WHERE t.merchant_id = merchant.id)",
                                Map.of("cutoffDate", cutoffDate));
                int copied = warehouseJdbcTemplate.batchUpdate("""
                                MERGE INTO archive_merchants AS target
                                USING (SELECT CAST(:id AS VARCHAR(36)) AS id, :name AS name, :category AS category,
                                              :country AS country, :createdAt AS created_at) AS source
                                ON target.id = source.id
                                WHEN NOT MATCHED THEN INSERT (id, name, category, country, created_at)
                                VALUES (source.id, source.name, source.category, source.country, source.created_at)
                                """, toParameterSources(rows, row -> new MapSqlParameterSource()
                                .addValue("id", row.get("id")).addValue("name", row.get("name"))
                                .addValue("category", row.get("category")).addValue("country", row.get("country"))
                                .addValue("createdAt", row.get("created_at")))).length;
                if (rows.size() != copied) {
                        throw new IllegalStateException("Archive verification failed for merchant");
                }
                if (!rows.isEmpty()) {
                        jdbcTemplate.update("DELETE FROM merchant WHERE id IN (:ids) AND NOT EXISTS "
                                        + "(SELECT 1 FROM transactions t WHERE t.merchant_id = merchant.id)",
                                        Map.of("ids", rows.stream().map(row -> row.get("id")).toList()));
                }
                return rows.size();
        }

        private void deleteIfCopied(String table, List<Map<String, Object>> rows, int copiedRows) {
                if (rows.size() != copiedRows) {
                        throw new IllegalStateException("Archive verification failed for " + table);
                }
                if (!rows.isEmpty()) {
                        jdbcTemplate.update("DELETE FROM " + table + " WHERE id IN (:ids)",
                                        Map.of("ids", rows.stream().map(row -> row.get("id")).toList()));
                }
        }

        private @NonNull SqlParameterSource[] toParameterSources(List<Map<String, Object>> rows,
                        Function<Map<String, Object>, MapSqlParameterSource> mapper) {
                SqlParameterSource[] sources = new SqlParameterSource[rows.size()];
                for (int index = 0; index < rows.size(); index++) {
                        sources[index] = mapper.apply(rows.get(index));
                }
                return sources;
        }

        private void ensureArchiveSchema() {
                warehouseJdbcTemplate.getJdbcTemplate()
                                .execute("""
                                                CREATE TABLE IF NOT EXISTS archive_transactions (
                                                    id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
                                                    merchant_id VARCHAR(36) NOT NULL, amount NUMERIC(18,2) NOT NULL,
                                                    currency VARCHAR(10) NOT NULL, status VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL)
                                                """);
                warehouseJdbcTemplate.getJdbcTemplate()
                                .execute("""
                                                CREATE TABLE IF NOT EXISTS archive_rewards (
                                                    id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, transaction_id VARCHAR(36) NOT NULL,
                                                    reward_points INTEGER NOT NULL, reward_type VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL)
                                                """);
                warehouseJdbcTemplate.getJdbcTemplate()
                                .execute("""
                                                CREATE TABLE IF NOT EXISTS archive_referrals (
                                                    id VARCHAR(36) PRIMARY KEY, referrer_id VARCHAR(36) NOT NULL, referee_id VARCHAR(36) NOT NULL,
                                                    reward_points INTEGER NOT NULL, status VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL)
                                                """);
                warehouseJdbcTemplate.getJdbcTemplate().execute("""
                                CREATE TABLE IF NOT EXISTS archive_merchants (
                                    id VARCHAR(36) PRIMARY KEY, name VARCHAR(255) NOT NULL, category VARCHAR(100),
                                    country VARCHAR(100), created_at TIMESTAMP NOT NULL)
                                """);
        }
}