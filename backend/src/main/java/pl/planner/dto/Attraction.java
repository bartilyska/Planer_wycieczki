package pl.planner.dto;

import java.time.LocalTime;
import java.util.List;

public record Attraction(String id, String name, String location, String description, String category,
                         Boolean indoor, Long priceGroszy, int durationMinutes, Place coordinates,
                         List<OpeningWindow> openingHours) {
    public record OpeningWindow(LocalTime from, LocalTime to) {}
}
