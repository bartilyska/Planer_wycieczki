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
import pl.planner.integration.PlannerSources.LocationPair;
import pl.planner.integration.PlannerSources.MealProposal;
import pl.planner.integration.PlannerSources.Transport;

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
    void prefersDirectTripsAndAppliesBudgetToTheTripActuallyUsed() {
        var leg = new Journey(10, BigDecimal.ONE, 200L, "CAR", List.of());
        var proposalLeg = new Journey(50, null, 9999L, "CAR", List.of());
        setAttractions(proposalLeg, proposalLeg);
        var startToPark = new LocationPair("start", "attr-park");
        var startToMuseum = new LocationPair("start", "attr-museum");
        var museumToPark = new LocationPair("attr-museum", "attr-park");
        var attractionsBatch = List.of(startToPark, startToMuseum, museumToPark);
        when(transport.routes(anyMap(), eq(attractionsBatch), eq(TransportMode.CAR), eq(1)))
                .thenReturn(Map.of(startToPark, leg, startToMuseum, leg,
                        museumToPark, new Journey(7, null, 150L, "CAR", List.of())));
        var restaurant = new Place(52.25, 21.03);
        var startToMeal = new LocationPair("start", "rest-01");
        var parkToMeal = new LocationPair("attr-park", "rest-01");
        var museumToMeal = new LocationPair("attr-museum", "rest-01");
        var mealBatch = List.of(startToMeal, parkToMeal, museumToMeal);
        when(transport.routes(anyMap(), eq(mealBatch), eq(TransportMode.CAR), eq(1)))
                .thenReturn(Map.of(startToMeal, leg, museumToMeal, new Journey(12, null, 120L, "CAR", List.of())));
        var inbound = new Journey(19, null, 350L, "CAR", List.of(), true);
        when(gastronomy.proposals(eq("attr-museum"), any()))
                .thenReturn(List.of(new MealProposal("rest-01", "meal-01", "Bistro", "Zupa", 40, 3000L, inbound, true, restaurant)));

        var plan = planner.plan(request(true, true, 3200L), false);

        assertEquals(List.of(Type.TRANSPORT, Type.ATTRACTION, Type.TRANSPORT, Type.ATTRACTION, Type.TRANSPORT,
                        Type.GASTRONOMY, Type.TRANSPORT),
                plan.items().stream().map(Item::type).toList());
        assertEquals("park", plan.items().get(1).id());
        assertFalse(plan.items().get(2).name().contains("przez punkt startowy"));
        assertEquals(7, plan.items().get(2).durationMinutes().intValue());
        assertEquals(12, plan.items().get(4).durationMinutes().intValue());
        assertEquals("Zupa · Bistro", plan.items().get(5).name());
        assertEquals(139, plan.totalDurationMinutes());
        assertEquals(5670L, plan.totalCostGroszy());
        assertEquals(LocalTime.of(13, 19), plan.items().getLast().endTime());
        assertTrue(plan.warnings().isEmpty());
        verify(transport).routes(anyMap(), eq(attractionsBatch), eq(TransportMode.CAR), eq(1));
        verify(transport).routes(anyMap(), eq(mealBatch), eq(TransportMode.CAR), eq(1));
        verify(gastronomy).proposals(eq("attr-museum"), any());
        verifyNoMoreInteractions(transport);
    }

    @Test
    void optimizesForRainAndMarksMissingTimeAndCosts() {
        setAttractions(null, null);
        when(transport.routes(anyMap(), anyList(), any(), anyInt()))
                .thenThrow(new IntegrationFailure("Transport", Reason.TIMEOUT));
        when(gastronomy.proposals(anyString(), any()))
                .thenThrow(new IntegrationFailure("Gastronomia", Reason.TIMEOUT));

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
        when(transport.routes(anyMap(), anyList(), any(), anyInt())).thenReturn(Map.of());

        var plan = planner.plan(request(false, true), false);

        assertEquals(60, plan.totalDurationMinutes());
        assertEquals(2000L, plan.totalCostGroszy());
        assertTrue(plan.warnings().stream().anyMatch(warning -> warning.contains("brak połączenia")));
    }

    @Test
    void usesBothStartLegsWhenThereIsNoDirectConnection() {
        setAttractions(null, null);
        var leg = new Journey(10, null, 200L, "CAR", List.of());
        when(transport.routes(anyMap(), anyList(), any(), anyInt())).thenReturn(Map.of(
                new LocationPair("start", "attr-park"), leg, new LocationPair("start", "attr-museum"), leg));

        var plan = planner.plan(request(false, false), false);

        var transfer = plan.items().get(2);
        assertTrue(transfer.name().contains("przez punkt startowy"));
        assertEquals(20, transfer.durationMinutes().intValue());
        assertEquals(400L, transfer.priceGroszy().longValue());
        assertTrue(plan.durationComplete());
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
        return request(meal, roundTrip, null);
    }

    private PlanRequest request(boolean meal, boolean roundTrip, Long budget) {
        return new PlanRequest("Warszawa", DATE, LocalTime.of(11, 0), LocalTime.of(18, 0), START,
                List.of("park", "museum"), meal, TransportMode.CAR, roundTrip, 1, null, budget);
    }
}
