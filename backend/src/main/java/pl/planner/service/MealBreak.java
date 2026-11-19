package pl.planner.service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanResponse.Type;
import pl.planner.exception.IntegrationFailure;
import pl.planner.integration.PlannerSources.Gastronomy;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.MealProposal;
import pl.planner.service.RouteLookup.Stop;

final class MealBreak {
    private final Gastronomy gastronomy;
    private final PlanRequest request;
    private final Set<String> warnings;
    private boolean scheduled;
    private boolean failed;
    private boolean queried;
    private boolean hadAffordableProposals;
    private boolean cachedTransport;

    MealBreak(Gastronomy gastronomy, PlanRequest request, Set<String> warnings) {
        this.gastronomy = gastronomy;
        this.request = request;
        this.warnings = warnings;
    }

    void afterAttraction(Schedule schedule, RouteLookup routes, List<Attraction> remaining, boolean optimize) {
        boolean last = remaining.isEmpty();
        if (scheduled || failed || schedule.point().id() == null
                || (!last && schedule.time().toLocalTime().isBefore(LocalTime.NOON))) return;

        var proposals = fetch(schedule.point().id());
        routes.loadMealStops(proposals);
        var candidates = proposals.stream().map(meal -> candidate(meal, schedule.point(), routes))
                .filter(candidate -> candidate != null && fitsBudget(candidate))
                .sorted(Comparator.comparingLong(Candidate::totalCost)).toList();
        hadAffordableProposals |= !candidates.isEmpty();
        var nextStops = last ? List.<Stop>of() : (optimize ? remaining : List.of(remaining.getFirst())).stream()
                .map(Stop::of).toList();
        for (var candidate : candidates) {
            var meal = candidate.meal();
            var inbound = candidate.journey();
            var destination = Stop.of(meal);
            var finish = schedule.time().plusMinutes(inbound.minutes()).plusMinutes(meal.visitMinutes());
            if (finish.isAfter(schedule.limit())) continue;
            if (!canContinue(destination, nextStops, routes)) continue;
            if (last && request.returnToStart()) {
                var start = Stop.start(request.startLocation());
                var onward = routes.between(destination, start);
                if (routes.knownNoConnection(destination, start, onward)) continue;
                if (onward != null && onward.minutes() != null && finish.plusMinutes(onward.minutes()).isAfter(schedule.limit())) {
                    continue;
                }
            }
            schedule.travel("Dojazd: " + meal.placeName(), destination, inbound);
            schedule.add(meal.mealId(), Type.GASTRONOMY, meal.mealName() + " · " + meal.placeName(),
                    meal.visitMinutes(), meal.priceGroszy(), null, meal.coordinates(), null);
            cachedTransport = candidate.usesGastronomyQuote() && meal.transportFromCache();
            scheduled = true;
            return;
        }
    }

    private Candidate candidate(MealProposal meal, Stop origin, RouteLookup routes) {
        var destination = Stop.of(meal);
        var journey = routes.between(origin, destination);
        if (routes.knownNoConnection(origin, destination, journey)) return null;
        if (journey == null) return new Candidate(meal, meal.inbound(), true);
        return new Candidate(meal, journey, false);
    }

    private boolean fitsBudget(Candidate candidate) {
        var budget = request.maxBudgetGrosze();
        return budget == null || (candidate.journey().priceGroszy() != null
                && Math.ceilDiv(candidate.totalCost(), (long) request.groupSize()) <= budget);
    }

    private boolean canContinue(Stop destination, List<Stop> nextStops, RouteLookup routes) {
        return nextStops.isEmpty() || nextStops.stream().anyMatch(next ->
                !routes.knownNoConnection(destination, next, routes.between(destination, next)));
    }

    void finish() {
        if (cachedTransport) warnings.add("Gastronomia użyła ostatnich znanych danych o dojeździe do lokalu.");
        if (scheduled || failed) return;
        if (!queried) {
            warnings.add("Gastronomia — brak identyfikatora lokalizacji atrakcji potrzebnego do wyszukania posiłku.");
        } else if (hadAffordableProposals) {
            warnings.add("Posiłek i dostępne przejazdy nie mieszczą się w harmonogramie.");
        } else {
            warnings.add("Gastronomia — brak propozycji dla wybranej diety, budżetu i dojazdu.");
        }
    }

    private List<MealProposal> fetch(String fromLocationId) {
        queried = true;
        try {
            return gastronomy.proposals(fromLocationId, request);
        } catch (IntegrationFailure failure) {
            failed = true;
            warnings.add(failure.getMessage());
            return List.of();
        }
    }

    private record Candidate(MealProposal meal, Journey journey, boolean usesGastronomyQuote) {
        long totalCost() {
            return journey.priceGroszy() == null ? Long.MAX_VALUE : Math.addExact(meal.priceGroszy(), journey.priceGroszy());
        }
    }
}
