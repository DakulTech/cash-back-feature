package com.example.cashback.referral.controller;

import com.example.cashback.referral.model.Referral;
import com.example.cashback.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping
    @PreAuthorize("@accessControl.canAccessUser(#referredUserId)")
    public ResponseEntity<Referral> createReferral(
            @RequestParam String referralCode,
            @RequestParam UUID referredUserId,
            @RequestParam(defaultValue = "false") boolean isUKReferral) {
        return ResponseEntity.ok(referralService.registerReferral(referralCode, referredUserId, isUKReferral));
    }

    @GetMapping("/{referrerId}")
    @PreAuthorize("@accessControl.canAccessUser(#referrerId)")
    public ResponseEntity<List<Referral>> getReferrals(@PathVariable UUID referrerId) {
        return ResponseEntity.ok(referralService.getReferrals(referrerId));
    }
}
