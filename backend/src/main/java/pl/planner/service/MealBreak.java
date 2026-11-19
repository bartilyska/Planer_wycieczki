package pl.planner.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanResponse.Type;
import pl.planner.exception.IntegrationFailure;
import pl.planner.integration.PlannerSources.Gastronomy;
import pl.planner.integration.PlannerSources.MealProposal;
import pl.planner.service.RouteLookup.Stop;

final class MealBreak {
    private final Gastronomy gastronomy;
    private final PlanRequest request;
    private final Set<String> warnings;
    private boolean scheduled;
    private boolean failed;
    private boolean queried;
    private boolean hadProposals;
    private boolean cachedTransport;

    MealBreak(Gastronomy gastronomy, PlanRequest request, Set<String> warnings) {
        this.gastronomy = gastronomy;
        this.request = request;
        this.warnings = warnings;
    }

    void afterAttraction(Schedule schedule, RouteLookup routes, boolean last) {
        if (scheduled || failed || schedule.point().id() == null
                || (!last && schedule.time().toLocalTime().isBefore(LocalTime.NOON))) return;

        var proposals = fetch(schedule.point().id());
        hadProposals |= !proposals.isEmpty();
        var feasible = proposals.stream().filter(meal -> !schedule.time()
                .plusMinutes(meal.inbound().minutes()).plusMinutes(meal.visitMinutes()).isAfter(schedule.limit())).toList();
        boolean needsOnwardTrip = !last || request.returnToStart();
        if (needsOnwardTrip) routes.loadMealStops(feasible);

        for (var meal : feasible) {
            var destination = Stop.of(meal);
            if (needsOnwardTrip) {
                var start = Stop.start(request.startLocation());
                var onward = routes.between(destination, start);
                if (routes.knownNoConnection(destination, start, onward)) continue;
                if (last && onward != null && onward.minutes() != null
                        && schedule.time().plusMinutes(meal.inbound().minutes()).plusMinutes(meal.visitMinutes())
                        .plusMinutes(onward.minutes()).isAfter(schedule.limit())) continue;
            }
            schedule.travel("Dojazd: " + meal.placeName(), destination, meal.inbound());
            schedule.add(meal.mealId(), Type.GASTRONOMY, meal.mealName() + " · " + meal.placeName(),
                    meal.visitMinutes(), meal.priceGroszy(), null, meal.coordinates(), null);
            cachedTransport = meal.transportFromCache();
            scheduled = true;
            return;
        }
    }

    void finish() {
        if (cachedTransport) warnings.add("Gastronomia użyła ostatnich znanych danych o dojeździe do lokalu.");
        if (scheduled || failed) return;
        if (!queried) {
            warnings.add("Gastronomia — brak identyfikatora lokalizacji atrakcji potrzebnego do wyszukania posiłku.");
        } else if (hadProposals) {
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

}
