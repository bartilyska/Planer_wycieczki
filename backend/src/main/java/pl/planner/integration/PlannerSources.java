package pl.planner.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Weather;

/** Normalized data ports used by the planner. Each HTTP adapter owns its team's wire contract. */
public final class PlannerSources {
    private PlannerSources() {}

    public interface Attractions {
        List<Attraction> list();
        Attraction detail(String id);
    }

    public interface Forecast {
        Weather day(String city, LocalDate date);
    }

    public interface Transport {
        Journey leg(Place from, Place to, TransportMode mode);
    }

    public interface Gastronomy {
        List<Venue> venues(String city, LocalDate date);
    }

    public record Journey(Integer minutes, BigDecimal distanceKm, Long priceGroszy,
                          String mode, List<Place> route) {}

    public record Venue(String id, String name, String address, Place coordinates,
                        String category, Long priceGroszy, Integer durationMinutes,
                        List<Attraction.OpeningWindow> openingHours) {}
}
