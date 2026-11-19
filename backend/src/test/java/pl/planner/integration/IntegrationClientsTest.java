package pl.planner.integration;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pl.planner.config.DiscoveryProperties;
import pl.planner.config.IntegrationProperties;
import pl.planner.config.IntegrationProperties.Service;
import pl.planner.config.PlannerProperties;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Condition;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.exception.PlanProblem;
import pl.planner.mapper.AttractionMapper;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntegrationClientsTest {
    private final ExchangeFunction network = mock(ExchangeFunction.class);
    private final WebClient.Builder builder = WebClient.builder().exchangeFunction(network);
    private final IntegrationHttp http = new IntegrationHttp(builder, mock(DiscoveryClient.class));
    private final Service configured = new Service("http://172.20.147.24:8000", Duration.ofSeconds(5));
    private final IntegrationProperties properties = new IntegrationProperties(configured, configured,
            new Service("", Duration.ofSeconds(5)));

    @Test
    void readsCatalogDetailsAndCategoriesFromVersionedEndpoints() {
        when(network.exchange(any())).thenAnswer(call -> {
            var request = call.getArgument(0, ClientRequest.class);
            assertEquals("Bearer example-token", request.headers().getFirst(HttpHeaders.AUTHORIZATION));
            return switch (request.url().getPath()) {
                case "/api/v1/attractions" -> response(HttpStatus.OK, """
                        {"items":[{"id":"ATR01","name":"Muzeum","category":"museum","indoor":true,
                          "pricePerPerson":25.15,"visitDurationMin":90,"location":{"id":"attr-01","lat":52.22,"lng":21.01}}],
                          "page":0,"totalPages":1}
                        """);
                case "/api/v1/attractions/ATR01" -> response(HttpStatus.OK, """
                        {"id":"ATR01","name":"Muzeum","category":"museum","visitDurationMin":90,
                        "description":"Kolekcja","openingHours":{"SUNDAY":{"open":"10:00","close":"18:00"}}}
                        """);
                case "/api/v1/categories" -> response(HttpStatus.OK, """
                        [{"code":"museum","name":"Muzeum"}]
                        """);
                default -> response(HttpStatus.NOT_FOUND, "{}");
            };
        });
        var client = new AttractionsClient(http, properties, new AttractionMapper(), "example-token");

        var item = client.list().getFirst();
        assertEquals("ATR01", item.id());
        assertTrue(item.indoor());
        assertEquals(2515L, item.priceGroszy().longValue());
        assertEquals(new Place(52.22, 21.01), item.coordinates());
        assertEquals("attr-01", item.transportLocationId());
        assertEquals(LocalTime.of(10, 0), client.detail("ATR01").weeklyOpeningHours().get(DayOfWeek.SUNDAY).from());
        assertEquals("Muzeum", client.categories().getFirst().name());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(PlanProblem.class, () -> client.detail("missing")).status());
    }

    @Test
    void usesRealDayParametersWeatherGroupPricesAndProposalErrors() {
        when(network.exchange(any())).thenAnswer(call -> {
            var request = call.getArgument(0, ClientRequest.class);
            assertEquals("/api/v1/attractions/proposals", request.url().getPath());
            assertEquals("date=2026-11-22&mode=transit&startLat=52.2297&startLng=21.0122&people=3&limit=50",
                    request.url().getQuery());
            return response(HttpStatus.OK, """
                    {"date":"2026-11-22","weather":{"condition":"rain","tempC":5,"source":"ok"},
                    "transport":{"source":"ok"},"proposals":[{
                      "id":"ATR01","name":"Muzeum","category":"museum","indoor":true,
                      "location":{"id":"attr-01","lat":52.2316,"lng":21.0247},"visitDurationMin":90,
                      "openingHours":{"open":"10:00","close":"18:00"},
                      "ticket":{"perPerson":25.00,"total":75.00},
                      "fromStart":{"timeMin":9,"costTotal":14.40},"toStart":{"timeMin":11,"costTotal":12.00}
                    }]}
                    """);
        });
        var client = new AttractionsClient(http, properties, new AttractionMapper(), "");
        var request = new PlanRequest("Warszawa", LocalDate.of(2026, 11, 22), LocalTime.of(9, 0), null,
                new Place(52.2297, 21.0122), List.of("ATR01"), false, TransportMode.PUBLIC_TRANSPORT, true, 3);

        var day = client.day(request);
        assertEquals(Condition.ADVERSE_OUTDOOR, day.weather().condition());
        assertEquals(5, day.weather().temperature().intValueExact());
        var proposal = day.proposals().getFirst();
        assertEquals(LocalTime.of(10, 0), proposal.attraction().openingHours().getFirst().from());
        assertEquals(7500L, proposal.attraction().priceGroszy().longValue());
        assertEquals(9, proposal.fromStart().minutes().intValue());
        assertEquals(1440L, proposal.fromStart().priceGroszy().longValue());
        assertEquals(1200L, proposal.toStart().priceGroszy().longValue());

        doReturn(response(HttpStatus.UNPROCESSABLE_ENTITY,
                "{\"error\":{\"code\":\"START_NOT_RESOLVED\"}}")).when(network).exchange(any());
        var invalid = assertThrows(PlanProblem.class, () -> client.day(request));
        assertEquals(HttpStatus.BAD_REQUEST, invalid.status());
        assertTrue(invalid.getMessage().contains("Punkt startowy"));
    }

    @Test
    void usesDiscoveredHostAndFallsBackOn404WithConfiguredPort() {
        var discovery = new DiscoveryClient(builder, new DiscoveryProperties(true, "http://172.20.147.20:8080"),
                new PlannerProperties(false, "http://localhost:5173"));
        var routed = new IntegrationHttp(builder, discovery);
        var client = new AttractionsClient(routed, properties, new AttractionMapper(), "");

        when(network.exchange(any())).thenAnswer(call -> {
            var request = call.getArgument(0, ClientRequest.class);
            if (request.url().getPath().equals("/services/attractions")) {
                return response(HttpStatus.OK, "{\"name\":\"attractions\",\"ip\":\"172.20.147.42\"}");
            }
            assertEquals("172.20.147.42", request.url().getHost());
            assertEquals(8000, request.url().getPort());
            return response(HttpStatus.OK, "[]");
        });
        assertTrue(client.categories().isEmpty());

        var fresh = new DiscoveryClient(builder, new DiscoveryProperties(true, "http://172.20.147.20:8080"),
                new PlannerProperties(false, "http://localhost:5173"));
        var fallbackClient = new AttractionsClient(new IntegrationHttp(builder, fresh), properties, new AttractionMapper(), "");
        doAnswer(call -> {
            var request = call.getArgument(0, ClientRequest.class);
            if (request.url().getPath().equals("/services/attractions")) return response(HttpStatus.NOT_FOUND, "{}");
            assertEquals("172.20.147.24", request.url().getHost());
            assertEquals(8000, request.url().getPort());
            return response(HttpStatus.OK, "[]");
        }).when(network).exchange(any());
        assertTrue(fallbackClient.categories().isEmpty());
    }

    @Test
    void readsTransportBatchAndMapsLocationIdsAndGroupCost() {
        when(network.exchange(any())).thenAnswer(call -> {
            var request = call.getArgument(0, ClientRequest.class);
            assertEquals(HttpMethod.POST, request.method());
            assertEquals("http://172.20.147.17:8083/api/v1/routes", request.url().toString());
            return response(HttpStatus.OK, """
                    {"mode":"PUBLIC_TRANSPORT","results":[
                      {"fromLocationId":"start","toLocationId":"ATR_001","status":"OK","durationMin":9,"costGrosze":480},
                      {"fromLocationId":"start","toLocationId":"rest-04","status":"NO_CONNECTION"}
                    ]}
                    """);
        });
        var service = new Service("http://172.20.147.17:8083", Duration.ofSeconds(2));
        var transport = new TransportClient(http, new IntegrationProperties(configured, service, configured), "attr-01=ATR_001");
        var start = new Place(52.22, 21.01);
        var destinations = new LinkedHashMap<String, Place>();
        destinations.put("attr-01", new Place(52.23, 21.02));
        destinations.put("rest-04", new Place(52.24, 21.03));

        var trips = transport.fromStart(start, destinations, TransportMode.PUBLIC_TRANSPORT, 3);

        assertEquals(1, trips.size());
        assertEquals(9, trips.get("attr-01").minutes().intValue());
        assertEquals(1440L, trips.get("attr-01").priceGroszy().longValue());
        assertNull(trips.get("attr-01").distanceKm());
        assertFalse(trips.containsKey("rest-04"));
    }

    @Test
    void timesOutAndDoesNotContactServicesWithoutConfigurationOrContract() {
        var beans = new StaticListableBeanFactory();
        var day = LocalDate.of(2026, 11, 20);
        var gastronomy = new GastronomyClient(http, properties, beans.getBeanProvider(GastronomyClient.Contract.class));
        assertEquals(Reason.NOT_CONFIGURED, assertThrows(IntegrationFailure.class, () -> gastronomy.venues("Warszawa", day)).reason());
        verifyNoInteractions(network);

        when(network.exchange(any())).thenReturn(Mono.never());
        var shortTimeout = new Service("https://example.test", Duration.ofMillis(25));
        var client = new AttractionsClient(http, new IntegrationProperties(shortTimeout, configured, configured),
                new AttractionMapper(), "");
        assertEquals(Reason.TIMEOUT, assertThrows(IntegrationFailure.class, client::list).reason());
    }

    private static Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(body).build());
    }
}
