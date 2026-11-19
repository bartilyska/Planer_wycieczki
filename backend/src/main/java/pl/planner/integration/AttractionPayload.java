package pl.planner.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class AttractionPayload {
    private AttractionPayload() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogPage(List<CatalogItem> items, Integer page, Integer totalPages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogItem(String id, String name, String category, Coordinates location, Boolean indoor,
                              Integer visitDurationMin, BigDecimal pricePerPerson, String description,
                              Map<DayOfWeek, Hours> openingHours) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Category(String code, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Day(LocalDate date, Forecast weather, Source transport, List<Proposal> proposals) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Proposal(String id, String name, String category, Coordinates location, Boolean indoor,
                           Integer visitDurationMin, Hours openingHours, Ticket ticket,
                           Leg fromStart, Leg toStart) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coordinates(String id, Double lat, Double lng) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hours(LocalTime open, LocalTime close) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ticket(BigDecimal total) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(Integer timeMin, BigDecimal costTotal) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Forecast(String condition, BigDecimal tempC, String source) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(String source) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorPayload(ApiError error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(String code, String field) {}
}
