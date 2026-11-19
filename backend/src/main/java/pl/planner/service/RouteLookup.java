package pl.planner.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import pl.planner.integration.PlannerSources.LocationPair;
import pl.planner.integration.PlannerSources.MealProposal;
import pl.planner.integration.PlannerSources.Transport;

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

        static Stop of(MealProposal meal) {
            return new Stop(meal.locationId(), meal.coordinates());
        }

        boolean same(Stop other) {
            if (id != null && other.id != null) return id.equals(other.id);
            return coordinates != null && coordinates.equals(other.coordinates);
        }
    }

    private final Map<String, Place> locations = new LinkedHashMap<>();
    private final Map<LocationPair, Journey> direct = new HashMap<>();
    private final Map<LocationPair, Journey> attractionQuotes = new HashMap<>();
    private final Set<LocationPair> queried = new HashSet<>();
    private final Transport source;
    private final PlanRequest request;
    private final Set<String> warnings;
    private boolean serviceFailed;

    RouteLookup(Transport source, PlanRequest request, DayData day, Set<String> warnings) {
        this.source = source;
        this.request = request;
        this.warnings = warnings;
        locations.put(Transport.START, request.startLocation());

        for (var proposal : day.proposals()) {
            var stop = Stop.of(proposal.attraction());
            if (stop.id() == null || Transport.START.equals(stop.id())) continue;
            locations.putIfAbsent(stop.id(), stop.coordinates());
            if (proposal.fromStart() != null) {
                attractionQuotes.put(new LocationPair(Transport.START, stop.id()), proposal.fromStart());
            }
            if (proposal.toStart() != null) {
                attractionQuotes.put(new LocationPair(stop.id(), Transport.START), proposal.toStart());
            }
        }
        var ids = new ArrayList<>(locations.keySet());
        var pairs = new ArrayList<LocationPair>();
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                pairs.add(pair(ids.get(i), ids.get(j)));
            }
        }
        load(pairs);
        if (serviceFailed && attractionQuotes.isEmpty() && day.travelWarning() != null) {
            warnings.add(day.travelWarning());
        }
    }

    void loadMealStops(List<MealProposal> proposals) {
        if (serviceFailed) return;
        var previousStops = List.copyOf(locations.keySet());
        var pairs = new ArrayList<LocationPair>();
        for (var proposal : proposals) {
            var stop = Stop.of(proposal);
            if (stop.id() == null || locations.containsKey(stop.id())) continue;
            locations.put(stop.id(), stop.coordinates());
            for (var id : previousStops) {
                pairs.add(pair(id, stop.id()));
            }
        }
        load(pairs);
    }

    private void load(List<LocationPair> pairs) {
        if (pairs.isEmpty()) return;
        try {
            direct.putAll(source.routes(locations, pairs, request.transportMode(), request.groupSize()));
            queried.addAll(pairs);
        } catch (IntegrationFailure failure) {
            serviceFailed = true;
            warnings.add(failure.getMessage());
            if (attractionQuotes.keySet().stream().anyMatch(quote -> !queried.contains(pair(quote.from(), quote.to())))) {
                warnings.add("Dostępne czasy i koszty przejazdów pochodzą z propozycji Atrakcji.");
            }
        }
    }

    Journey between(Stop from, Stop to) {
        if (from.same(to) || from.id() == null || to.id() == null) return null;
        var route = leg(from.id(), to.id());
        if (route != null || Transport.START.equals(from.id()) || Transport.START.equals(to.id())) return route;
        var first = leg(from.id(), Transport.START);
        var second = leg(Transport.START, to.id());
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
        } else if (knownNoConnection(from, to, journey)) {
            warnings.add("Transport — brak połączenia dla części punktów w wybranym trybie.");
        }
    }

    boolean knownNoConnection(Stop from, Stop to, Journey journey) {
        if (journey != null || from.same(to) || from.id() == null || to.id() == null
                || !queried.contains(pair(from.id(), to.id()))) return false;
        if (Transport.START.equals(from.id()) || Transport.START.equals(to.id())) return true;
        return missingConfirmedStartLeg(from.id()) || missingConfirmedStartLeg(to.id());
    }

    private Journey leg(String from, String to) {
        var key = pair(from, to);
        var journey = direct.get(key);
        if (journey == null && serviceFailed && !queried.contains(key)) {
            return attractionQuotes.get(new LocationPair(from, to));
        }
        return journey;
    }

    private boolean missingConfirmedStartLeg(String id) {
        var key = pair(Transport.START, id);
        return queried.contains(key) && !direct.containsKey(key);
    }

    private static LocationPair pair(String a, String b) {
        if (Transport.START.equals(a) || (!Transport.START.equals(b) && a.compareTo(b) <= 0)) {
            return new LocationPair(a, b);
        }
        return new LocationPair(b, a);
    }
}
