package pl.planner.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record Place(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {

    public boolean usable() {
        return lat != null && lng != null && Math.abs(lat) <= 90 && Math.abs(lng) <= 180;
    }

    public double distanceTo(Place other) {
        double latDelta = Math.toRadians(other.lat - lat);
        double lngDelta = Math.toRadians(other.lng - lng);
        double a = Math.pow(Math.sin(latDelta / 2), 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.pow(Math.sin(lngDelta / 2), 2);
        return 12_742 * Math.asin(Math.sqrt(Math.clamp(a, 0, 1)));
    }
}
