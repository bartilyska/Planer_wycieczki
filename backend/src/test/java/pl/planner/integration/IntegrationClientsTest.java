package pl.planner.integration;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pl.planner.config.IntegrationProperties;
import pl.planner.config.IntegrationProperties.Service;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest.TransportMode;
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
    private final IntegrationHttp http = new IntegrationHttp(WebClient.builder().exchangeFunction(network));
    private final Service configured = new Service("https://example.test", Duration.ofSeconds(5));
    private final IntegrationProperties properties = new IntegrationProperties(configured, configured,
            new Service("", Duration.ofSeconds(5)));

    @Test
    void mapsAttractionsAndDistinguishesDetail404FromListFailure() {
        when(network.exchange(any())).thenAnswer(call -> {
            var request = call.getArgument(0, org.springframework.web.reactive.function.client.ClientRequest.class);
            return switch (request.url().getPath()) {
                case "/attractions/proposal" -> response(HttpStatus.OK, """
                        [{"id":"park","name":"Park","location":"Warszawa","isIndoor":false,
                          "priceGroszy":0,"durationMinutes":90,"coordinates":{"lat":52.22,"lng":21.01}}]
                        """);
                default -> response(HttpStatus.NOT_FOUND, "{}");
            };
        });
        var client = new AttractionsClient(http, properties, new AttractionMapper());

        var item = assertDoesNotThrow(client::list).getFirst();
        assertEquals("park", item.id());
        assertFalse(item.indoor());
        assertEquals(new Place(52.22, 21.01), item.coordinates());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(PlanProblem.class, () -> client.detail("missing")).status());

        doReturn(response(HttpStatus.NOT_FOUND, "{}")).when(network).exchange(any());
        assertEquals(Reason.INVALID_RESPONSE, assertThrows(IntegrationFailure.class, client::list).reason());
    }

    @Test
    void timesOutAndDoesNotContactServicesWithoutConfigurationOrContract() {
        var beans = new StaticListableBeanFactory();
        var day = LocalDate.of(2026, 11, 20);
        var attractions = new AttractionsClient(http, properties, new AttractionMapper());
        var transport = new TransportClient(http, properties, beans.getBeanProvider(TransportClient.Contract.class));
        var gastronomy = new GastronomyClient(http, properties, beans.getBeanProvider(GastronomyClient.Contract.class));
        var start = new Place(52.22, 21.01);
        var end = new Place(52.23, 21.02);

        assertEquals(Reason.CONTRACT_PENDING, assertThrows(IntegrationFailure.class, () -> attractions.day("Warszawa", day)).reason());
        assertEquals(Reason.CONTRACT_PENDING, assertThrows(IntegrationFailure.class, () -> transport.leg(start, end, TransportMode.CAR)).reason());
        assertEquals(Reason.NOT_CONFIGURED, assertThrows(IntegrationFailure.class, () -> gastronomy.venues("Warszawa", day)).reason());
        verifyNoInteractions(network);

        when(network.exchange(any())).thenReturn(Mono.never());
        var shortTimeout = new Service("https://example.test", Duration.ofMillis(25));
        var client = new AttractionsClient(http, new IntegrationProperties(shortTimeout, configured, configured),
                new AttractionMapper());
        assertEquals(Reason.TIMEOUT, assertThrows(IntegrationFailure.class, client::list).reason());
    }

    private static Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body(body).build());
    }
}
