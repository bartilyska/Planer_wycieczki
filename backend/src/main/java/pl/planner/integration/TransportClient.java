package pl.planner.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import pl.planner.config.IntegrationProperties;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.LocationPair;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class TransportClient implements PlannerSources.Transport {
    private static final int MAX_PAIRS = 500;
    private final IntegrationHttp.Endpoint endpoint;
    private final Map<String, String> locationIds;

    public TransportClient(IntegrationHttp http, IntegrationProperties properties,
                           @Value("${integration.transport.location-id-map:}") String locationIdMap) {
        this.endpoint = http.endpoint("transport", "Transport", properties.transport());
        this.locationIds = parseLocationIds(locationIdMap);
    }

    @Override
    public Map<LocationPair, Journey> routes(Map<String, Place> locations, List<LocationPair> requested,
                                             TransportMode mode, int people) {
        var journeys = new HashMap<LocationPair, Journey>();
        var distinctLocations = requested.stream().filter(pair -> !resolve(pair.from()).equals(resolve(pair.to()))).toList();
        for (int offset = 0; offset < distinctLocations.size(); offset += MAX_PAIRS) {
            var batch = distinctLocations.subList(offset, Math.min(offset + MAX_PAIRS, distinctLocations.size()));
            var pairs = batch.stream().map(pair -> new Pair(resolve(pair.from()), resolve(pair.to()))).toList();
            var response = endpoint.exchange(client -> client.post().uri("/api/v1/routes")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(new RoutesRequest(mode, pairs))
                    .retrieve().bodyToMono(RoutesResponse.class), false);
            if (response.mode() != mode || response.results() == null || response.results().size() != pairs.size()) {
                throw invalidResponse();
            }
            for (int index = 0; index < pairs.size(); index++) {
                var journey = map(response.results().get(index), pairs.get(index), mode, people);
                if (journey != null) journeys.put(batch.get(index), journey);
            }
        }
        return Map.copyOf(journeys);
    }

    private String resolve(String locationId) {
        return locationIds.getOrDefault(locationId, locationId);
    }

    private static Journey map(Result result, Pair pair, TransportMode mode, int people) {
        if (result == null || !pair.fromLocationId().equals(result.fromLocationId())
                || !pair.toLocationId().equals(result.toLocationId())) {
            throw invalidResponse();
        }
        if ("NO_CONNECTION".equals(result.status())) return null;
        if (!"OK".equals(result.status()) || result.durationMin() == null || result.durationMin() < 0
                || result.costGrosze() == null || result.costGrosze() < 0) {
            throw invalidResponse();
        }
        try {
            return new Journey(result.durationMin(), null, Math.multiplyExact(result.costGrosze(), (long) people),
                    mode.name(), List.of());
        } catch (ArithmeticException overflow) {
            throw invalidResponse();
        }
    }

    private static Map<String, String> parseLocationIds(String mapping) {
        if (mapping.isBlank()) return Map.of();
        var result = new HashMap<String, String>();
        for (String entry : mapping.split(",", -1)) {
            var pair = entry.split("=", -1);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()
                    || START.equals(pair[0].strip()) || START.equals(pair[1].strip())
                    || result.putIfAbsent(pair[0].strip(), pair[1].strip()) != null) {
                throw new IllegalArgumentException("TRANSPORT_LOCATION_ID_MAP wymaga par źródło=cel oddzielonych przecinkami.");
            }
        }
        return Map.copyOf(result);
    }

    private static IntegrationFailure invalidResponse() {
        return new IntegrationFailure("Transport", Reason.INVALID_RESPONSE);
    }

    record Pair(String fromLocationId, String toLocationId) {}
    record RoutesRequest(TransportMode mode, List<Pair> pairs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoutesResponse(TransportMode mode, List<Result> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(String fromLocationId, String toLocationId, String status, Integer durationMin, Long costGrosze) {}
}
