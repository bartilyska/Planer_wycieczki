package pl.planner.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.exception.IntegrationFailure;
import pl.planner.integration.PlannerSources.DayData;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.Transport;
import pl.planner.integration.PlannerSources.Venue;

final class RouteLookup {
    record Stop(String id, Place coordinates) {
        Stop {
            if (id != null && id.isBlank()) id = null;
        }

        static Stop start(Place coordinates) {
            return new Stop(Transport.START, coordinates);
        }

        static Stop of(Attraction attraction) {
            return new Stop(attraction.transportLocationId(), attraction.coordinates());
        }

        static Stop of(Venue venue) {
            return new Stop(venue.transportLocationId(), venue.coordinates());
        }

        boolean same(Stop other) {
            if (id != null && other.id != null) return id.equals(other.id);
            return coordinates != null && coordinates.equals(other.coordinates);
        }
    }

    private record StartLegs(Journey fromStart, Journey toStart) {}

    private final Map<String, StartLegs> legs = new HashMap<>();
    private final Set<String> warnings;
    private boolean serviceFailed;

    RouteLookup(Transport source, PlanRequest request, DayData day, List<Venue> venues, Set<String> warnings) {
        this.warnings = warnings;
        var destinations = new LinkedHashMap<String, Place>();
        day.proposals().forEach(proposal -> addDestination(destinations, Stop.of(proposal.attraction())));
        if (venues != null) venues.forEach(venue -> addDestination(destinations, Stop.of(venue)));
        if (destinations.isEmpty()) return;

        try {
            source.fromStart(request.startLocation(), destinations, request.transportMode(), request.groupSize())
                    .forEach((id, journey) -> legs.put(id, new StartLegs(journey, journey)));
        } catch (IntegrationFailure failure) {
            serviceFailed = true;
            warnings.add(failure.getMessage());
            for (var proposal : day.proposals()) {
                var id = proposal.attraction().transportLocationId();
                if (id != null && (proposal.fromStart() != null || proposal.toStart() != null)) {
                    legs.put(id, new StartLegs(proposal.fromStart(), proposal.toStart()));
                }
            }
            if (!legs.isEmpty()) warnings.add("Dostępne czasy i koszty przejazdów pochodzą z propozycji Atrakcji.");
            else if (day.travelWarning() != null) warnings.add(day.travelWarning());
        }
    }

    Journey between(Stop from, Stop to) {
        if (from.same(to) || from.id() == null || to.id() == null) return null;
        if (Transport.START.equals(from.id())) return fromStart(to.id());
        if (Transport.START.equals(to.id())) return toStart(from.id());
        var first = toStart(from.id());
        var second = fromStart(to.id());
        if (first == null || second == null || first.minutes() == null || second.minutes() == null) return null;
        var cost = first.priceGroszy() == null || second.priceGroszy() == null
                ? null : Math.addExact(first.priceGroszy(), second.priceGroszy());
        var distance = first.distanceKm() == null || second.distanceKm() == null
                ? null : first.distanceKm().add(second.distanceKm());
        return new Journey(Math.addExact(first.minutes(), second.minutes()), distance, cost, first.mode(), List.of(), true);
    }

    void reportUnavailable(Stop from, Stop to, Journey journey) {
        if (journey != null || from.same(to)) return;
        if (from.id() == null || to.id() == null) {
            warnings.add("Transport — dla części punktów brakuje identyfikatora lokalizacji.");
        } else if (!serviceFailed) {
            warnings.add("Transport — brak połączenia dla części punktów w wybranym trybie.");
        }
    }

    boolean knownNoConnection(Stop from, Stop to, Journey journey) {
        return !serviceFailed && journey == null && !from.same(to) && from.id() != null && to.id() != null;
    }

    private Journey fromStart(String id) {
        var entry = legs.get(id);
        return entry == null ? null : entry.fromStart();
    }

    private Journey toStart(String id) {
        var entry = legs.get(id);
        return entry == null ? null : entry.toStart();
    }

    private static void addDestination(Map<String, Place> destinations, Stop stop) {
        if (stop.id() != null && !Transport.START.equals(stop.id())) {
            destinations.putIfAbsent(stop.id(), stop.coordinates());
        }
    }
}
