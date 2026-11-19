package pl.planner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record Attraction(String id, String name, String location, String description, String category,
                         Boolean indoor, Long priceGroszy, int durationMinutes, Place coordinates,
                         List<OpeningWindow> openingHours, Map<DayOfWeek, OpeningWindow> weeklyOpeningHours,
                         String transportLocationId) {
    public record OpeningWindow(@JsonFormat(pattern = "HH:mm") LocalTime from,
                                @JsonFormat(pattern = "HH:mm") LocalTime to) {}
}
