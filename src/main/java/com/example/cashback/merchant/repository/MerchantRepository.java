package com.example.cashback.merchant.repository;

import com.example.cashback.merchant.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    // Find merchant by email
    Optional<Merchant> findByEmail(String email);

    // Find merchants by category
    List<Merchant> findByCategory(String category);

    // Find merchants by country
    List<Merchant> findByCountry(String country);

    // Find merchants by name (case-insensitive search)
    List<Merchant> findByNameContainingIgnoreCase(String name);

    // Count merchants by category
    @Query("SELECT m.category, COUNT(m) FROM Merchant m GROUP BY m.category")
    List<Object[]> countMerchantsByCategory();

    // Count merchants by country
    @Query("SELECT m.country, COUNT(m) FROM Merchant m GROUP BY m.country")
    List<Object[]> countMerchantsByCountry();

    // Find top N categories by merchant count
    @Query("SELECT m.category, COUNT(m) FROM Merchant m GROUP BY m.category ORDER BY COUNT(m) DESC")
    List<Object[]> topCategoriesByMerchantCount(org.springframework.data.domain.Pageable pageable);
}
