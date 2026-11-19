package pl.planner.integration;

import java.time.LocalDate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.planner.config.IntegrationProperties;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class WeatherClient implements PlannerSources.Forecast {
    /** Implement this mapper once the weather team delivers the day response schema. */
    public interface ResponseMapper {
        Weather day(JsonNode payload);
    }

    private final IntegrationHttp.Endpoint endpoint;
    private final ObjectProvider<ResponseMapper> mappers;

    public WeatherClient(IntegrationHttp http, IntegrationProperties properties, ObjectProvider<ResponseMapper> mappers) {
        this.endpoint = http.endpoint("Pogoda", properties.weather());
        this.mappers = mappers;
    }

    @Override
    public Weather day(String city, LocalDate date) {
        endpoint.requireConfigured();
        var mapper = mappers.getIfAvailable();
        if (mapper == null) throw endpoint.waitingForContract();
        var payload = endpoint.exchange(client -> client.get()
                .uri(builder -> builder.path("/api/weather/day")
                        .queryParam("city", "{city}").queryParam("date", "{date}").build(city, date.toString()))
                .retrieve().bodyToMono(JsonNode.class), false);
        try {
            return mapper.day(payload);
        } catch (IllegalArgumentException error) {
            throw new IntegrationFailure("Pogoda", Reason.INVALID_RESPONSE, error);
        }
    }
}
