package pl.planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlanRequest(
        @NotBlank @Size(max = 100) String city,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        LocalTime endTime,
        @NotNull @Valid Place startLocation,
        @NotNull @Size(min = 1, max = MAX_ATTRACTIONS) List<
                @NotBlank @Size(max = 150) @Pattern(regexp = "[^/\\\\%?#\\p{Cntrl}]+") String> attractionIds,
        boolean includeGastronomy,
        @NotNull TransportMode transportMode,
        boolean returnToStart) {
    public static final int MAX_ATTRACTIONS = 12;

    public enum TransportMode { WALK, BIKE, PUBLIC_TRANSPORT, CAR }
}
