package com.example.cashback.merchant.service;

import com.example.cashback.merchant.model.Merchant;
import com.example.cashback.merchant.model.MerchantOffer;
import com.example.cashback.merchant.repository.MerchantRepository;
import com.example.cashback.merchant.repository.MerchantOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantOfferRepository offerRepository;
    private final MerchantOfferCacheService offerCacheService;

    public Merchant registerMerchant(Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    public MerchantOffer createOffer(MerchantOffer offer) {
        MerchantOffer savedOffer = offerRepository.save(offer);
        if (savedOffer.getMerchant() != null) {
            offerCacheService.evict(savedOffer.getMerchant().getId());
        }
        return savedOffer;
    }

    public List<MerchantOffer> getOffers(UUID merchantId) {
        return offerCacheService.getActiveOffers(merchantId);
    }
}
