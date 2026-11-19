package pl.planner.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration")
public record IntegrationProperties(Service attractions, Service weather, Service transport, Service gastronomy) {
    public record Service(String baseUrl, Duration timeout) {
        public boolean configured() {
            return baseUrl != null && !baseUrl.isBlank();
        }
    }
}
