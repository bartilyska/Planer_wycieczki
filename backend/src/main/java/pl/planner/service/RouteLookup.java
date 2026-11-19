package pl.planner.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.exception.IntegrationFailure;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.Transport;

final class RouteLookup {
    private record Segment(Place from, Place to) {}

    private final Transport source;
    private final TransportMode mode;
    private final Set<String> warnings;
    private final Map<Segment, Optional<Journey>> cache = new HashMap<>();
    private boolean disabled;

    RouteLookup(Transport source, TransportMode mode, Set<String> warnings) {
        this.source = source;
        this.mode = mode;
        this.warnings = warnings;
    }

    Journey between(Place from, Place to) {
        if (from == null || to == null) {
            warnings.add("Dla części punktów brakuje współrzędnych potrzebnych do wyznaczenia dojazdu.");
            return null;
        }
        return cache.computeIfAbsent(new Segment(from, to), this::fetch).orElse(null);
    }

    private Optional<Journey> fetch(Segment segment) {
        if (disabled) return Optional.empty();
        try {
            return Optional.ofNullable(source.leg(segment.from(), segment.to(), mode));
        } catch (IntegrationFailure failure) {
            warnings.add(failure.getMessage());
            disabled = failure.reason() != IntegrationFailure.Reason.NO_DATA;
            return Optional.empty();
        }
    }
}
