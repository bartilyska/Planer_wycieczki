package pl.planner.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanResponse.Condition;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.integration.PlannerSources.Journey;

@Component
final class PlanOptimizer {
    Attraction next(List<Attraction> candidates, Schedule schedule, RouteLookup routes, Weather weather) {
        return candidates.stream()
                .map(attraction -> new Candidate(attraction, score(attraction, schedule, routes, weather)))
                .min(Comparator.comparingDouble(Candidate::score)).orElseThrow().attraction();
    }

    private double score(Attraction attraction, Schedule schedule, RouteLookup routes, Weather weather) {
        var point = schedule.point();
        var target = attraction.coordinates();
        Journey journey = Objects.equals(point, target) && point != null ? null : routes.between(point, target);
        Integer minutes = journey == null ? null : journey.minutes();
        double travelRank = minutes != null ? minutes : (point != null && target != null ? point.distanceTo(target) * 4 : 25);
        var arrival = schedule.time().plusMinutes(minutes == null ? 0 : minutes);
        var opening = Schedule.firstOpening(arrival, attraction.durationMinutes(), attraction.openingHours());
        if (opening == null || opening.plusMinutes(attraction.durationMinutes()).isAfter(schedule.limit())) {
            return Double.POSITIVE_INFINITY;
        }
        return travelRank + java.time.Duration.between(arrival, opening).toMinutes() + weatherPenalty(attraction, weather);
    }

    private int weatherPenalty(Attraction attraction, Weather weather) {
        if (!weather.available() || attraction.indoor() == null) return 0;
        var condition = weather.condition();
        if (condition == Condition.ADVERSE_OUTDOOR && !attraction.indoor()) return 30;
        if (condition == Condition.FAVORABLE_OUTDOOR && attraction.indoor()) return 15;
        return 0;
    }

    private record Candidate(Attraction attraction, double score) {}
}
