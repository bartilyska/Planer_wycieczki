package pl.planner.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Weather;

/** Normalized data ports used by the planner. Each HTTP adapter owns its team's wire contract. */
public final class PlannerSources {
    private PlannerSources() {}

    public interface Attractions {
        List<Attraction> list();
        Attraction detail(String id);
        List<Category> categories();
        DayData day(PlanRequest request);
    }

    public interface Transport {
        String START = "start";

        Map<LocationPair, Journey> routes(Map<String, Place> locations, List<LocationPair> pairs, TransportMode mode, int people);
    }

    public record LocationPair(String from, String to) {}

    public interface Gastronomy {
        List<MealProposal> proposals(String fromLocationId, PlanRequest request);
    }

    public record Journey(Integer minutes, BigDecimal distanceKm, Long priceGroszy,
                          String mode, List<Place> route, boolean viaStart) {
        public Journey(Integer minutes, BigDecimal distanceKm, Long priceGroszy, String mode, List<Place> route) {
            this(minutes, distanceKm, priceGroszy, mode, route, false);
        }
    }

    public record Category(String code, String name) {}
    public record DayData(Weather weather, List<DayProposal> proposals, String travelWarning) {}
    public record DayProposal(Attraction attraction, Journey fromStart, Journey toStart) {}

    public record MealProposal(String locationId, String mealId, String placeName, String mealName,
                               int visitMinutes, long priceGroszy, Journey inbound, boolean transportFromCache,
                               Place coordinates) {}
}
