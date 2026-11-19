package pl.planner.integration;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import pl.planner.config.IntegrationProperties;
import pl.planner.dto.Attraction;
import pl.planner.exception.IntegrationFailure;
import pl.planner.mapper.AttractionMapper;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class AttractionsClient implements PlannerSources.Attractions {
    private static final ParameterizedTypeReference<List<AttractionPayload>> CATALOG = new ParameterizedTypeReference<>() {};
    private final IntegrationHttp.Endpoint endpoint;
    private final AttractionMapper mapper;

    public AttractionsClient(IntegrationHttp http, IntegrationProperties properties, AttractionMapper mapper) {
        this.endpoint = http.endpoint("Atrakcje", properties.attractions());
        this.mapper = mapper;
    }

    @Override
    public List<Attraction> list() {
        var payload = endpoint.exchange(client -> client.get().uri("/attractions/proposal")
                .retrieve().bodyToMono(CATALOG), false);
        if (payload.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IntegrationFailure("Atrakcje", IntegrationFailure.Reason.INVALID_RESPONSE);
        }
        return payload.stream().map(mapper::map).toList();
    }

    @Override
    public Attraction detail(String id) {
        var payload = endpoint.exchange(client -> client.get()
                .uri(builder -> builder.pathSegment("attractions", "proposal", "{id}").build(id))
                .retrieve().bodyToMono(AttractionPayload.class), true);
        if (!id.equals(payload.id())) {
            throw new IntegrationFailure("Atrakcje", IntegrationFailure.Reason.INVALID_RESPONSE);
        }
        return mapper.map(payload);
    }
}
