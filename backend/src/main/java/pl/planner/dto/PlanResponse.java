package pl.planner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PlanResponse(String city, LocalDate date, Weather weather, long totalCostGroszy,
                           int totalDurationMinutes, boolean costComplete, boolean durationComplete,
                           boolean demo, Place startLocation, List<Item> items, List<String> warnings) {
    public record Weather(BigDecimal temperature, String description, Condition condition, boolean available) {
        public static Weather unavailable() {
            return new Weather(null, null, null, false);
        }
    }

    public enum Condition { FAVORABLE_OUTDOOR, ADVERSE_OUTDOOR, NEUTRAL }
    public enum Type { ATTRACTION, TRANSPORT, GASTRONOMY, WAIT }

    public record Item(String id, Type type, String name,
                       @JsonFormat(pattern = "HH:mm") LocalTime startTime,
                       @JsonFormat(pattern = "HH:mm") LocalTime endTime,
                       Integer durationMinutes, Long priceGroszy, BigDecimal distanceKm,
                       Integer travelMinutes, String location, Place coordinates,
                       List<Place> route, String transportMode) {}
}
