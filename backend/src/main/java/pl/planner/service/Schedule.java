package pl.planner.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pl.planner.dto.Attraction.OpeningWindow;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanResponse.Item;
import pl.planner.dto.PlanResponse.Type;
import pl.planner.integration.PlannerSources.Journey;

final class Schedule {
    private final List<Item> items = new ArrayList<>();
    private final LocalDateTime start;
    private final LocalDateTime limit;
    private LocalDateTime time;
    private Place point;

    Schedule(PlanRequest request) {
        start = request.date().atTime(request.startTime());
        limit = request.date().atTime(request.endTime() == null ? LocalTime.of(23, 59) : request.endTime());
        time = start;
        point = request.startLocation();
    }

    LocalDateTime time() { return time; }
    LocalDateTime limit() { return limit; }
    Place point() { return point; }
    List<Item> items() { return List.copyOf(items); }
    int minutes() { return Math.toIntExact(ChronoUnit.MINUTES.between(start, time)); }

    void waitUntil(LocalDateTime until) {
        if (until.isAfter(time)) {
            add("wait-" + items.size(), Type.WAIT, "Przerwa w harmonogramie",
                    Math.toIntExact(ChronoUnit.MINUTES.between(time, until)), 0L, null, point, null);
        }
    }

    void travel(String name, Place destination, Journey journey) {
        if (!Objects.equals(point, destination) || destination == null) {
            add("transport-" + items.size(), Type.TRANSPORT, name, journey == null ? null : journey.minutes(),
                    journey == null ? null : journey.priceGroszy(), null, destination, journey);
        }
        point = destination;
    }

    void add(String id, Type type, String name, Integer minutes, Long price, String location, Place coordinates, Journey journey) {
        LocalDateTime end = minutes == null ? null : time.plusMinutes(minutes);
        items.add(new Item(id, type, name, time.toLocalTime(), end == null ? null : end.toLocalTime(),
                minutes, price, journey == null ? null : journey.distanceKm(),
                journey == null ? null : journey.minutes(), location, coordinates,
                journey == null ? null : journey.route(), journey == null ? null : journey.mode()));
        if (end != null) time = end;
    }

    static LocalDateTime firstOpening(LocalDateTime arrival, int duration, List<OpeningWindow> hours) {
        if (hours == null || hours.isEmpty()) return arrival;
        return hours.stream().map(window -> {
            var opens = arrival.toLocalDate().atTime(window.from()).truncatedTo(ChronoUnit.MINUTES);
            if (!opens.toLocalTime().equals(window.from())) opens = opens.plusMinutes(1);
            var candidate = arrival.isAfter(opens) ? arrival : opens;
            return candidate.plusMinutes(duration).isAfter(arrival.toLocalDate().atTime(window.to())) ? null : candidate;
        }).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
    }
}
