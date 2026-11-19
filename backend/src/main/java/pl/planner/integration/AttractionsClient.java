package pl.planner.integration;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import pl.planner.config.IntegrationProperties;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanRequest;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.Category;
import pl.planner.integration.PlannerSources.DayData;
import pl.planner.mapper.AttractionMapper;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class AttractionsClient implements PlannerSources.Attractions {
    private static final String CATALOG = "/api/v1/attractions";
    private static final ParameterizedTypeReference<List<AttractionPayload.Category>> CATEGORY_LIST =
            new ParameterizedTypeReference<>() {};
    private final IntegrationHttp.Endpoint endpoint;
    private final AttractionMapper mapper;
    private final String token;

    public AttractionsClient(IntegrationHttp http, IntegrationProperties properties, AttractionMapper mapper,
                             @Value("${integration.attractions.token:}") String token) {
        this.endpoint = http.endpoint("attractions", "Atrakcje", properties.attractions());
        this.mapper = mapper;
        this.token = token.strip();
    }

    @Override
    public List<Attraction> list() {
        var items = new ArrayList<Attraction>();
        for (int page = 0; ; page++) {
            int pageNumber = page;
            var result = endpoint.exchange(client -> client.get()
                    .uri(builder -> builder.path(CATALOG).queryParam("page", pageNumber).queryParam("size", 100).build())
                    .headers(this::authorize).retrieve().bodyToMono(AttractionPayload.CatalogPage.class), false);
            if (result.items() == null || result.page() == null || result.page() != pageNumber
                    || result.totalPages() == null || result.totalPages() < 0
                    || (result.items().isEmpty() && pageNumber + 1 < result.totalPages())) {
                throw new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
            }
            result.items().stream().map(mapper::catalog).forEach(items::add);
            if (pageNumber + 1 >= result.totalPages()) return List.copyOf(items);
        }
    }

    @Override
    public Attraction detail(String id) {
        var payload = endpoint.exchange(client -> client.get()
                .uri(builder -> builder.path(CATALOG).pathSegment("{id}").build(id))
                .headers(this::authorize).retrieve().bodyToMono(AttractionPayload.CatalogItem.class), true);
        if (!id.equals(payload.id())) throw new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
        return mapper.catalog(payload);
    }

    @Override
    public List<Category> categories() {
        var entries = endpoint.exchange(client -> client.get().uri("/api/v1/categories")
                .headers(this::authorize).retrieve().bodyToMono(CATEGORY_LIST), false);
        return entries.stream().map(value -> {
            if (value == null || value.code() == null || value.code().isBlank()
                    || value.name() == null || value.name().isBlank()) {
                throw new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
            }
            return new Category(value.code(), value.name());
        }).toList();
    }

    @Override
    public DayData day(PlanRequest request) {
        var mode = switch (request.transportMode()) {
            case WALK -> "walk";
            case PUBLIC_TRANSPORT -> "transit";
            case CAR -> "car";
            case BIKE -> throw PlanProblem.invalid("Atrakcje obsługują dojście pieszo, komunikację miejską i samochód.");
        };
        var payload = endpoint.exchange(client -> client.get()
                .uri(builder -> builder.path(CATALOG + "/proposals")
                        .queryParam("date", request.date().toString())
                        .queryParam("mode", mode)
                        .queryParam("startLat", request.startLocation().lat())
                        .queryParam("startLng", request.startLocation().lng())
                        .queryParam("people", request.groupSize())
                        .queryParam("limit", 50).build())
                .headers(this::authorize)
                .retrieve()
                .onStatus(status -> status.value() == 400 || status.value() == 422, response ->
                        response.bodyToMono(AttractionPayload.ErrorPayload.class)
                                .onErrorResume(error -> Mono.empty())
                                .<RuntimeException>map(AttractionsClient::proposalError)
                                .defaultIfEmpty(PlanProblem.invalid("Serwis Atrakcji odrzucił parametry planu.")))
                .bodyToMono(AttractionPayload.Day.class), false);
        return mapper.day(payload, request);
    }

    private void authorize(HttpHeaders headers) {
        if (!token.isEmpty()) headers.setBearerAuth(token);
    }

    private static RuntimeException proposalError(AttractionPayload.ErrorPayload response) {
        var error = response.error();
        if (error != null && "START_NOT_RESOLVED".equals(error.code())) {
            return PlanProblem.invalid("Punkt startowy jest poza obszarem obsługiwanym przez Atrakcje. "
                    + "Wybierz punkt w pobliżu współrzędnych 52.2297, 21.0122.");
        }
        if (error != null && "VALIDATION_ERROR".equals(error.code()) && error.field() != null) {
            return PlanProblem.invalid("Serwis Atrakcji odrzucił parametr „" + error.field() + "”.");
        }
        return PlanProblem.invalid("Serwis Atrakcji odrzucił parametry planu.");
    }
}
