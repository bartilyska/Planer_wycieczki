package pl.planner.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final PlannerProperties properties;

    public WebConfiguration(PlannerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(StringUtils.tokenizeToStringArray(properties.frontendOrigin(), ","))
                .allowedMethods("GET", "POST", "OPTIONS");
    }

    @Bean
    Clock plannerClock() {
        return Clock.system(ZoneId.of("Europe/Warsaw"));
    }
}
