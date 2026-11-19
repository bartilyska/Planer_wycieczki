package pl.planner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.discovery")
public record DiscoveryProperties(boolean enabled, String baseUrl) {}
