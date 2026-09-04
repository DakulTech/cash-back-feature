package com.example.cashback.referral.repository;

import com.example.cashback.referral.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {

        // Find referrals by referrer
        List<Referral> findByReferrerId(UUID referrerId);

        boolean existsByReferredUserId(UUID referredUserId);

        @Query("SELECT COALESCE(SUM(r.bonusAmount), 0) FROM Referral r WHERE r.referrerId = :referrerId")
        BigDecimal totalBonusByReferrerId(@Param("referrerId") UUID referrerId);

        @Modifying
        @Query(value = "INSERT INTO referral_bonus_totals(referrer_id, total_bonus) VALUES (:referrerId, 0) "
                        + "ON CONFLICT (referrer_id) DO NOTHING", nativeQuery = true)
        void initializeBonusTotal(@Param("referrerId") UUID referrerId);

        @Modifying
        @Query(value = "UPDATE referral_bonus_totals SET total_bonus = total_bonus + :bonus "
                        + "WHERE referrer_id = :referrerId AND total_bonus + :bonus <= :maximumBonus", nativeQuery = true)
        int reserveBonus(@Param("referrerId") UUID referrerId, @Param("bonus") BigDecimal bonus,
                        @Param("maximumBonus") BigDecimal maximumBonus);

        @Query(value = "SELECT total_bonus FROM referral_bonus_totals WHERE referrer_id = :referrerId", nativeQuery = true)
        BigDecimal reservedBonusTotal(@Param("referrerId") UUID referrerId);

        // Find referrals by referee
        @Query("SELECT r FROM Referral r WHERE r.referredUserId = :refereeId")
        List<Referral> findByRefereeId(@Param("refereeId") UUID refereeId);

        // Find referrals by status
        List<Referral> findByStatus(String status);

        // Count referrals per referrer
        @Query("SELECT r.referrerId, COUNT(r) FROM Referral r GROUP BY r.referrerId")
        List<Object[]> countReferralsPerReferrer();

        // Count successful referrals per referrer
        @Query("SELECT r.referrerId, COUNT(r) FROM Referral r WHERE r.status = 'SUCCESS' GROUP BY r.referrerId")
        List<Object[]> countSuccessfulReferralsPerReferrer();

        // Find top N referrers by number of referrals
        @Query("SELECT r.referrerId, COUNT(r) FROM Referral r GROUP BY r.referrerId ORDER BY COUNT(r) DESC")
        List<Object[]> topReferrers(org.springframework.data.domain.Pageable pageable);

        // Find referrals created in a given period
        @Query("SELECT r FROM Referral r WHERE r.createdAt BETWEEN :start AND :end")
        List<Referral> findReferralsInPeriod(@Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        // Count referrals by status
        @Query("SELECT r.status, COUNT(r) FROM Referral r GROUP BY r.status")
        List<Object[]> countReferralsByStatus();
}
