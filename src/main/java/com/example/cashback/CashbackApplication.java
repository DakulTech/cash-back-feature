package com.example.cashback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching // Enables Redis caching
@EnableMethodSecurity // Enables @PreAuthorize for RBAC
@EnableScheduling
public class CashbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashbackApplication.class, args);
    }
}
