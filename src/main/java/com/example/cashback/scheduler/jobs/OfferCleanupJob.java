package com.example.cashback.scheduler.jobs;

import com.example.cashback.merchant.repository.MerchantOfferRepository;
import com.example.cashback.scheduler.metrics.JobMetrics;

import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class OfferCleanupJob implements Job {

    private static final int PAGE_SIZE = 500;

    private final MerchantOfferRepository offerRepository;
    private final JobMetrics jobMetrics;

    @Override
    public void execute(JobExecutionContext context) {
        Pageable pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<com.example.cashback.merchant.model.MerchantOffer> offers;
        do {
            offers = offerRepository.findAll(pageRequest);
            offers.forEach(offer -> {
                if (offer.getExpiryDate() != null && offer.getExpiryDate().isBefore(LocalDate.now())) {
                    offer.setActive(false);
                }
            });
            offerRepository.saveAll(offers.getContent());
            pageRequest = pageRequest.next();
        } while (offers.hasNext());
        System.out.println("Offer Cleanup Job executed.");

        // Record metric
        jobMetrics.recordJobExecution("OfferCleanupJob");
    }
}
