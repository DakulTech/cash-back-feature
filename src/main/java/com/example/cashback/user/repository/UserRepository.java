package com.example.cashback.user.repository;

import com.example.cashback.user.model.User;
import com.example.cashback.user.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    List<User> findByRole(Role role);

    List<User> findByReferredBy(User referredBy);

    List<User> findByRewardBalanceGreaterThan(BigDecimal balance);

    // Count users by role
    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countUsersByRole();

    // Find top N users by reward balance
    @Query("SELECT u FROM User u ORDER BY u.rewardBalance DESC")
    List<User> findTopUsersByRewardBalance(org.springframework.data.domain.Pageable pageable);

    // Count referrals per user
    @Query("SELECT u.fullName, COUNT(r) FROM User u LEFT JOIN User r ON r.referredBy = u GROUP BY u.fullName")
    List<Object[]> countReferralsPerUser();

    // Find users with no referrals
    @Query("SELECT u FROM User u WHERE u.referredBy IS NULL")
    List<User> findUsersWithoutReferrals();

    // Average reward balance across all users
    @Query("SELECT AVG(u.rewardBalance) FROM User u")
    BigDecimal averageRewardBalance();

    // Find users by role and minimum reward balance
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.rewardBalance > :balance")
    List<User> findUsersByRoleAndBalance(@Param("role") Role role, @Param("balance") BigDecimal balance);
}
