package pl.planner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("planner")
public record PlannerProperties(boolean demo, String frontendOrigin) {}
