package pl.planner.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
import pl.planner.integration.PlannerSources.DayData;
import pl.planner.integration.PlannerSources.DayProposal;
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
    private final Transport transport = mock(Transport.class);
    private final Gastronomy gastronomy = mock(Gastronomy.class);
    private final PlannerService planner = new PlannerService(new AttractionCatalog(attractions),
            transport, gastronomy, new PlannerProperties(false, "http://localhost:5173"), new PlanOptimizer(),
            Clock.fixed(Instant.parse("2026-11-19T09:00:00Z"), ZoneOffset.UTC));

    @Test
    void previewsInManualOrderAndIncludesMealAndReturn() {
        var leg = new Journey(10, BigDecimal.ONE, 200L, "CAR", List.of());
        var proposalLeg = new Journey(50, null, 9999L, "CAR", List.of());
        setAttractions(proposalLeg, proposalLeg);
        when(transport.fromStart(eq(START), anyMap(), eq(TransportMode.CAR), eq(1)))
                .thenReturn(Map.of("attr-park", leg, "attr-museum", leg));
        when(gastronomy.venues("Warszawa", DATE))
                .thenReturn(List.of(new Venue("lunch", "Bistro", "Centrum", MUSEUM, "bistro", 3000L, 40, List.of(), "attr-museum")));

        var plan = planner.plan(request(true, true), false);

        assertEquals(List.of(Type.TRANSPORT, Type.ATTRACTION, Type.TRANSPORT, Type.ATTRACTION, Type.GASTRONOMY, Type.TRANSPORT),
                plan.items().stream().map(Item::type).toList());
        assertEquals("park", plan.items().get(1).id());
        assertTrue(plan.items().get(2).name().contains("przez punkt startowy"));
        assertEquals(140, plan.totalDurationMinutes());
        assertEquals(5800L, plan.totalCostGroszy());
        assertEquals(LocalTime.of(13, 20), plan.items().getLast().endTime());
        assertTrue(plan.warnings().isEmpty());
        verify(transport).fromStart(START, Map.of("attr-park", PARK, "attr-museum", MUSEUM), TransportMode.CAR, 1);
        verifyNoMoreInteractions(transport);
    }

    @Test
    void optimizesForRainAndMarksMissingTimeAndCosts() {
        setAttractions(null, null);
        when(transport.fromStart(any(), anyMap(), any(), anyInt()))
                .thenThrow(new IntegrationFailure("Transport", Reason.TIMEOUT));
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

    @Test
    void noConnectionStaysUnknownEvenWhenAttractionsHaveAnOlderQuote() {
        var oldQuote = new Journey(10, null, 200L, "CAR", List.of());
        setAttractions(oldQuote, oldQuote);
        when(transport.fromStart(any(), anyMap(), any(), anyInt())).thenReturn(Map.of());

        var plan = planner.plan(request(false, true), false);

        assertEquals(60, plan.totalDurationMinutes());
        assertEquals(2000L, plan.totalCostGroszy());
        assertTrue(plan.warnings().stream().anyMatch(warning -> warning.contains("brak połączenia")));
    }

    private void setAttractions(Journey fromStart, Journey toStart) {
        var park = new Attraction("park", "Park", "Warszawa", null,
                "przyroda", false, 0L, 30, PARK, List.of(), Map.of(), "attr-park");
        var museum = new Attraction("museum", "Muzeum", "Warszawa", null,
                "kultura", true, 2000L, 30, MUSEUM, List.of(), Map.of(), "attr-museum");
        when(attractions.day(any())).thenReturn(new DayData(
                new Weather(BigDecimal.TEN, "Deszcz", Condition.ADVERSE_OUTDOOR, true),
                List.of(new DayProposal(park, fromStart, null), new DayProposal(museum, null, toStart)), null));
    }

    private PlanRequest request(boolean meal, boolean roundTrip) {
        return new PlanRequest("Warszawa", DATE, LocalTime.of(11, 0), LocalTime.of(18, 0), START,
                List.of("park", "museum"), meal, TransportMode.CAR, roundTrip, 1);
    }
}
