package com.businessmanager.backend.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_METRICS_CACHE = "dashboardMetrics";
    public static final String DASHBOARD_ALERTS_CACHE = "dashboardAlerts";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DASHBOARD_METRICS_CACHE, DASHBOARD_ALERTS_CACHE);
        // Short-lived cache: 30 seconds to prevent expensive recomputations on repeated widget refreshes
        // while guaranteeing data is nearly real-time.
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(100));
        return cacheManager;
    }
}
