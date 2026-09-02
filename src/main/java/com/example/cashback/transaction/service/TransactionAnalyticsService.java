package com.example.cashback.transaction.service;

import com.example.cashback.transaction.dto.TransactionAnalyticsDTO;
import com.example.cashback.analytics.repository.WarehouseAnalyticsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionAnalyticsService {

    private final WarehouseAnalyticsRepository warehouseAnalyticsRepository;

    public TransactionAnalyticsService(WarehouseAnalyticsRepository warehouseAnalyticsRepository) {
        this.warehouseAnalyticsRepository = warehouseAnalyticsRepository;
    }

    public List<TransactionAnalyticsDTO> getTransactionAnalytics() {
        List<TransactionAnalyticsDTO> analytics = new ArrayList<>();

        for (Object[] row : warehouseAnalyticsRepository.totalAmountsPerUser()) {
            analytics.add(new TransactionAnalyticsDTO((UUID) row[0], (Long) row[1],
                    (java.math.BigDecimal) row[2], (java.math.BigDecimal) row[3]));
        }

        return analytics;
    }
}
