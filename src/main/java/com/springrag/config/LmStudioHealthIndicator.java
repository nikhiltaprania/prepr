package com.springrag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Actuator health indicator for the LM Studio inference server — the platform's most critical
 * external dependency. Surfaces under {@code /actuator/health} as the {@code lmStudio} component.
 * Reuses {@code spring.ai.openai.base-url} and checks reachability with a short timeout so a hung
 * server cannot block the health endpoint.
 */
@Component
public class LmStudioHealthIndicator implements HealthIndicator {

    private static final int TIMEOUT_MILLIS = 3000;

    private final RestClient restClient;
    private final String baseUrl;

    public LmStudioHealthIndicator(@Value("${spring.ai.openai.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public Health health() {
        try {
            restClient.get().uri("/models").retrieve().toBodilessEntity();
            return Health.up().withDetail("baseUrl", baseUrl).build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("error", ex.getClass().getSimpleName() + ": " + ex.getMessage())
                    .build();
        }
    }
}
