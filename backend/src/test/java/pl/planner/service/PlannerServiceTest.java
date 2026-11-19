package pl.planner.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.planner.config.PlannerProperties;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Condition;
import pl.planner.dto.PlanResponse.Item;
import pl.planner.dto.PlanResponse.Type;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.integration.PlannerSources.Attractions;
import pl.planner.integration.PlannerSources.Forecast;
import pl.planner.integration.PlannerSources.Gastronomy;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.Transport;
import pl.planner.integration.PlannerSources.Venue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlannerServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    private static final Place START = new Place(52.22, 21.01);
    private static final Place PARK = new Place(52.23, 21.01);
    private static final Place MUSEUM = new Place(52.24, 21.02);
    private final Attractions attractions = mock(Attractions.class);
    private final Forecast forecast = mock(Forecast.class);
    private final Transport transport = mock(Transport.class);
    private final Gastronomy gastronomy = mock(Gastronomy.class);
    private final PlannerService planner = new PlannerService(new AttractionCatalog(attractions), forecast,
            transport, gastronomy, new PlannerProperties(false, "http://localhost:5173"), new PlanOptimizer(),
            Clock.fixed(Instant.parse("2026-11-19T09:00:00Z"), ZoneOffset.UTC));

    @Test
    void previewsInManualOrderAndIncludesMealAndReturn() {
        setAttractions();
        when(forecast.day("Warszawa", DATE)).thenReturn(new Weather(BigDecimal.TEN, "Deszcz", Condition.ADVERSE_OUTDOOR, true));
        when(transport.leg(any(), any(), eq(TransportMode.CAR)))
                .thenReturn(new Journey(10, BigDecimal.ONE, 200L, "CAR", List.of()));
        when(gastronomy.venues("Warszawa", DATE))
                .thenReturn(List.of(new Venue("lunch", "Bistro", "Centrum", MUSEUM, "bistro", 3000L, 40, List.of())));

        var plan = planner.plan(request(true, true), false);

        assertEquals(List.of(Type.TRANSPORT, Type.ATTRACTION, Type.TRANSPORT, Type.ATTRACTION, Type.GASTRONOMY, Type.TRANSPORT),
                plan.items().stream().map(Item::type).toList());
        assertEquals("park", plan.items().get(1).id());
        assertEquals(130, plan.totalDurationMinutes());
        assertEquals(5600L, plan.totalCostGroszy());
        assertEquals(LocalTime.of(13, 10), plan.items().getLast().endTime());
        assertTrue(plan.warnings().isEmpty());
        verify(transport).leg(START, PARK, TransportMode.CAR);
        verify(transport).leg(PARK, MUSEUM, TransportMode.CAR);
        verify(transport).leg(MUSEUM, START, TransportMode.CAR);
    }

    @Test
    void optimizesForRainAndMarksMissingTimeAndCosts() {
        setAttractions();
        when(forecast.day("Warszawa", DATE)).thenReturn(new Weather(BigDecimal.TEN, "Deszcz", Condition.ADVERSE_OUTDOOR, true));
        when(transport.leg(any(), any(), any()))
                .thenThrow(new IntegrationFailure("Transport", Reason.NOT_CONFIGURED));
        when(gastronomy.venues("Warszawa", DATE))
                .thenThrow(new IntegrationFailure("Gastronomia", Reason.CONTRACT_PENDING));

        var plan = planner.plan(request(true, false), true);

        assertEquals(List.of("museum", "park"),
                plan.items().stream().filter(item -> item.type() == Type.ATTRACTION).map(Item::id).toList());
        assertNull(plan.items().getFirst().durationMinutes());
        assertFalse(plan.durationComplete());
        assertFalse(plan.costComplete());
        assertEquals(60, plan.totalDurationMinutes());
        assertEquals(2000L, plan.totalCostGroszy());
        assertEquals(4, plan.warnings().size());
    }

    private void setAttractions() {
        when(attractions.detail("park")).thenReturn(new Attraction("park", "Park", "Warszawa", null,
                "przyroda", false, 0L, 30, PARK, List.of()));
        when(attractions.detail("museum")).thenReturn(new Attraction("museum", "Muzeum", "Warszawa", null,
                "kultura", true, 2000L, 30, MUSEUM, List.of()));
    }

    private PlanRequest request(boolean meal, boolean roundTrip) {
        return new PlanRequest("Warszawa", DATE, LocalTime.of(11, 0), LocalTime.of(18, 0), START,
                List.of("park", "museum"), meal, TransportMode.CAR, roundTrip);
    }
}
