package com.example.cashback.merchant.controller;

import com.example.cashback.merchant.dto.OfferAnalyticsDTO;
import com.example.cashback.merchant.model.Merchant;
import com.example.cashback.merchant.model.MerchantOffer;
import com.example.cashback.merchant.service.MerchantService;
import com.example.cashback.merchant.service.MerchantOfferAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantOfferAnalyticsService analyticsService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Merchant> register(@RequestBody Merchant merchant) {
        return ResponseEntity.ok(merchantService.registerMerchant(merchant));
    }

    @PostMapping("/{merchantId}/offers")
    @PreAuthorize("@accessControl.canAccessMerchant(#merchantId)")
    public ResponseEntity<MerchantOffer> createOffer(
            @PathVariable UUID merchantId,
            @Valid @RequestBody MerchantOffer offer) {
        offer.setMerchant(Merchant.builder().id(merchantId).build());
        return ResponseEntity.ok(merchantService.createOffer(offer));
    }

    @GetMapping("/{merchantId}/offers")
    @PreAuthorize("@accessControl.canAccessMerchant(#merchantId)")
    public ResponseEntity<List<MerchantOffer>> getOffers(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(merchantService.getOffers(merchantId));
    }

    // Unified analytics endpoint
    @GetMapping("/offers/analytics")
    public ResponseEntity<List<OfferAnalyticsDTO>> getOfferAnalytics() {
        return ResponseEntity.ok(analyticsService.getOfferAnalytics());
    }
}
