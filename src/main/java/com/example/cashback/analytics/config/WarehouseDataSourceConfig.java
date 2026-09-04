package com.example.cashback.analytics.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class WarehouseDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    @org.springframework.context.annotation.Primary
    public DataSourceProperties applicationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public DataSource dataSource(
            @Qualifier("applicationDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("warehouse.datasource")
    public DataSourceProperties warehouseDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource warehouseDataSource(
            @Qualifier("warehouseDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public NamedParameterJdbcTemplate warehouseJdbcTemplate(
            @Qualifier("warehouseDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}