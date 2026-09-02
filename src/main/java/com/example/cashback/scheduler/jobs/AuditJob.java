package com.example.cashback.scheduler.jobs;

import com.example.cashback.user.repository.UserRepository;
import com.example.cashback.referral.repository.ReferralRepository;
import com.example.cashback.scheduler.metrics.JobMetrics;
import com.example.cashback.audit.service.AuditEventService;

import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class AuditJob implements Job {

    private static final int PAGE_SIZE = 500;

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final JobMetrics jobMetrics;
    private final AuditEventService auditEventService;

    @Override
    public void execute(JobExecutionContext context) {
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<com.example.cashback.user.model.User> users;
        do {
            users = userRepository.findAll(pageRequest);
            users.forEach(user -> {
                if (user.getRewardBalance() != null && user.getRewardBalance().compareTo(BigDecimal.ZERO) < 0) {
                    auditEventService.recordEventIfAbsent(
                            "NEGATIVE_BALANCE",
                            "AuditJob",
                            "User " + user.getEmail() + " has balance " + user.getRewardBalance());
                }
            });
            pageRequest = pageRequest.next();
        } while (users.hasNext());

        long totalReferrals = referralRepository.count();
        Set<java.util.UUID> referredUsers = new HashSet<>();
        pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<com.example.cashback.referral.model.Referral> referrals;
        do {
            referrals = referralRepository.findAll(pageRequest);
            referrals.forEach(referral -> referredUsers.add(referral.getReferredUserId()));
            pageRequest = pageRequest.next();
        } while (referrals.hasNext());
        long uniqueReferrals = referredUsers.size();

        if (uniqueReferrals < totalReferrals) {
            auditEventService.recordEventIfAbsent(
                    "DUPLICATE_REFERRAL",
                    "AuditJob",
                    "Detected " + (totalReferrals - uniqueReferrals) + " duplicate referrals");
        }

        // Record metric for Prometheus
        jobMetrics.recordJobExecution("AuditJob");
    }
}
