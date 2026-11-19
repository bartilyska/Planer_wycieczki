package pl.planner.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        Map<String, Journey> fromStart(Place start, Map<String, Place> destinations, TransportMode mode, int people);
    }

    public interface Gastronomy {
        List<Venue> venues(String city, LocalDate date);
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

    public record Venue(String id, String name, String address, Place coordinates,
                        String category, Long priceGroszy, Integer durationMinutes,
                        List<Attraction.OpeningWindow> openingHours, String transportLocationId) {}
}
