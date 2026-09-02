package com.example.cashback.merchant.repository;

import com.example.cashback.merchant.model.MerchantOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantOfferRepository extends JpaRepository<MerchantOffer, UUID> {

    List<MerchantOffer> findByMerchantId(UUID merchantId);

    List<MerchantOffer> findByActiveTrue();

}
