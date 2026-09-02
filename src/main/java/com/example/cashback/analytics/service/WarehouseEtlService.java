package com.example.cashback.analytics.service;

import com.example.cashback.transaction.model.Transaction;
import com.example.cashback.transaction.repository.TransactionRepository;
import com.example.cashback.rewards.model.Reward;
import com.example.cashback.rewards.repository.RewardRepository;
import com.example.cashback.merchant.model.MerchantOffer;
import com.example.cashback.merchant.repository.MerchantOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.ResultSetExtractor;

@Service
@RequiredArgsConstructor
public class WarehouseEtlService {

    private static final String WATERMARK_KEY = "transactions";
    private static final int PAGE_SIZE = 500;

    private final TransactionRepository transactionRepository;
    private final RewardRepository rewardRepository;
    private final MerchantOfferRepository merchantOfferRepository;

    @Qualifier("warehouseJdbcTemplate")
    private final NamedParameterJdbcTemplate warehouseJdbcTemplate;

    @Transactional(readOnly = true)
    public int loadTransactions() {
        ensureWarehouseSchema();
        loadRewards();
        loadOffers();
        Watermark watermark = readWatermark();
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        Page<Transaction> transactionPage;
        int loadedTransactions = 0;
        Watermark newWatermark = watermark;

        do {
            transactionPage = transactionRepository.findAfterWatermark(
                    watermark.createdAt(), watermark.id(), pageRequest);
            for (Transaction transaction : transactionPage) {
                warehouseJdbcTemplate.update(
                        """
                                MERGE INTO fact_transactions AS target
                                USING (SELECT CAST(:transactionId AS VARCHAR(36)) AS transaction_id,
                                              CAST(:userId AS VARCHAR(36)) AS user_id,
                                              CAST(:merchantId AS VARCHAR(36)) AS merchant_id,
                                              CAST(:offerId AS VARCHAR(36)) AS offer_id,
                                              :amount AS amount,
                                              :transactionType AS transaction_type,
                                              :cashbackAmount AS cashback_amount,
                                              :merchantPayout AS merchant_payout,
                                              :createdAt AS created_at) AS source
                                ON target.transaction_id = source.transaction_id
                                WHEN MATCHED THEN UPDATE SET
                                    user_id = source.user_id,
                                    merchant_id = source.merchant_id,
                                    offer_id = source.offer_id,
                                    amount = source.amount,
                                    transaction_type = source.transaction_type,
                                    cashback_amount = source.cashback_amount,
                                    merchant_payout = source.merchant_payout,
                                    created_at = source.created_at
                                WHEN NOT MATCHED THEN INSERT
                                    (transaction_id, user_id, merchant_id, offer_id, amount, transaction_type,
                                     cashback_amount, merchant_payout, created_at)
                                VALUES (source.transaction_id, source.user_id, source.merchant_id, source.offer_id, source.amount,
                                        source.transaction_type, source.cashback_amount, source.merchant_payout,
                                        source.created_at)
                                """,
                        new MapSqlParameterSource()
                                .addValue("transactionId", transaction.getId())
                                .addValue("userId", transaction.getUser().getId())
                                .addValue("merchantId", transaction.getMerchant().getId())
                                .addValue("offerId",
                                        transaction.getOffer() == null ? null : transaction.getOffer().getId())
                                .addValue("amount", transaction.getAmount())
                                .addValue("transactionType", transaction.getType().name())
                                .addValue("cashbackAmount", transaction.getCashbackAmount())
                                .addValue("merchantPayout",
                                        transaction.getAmount().subtract(transaction.getCashbackAmount()))
                                .addValue("createdAt", transaction.getCreatedAt()));
                loadedTransactions++;
                if (transaction.getCreatedAt().isAfter(newWatermark.createdAt())
                        || (transaction.getCreatedAt().equals(newWatermark.createdAt())
                                && transaction.getId().compareTo(newWatermark.id()) > 0)) {
                    newWatermark = new Watermark(transaction.getCreatedAt(), transaction.getId());
                }
            }
            pageRequest = pageRequest.next();
        } while (transactionPage.hasNext());

        if (loadedTransactions > 0) {
            warehouseJdbcTemplate.update("""
                    MERGE INTO etl_watermarks AS target
                    USING (SELECT :pipelineName AS pipeline_name,
                                  :lastCreatedAt AS last_created_at) AS source
                    ON target.pipeline_name = source.pipeline_name
                    WHEN MATCHED THEN UPDATE SET last_created_at = source.last_created_at
                    WHEN NOT MATCHED THEN INSERT (pipeline_name, last_created_at)
                    VALUES (source.pipeline_name, source.last_created_at)
                    """, new MapSqlParameterSource()
                    .addValue("pipelineName", WATERMARK_KEY)
                    .addValue("lastCreatedAt", newWatermark.createdAt())
                    .addValue("lastTransactionId", newWatermark.id().toString()));
        }

        return loadedTransactions;
    }

    private void loadRewards() {
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<Reward> rewardPage;
        do {
            rewardPage = rewardRepository.findAll(pageRequest);
            for (Reward reward : rewardPage) {
                warehouseJdbcTemplate.update("""
                        MERGE INTO fact_rewards AS target
                        USING (SELECT CAST(:rewardId AS VARCHAR(36)) AS reward_id,
                                      CAST(:userId AS VARCHAR(36)) AS user_id,
                                      :amount AS amount, :description AS description,
                                      :createdAt AS created_at) AS source
                        ON target.reward_id = source.reward_id
                        WHEN MATCHED THEN UPDATE SET user_id = source.user_id, amount = source.amount,
                            description = source.description, created_at = source.created_at
                        WHEN NOT MATCHED THEN INSERT (reward_id, user_id, amount, description, created_at)
                        VALUES (source.reward_id, source.user_id, source.amount, source.description, source.created_at)
                        """, new MapSqlParameterSource()
                        .addValue("rewardId", reward.getId())
                        .addValue("userId", reward.getUserId())
                        .addValue("amount", reward.getAmount())
                        .addValue("description", reward.getDescription())
                        .addValue("createdAt", reward.getCreatedAt()));
            }
            pageRequest = pageRequest.next();
        } while (rewardPage.hasNext());
    }

    private void loadOffers() {
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<MerchantOffer> offerPage;
        do {
            offerPage = merchantOfferRepository.findAll(pageRequest);
            for (MerchantOffer offer : offerPage) {
                warehouseJdbcTemplate.update("""
                        MERGE INTO dim_merchant_offers AS target
                        USING (SELECT CAST(:offerId AS VARCHAR(36)) AS offer_id,
                                      CAST(:merchantId AS VARCHAR(36)) AS merchant_id,
                                      :cashbackRate AS cashback_rate, :expiryDate AS expiry_date,
                                      :active AS active) AS source
                        ON target.offer_id = source.offer_id
                        WHEN MATCHED THEN UPDATE SET merchant_id = source.merchant_id,
                            cashback_rate = source.cashback_rate, expiry_date = source.expiry_date,
                            active = source.active
                        WHEN NOT MATCHED THEN INSERT (offer_id, merchant_id, cashback_rate, expiry_date, active)
                        VALUES (source.offer_id, source.merchant_id, source.cashback_rate,
                                source.expiry_date, source.active)
                        """, new MapSqlParameterSource()
                        .addValue("offerId", offer.getId())
                        .addValue("merchantId", offer.getMerchant().getId())
                        .addValue("cashbackRate", offer.getCashbackRate())
                        .addValue("expiryDate", offer.getExpiryDate())
                        .addValue("active", offer.getActive()));
            }
            pageRequest = pageRequest.next();
        } while (offerPage.hasNext());
    }

    public void exportDailyReport() {
        ensureWarehouseSchema();
        warehouseJdbcTemplate.update("""
                MERGE INTO daily_transaction_reports AS target
                USING (
                    SELECT CAST(:reportDate AS DATE) AS report_date,
                           COUNT(*) AS transaction_count,
                           COALESCE(SUM(amount), 0) AS total_spend,
                           COALESCE(SUM(cashback_amount), 0) AS total_cashback,
                           COALESCE(SUM(merchant_payout), 0) AS total_merchant_payout
                    FROM fact_transactions
                ) AS source
                ON target.report_date = source.report_date
                WHEN MATCHED THEN UPDATE SET
                    transaction_count = source.transaction_count,
                    total_spend = source.total_spend,
                    total_cashback = source.total_cashback,
                    total_merchant_payout = source.total_merchant_payout
                WHEN NOT MATCHED THEN INSERT
                    (report_date, transaction_count, total_spend, total_cashback, total_merchant_payout)
                VALUES (source.report_date, source.transaction_count, source.total_spend,
                        source.total_cashback, source.total_merchant_payout)
                """, new MapSqlParameterSource("reportDate", LocalDate.now()));
    }

    private Watermark readWatermark() {
        ResultSetExtractor<Watermark> extractor = rows -> rows.next()
                ? new Watermark(rows.getTimestamp(1).toLocalDateTime(),
                        rows.getString(2) == null ? new UUID(0, 0) : UUID.fromString(rows.getString(2)))
                : new Watermark(LocalDateTime.of(1970, 1, 1, 0, 0), new UUID(0, 0));
        return warehouseJdbcTemplate.query("""
                    SELECT last_created_at, last_transaction_id
                    FROM etl_watermarks WHERE pipeline_name = :pipelineName
                """, new MapSqlParameterSource("pipelineName", WATERMARK_KEY), extractor);
    }

    private record Watermark(LocalDateTime createdAt, UUID id) {
    }

    private void ensureWarehouseSchema() {
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS etl_watermarks (
                    pipeline_name VARCHAR(100) PRIMARY KEY,
                    last_created_at TIMESTAMP NOT NULL,
                    last_transaction_id VARCHAR(36)
                )
                """);
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                ALTER TABLE etl_watermarks ADD COLUMN IF NOT EXISTS last_transaction_id VARCHAR(36)
                """);
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS fact_transactions (
                    transaction_id VARCHAR(36) PRIMARY KEY,
                    user_id VARCHAR(36) NOT NULL,
                    merchant_id VARCHAR(36) NOT NULL,
                    offer_id VARCHAR(36),
                    amount NUMERIC(18,2) NOT NULL,
                    transaction_type VARCHAR(50) NOT NULL,
                    cashback_amount NUMERIC(18,2) NOT NULL,
                    merchant_payout NUMERIC(18,2) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                ALTER TABLE fact_transactions ADD COLUMN IF NOT EXISTS offer_id VARCHAR(36)
                """);
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS daily_transaction_reports (
                    report_date DATE PRIMARY KEY,
                    transaction_count BIGINT NOT NULL,
                    total_spend NUMERIC(18,2) NOT NULL,
                    total_cashback NUMERIC(18,2) NOT NULL,
                    total_merchant_payout NUMERIC(18,2) NOT NULL
                )
                """);
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS fact_rewards (
                    reward_id VARCHAR(36) PRIMARY KEY,
                    user_id VARCHAR(36) NOT NULL,
                    amount NUMERIC(18,2) NOT NULL,
                    description VARCHAR(500),
                    created_at TIMESTAMP NOT NULL
                )
                """);
        warehouseJdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS dim_merchant_offers (
                    offer_id VARCHAR(36) PRIMARY KEY,
                    merchant_id VARCHAR(36) NOT NULL,
                    cashback_rate NUMERIC(10,6) NOT NULL,
                    expiry_date DATE,
                    active BOOLEAN NOT NULL
                )
                """);
    }
}