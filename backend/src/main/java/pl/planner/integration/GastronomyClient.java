package pl.planner.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import pl.planner.config.IntegrationProperties;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.MealProposal;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class GastronomyClient implements PlannerSources.Gastronomy {
    private final IntegrationHttp.Endpoint endpoint;

    public GastronomyClient(IntegrationHttp http, IntegrationProperties properties) {
        this.endpoint = http.endpoint("gastronomy", "Gastronomia", properties.gastronomy());
    }

    @Override
    public List<MealProposal> proposals(String fromLocationId, PlanRequest request) {
        var response = endpoint.exchange(client -> client.get()
                .uri(builder -> builder.path("/api/gastronomy/proposals")
                        .queryParam("fromLocationId", "{from}")
                        .queryParam("mode", request.transportMode().name())
                        .queryParamIfPresent("diet", Optional.ofNullable(request.diet()))
                        .build(fromLocationId))
                .retrieve()
                .onStatus(status -> status.value() == 503, GastronomyClient::serviceUnavailable)
                .bodyToMono(ProposalsResponse.class), false);
        if (!fromLocationId.equals(response.fromLocationId()) || request.transportMode() != response.mode()
                || response.proposals() == null) {
            throw invalidResponse();
        }
        return response.proposals().stream().map(item -> map(item, fromLocationId, request)).toList();
    }

    private static MealProposal map(Proposal item, String fromLocationId, PlanRequest request) {
        if (item == null || missing(item.placeName()) || missing(item.locationId()) || missing(item.mealId())
                || missing(item.mealName()) || item.visitDurationMin() == null || item.visitDurationMin() <= 0
                || item.travelDurationMin() == null || item.travelDurationMin() < 0
                || item.mealCostGrosze() == null || item.mealCostGrosze() < 0
                || item.travelCostGrosze() == null || item.travelCostGrosze() < 0 || item.transportFromCache() == null) {
            throw invalidResponse();
        }
        try {
            var arrival = new Journey(item.travelDurationMin(), null,
                    Math.multiplyExact(item.travelCostGrosze(), (long) request.groupSize()), request.transportMode().name(),
                    List.of(), !PlannerSources.Transport.START.equals(fromLocationId));
            return new MealProposal(item.locationId(), item.mealId(), item.placeName(), item.mealName(),
                    item.visitDurationMin(), Math.multiplyExact(item.mealCostGrosze(), (long) request.groupSize()),
                    arrival, item.transportFromCache(), null);
        } catch (ArithmeticException overflow) {
            throw invalidResponse();
        }
    }

    private static Mono<IntegrationFailure> serviceUnavailable(ClientResponse response) {
        String retryAfter = response.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        var generic = new IntegrationFailure("Gastronomia", Reason.UNAVAILABLE);
        return response.bodyToMono(ErrorPayload.class)
                .onErrorResume(error -> Mono.empty())
                .map(error -> {
                    if (!"TRANSPORT_UNAVAILABLE".equals(error.error())) return generic;
                    var message = "Transport jest niedostępny. Gastronomia nie mogła ustalić dojazdu do lokalu.";
                    if (retryAfter != null && retryAfter.matches("[0-9]{1,9}")) {
                        message += " Spróbuj ponownie za " + retryAfter + " s.";
                    }
                    return IntegrationFailure.unavailable(message);
                })
                .defaultIfEmpty(generic);
    }

    private static boolean missing(String value) {
        return value == null || value.isBlank();
    }

    private static IntegrationFailure invalidResponse() {
        return new IntegrationFailure("Gastronomia", Reason.INVALID_RESPONSE);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProposalsResponse(String fromLocationId, TransportMode mode, List<Proposal> proposals) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Proposal(String placeName, String locationId, String mealId, String mealName,
                    Long mealCostGrosze, Integer visitDurationMin, Integer travelDurationMin,
                    Long travelCostGrosze, Boolean transportFromCache) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorPayload(String error) {}
}
