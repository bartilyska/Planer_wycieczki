package pl.planner.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttractionPayload(String id, String name, String location, String description, String category,
                                @JsonProperty("isIndoor") Boolean indoor, Long priceGroszy,
                                Integer durationMinutes, Coordinates coordinates) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coordinates(Double lat, Double lng) {}
}
