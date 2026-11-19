package pl.planner.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import pl.planner.config.PlannerProperties;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanResponse;
import pl.planner.dto.PlanResponse.Type;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.DayProposal;
import pl.planner.integration.PlannerSources.Gastronomy;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.Transport;
import pl.planner.service.RouteLookup.Stop;

@Service
public class PlannerService {
    private final AttractionCatalog catalog;
    private final Transport transport;
    private final Gastronomy gastronomy;
    private final PlannerProperties properties;
    private final PlanOptimizer optimizer;
    private final Clock clock;

    public PlannerService(AttractionCatalog catalog, Transport transport, Gastronomy gastronomy,
                          PlannerProperties properties, PlanOptimizer optimizer, Clock clock) {
        this.catalog = catalog;
        this.transport = transport;
        this.gastronomy = gastronomy;
        this.properties = properties;
        this.optimizer = optimizer;
        this.clock = clock;
    }

    public PlanResponse plan(PlanRequest request, boolean optimize) {
        validate(request);
        var day = catalog.day(request);
        var attractions = day.proposals().stream().map(DayProposal::attraction).toList();
        String city = request.city().strip();
        if (attractions.stream().anyMatch(attraction -> attraction.location() != null
                && !attraction.location().isBlank()
                && !AttractionCatalog.matches(attraction.location(), city))) {
            throw PlanProblem.invalid("Wybierz atrakcje z miasta podanego w planie.");
        }

        Set<String> warnings = new LinkedHashSet<>();
        Weather weather = day.weather();
        if (!weather.available()) warnings.add("Pogoda — Atrakcje nie udostępniły prognozy dla wybranego dnia.");
        var meal = request.includeGastronomy() ? new MealBreak(gastronomy, request, warnings) : null;
        var schedule = new Schedule(request);
        var routes = new RouteLookup(transport, request, day, warnings);
        try {
            assemble(request, optimize, attractions, weather, meal, schedule, routes);
            if (meal != null) meal.finish();
        } catch (PlanProblem failure) {
            if (schedule.items().stream().noneMatch(item -> item.type() == Type.GASTRONOMY)) throw failure;
            schedule = new Schedule(request);
            assemble(request, optimize, attractions, weather, null, schedule, routes);
            warnings.add("Przerwa na posiłek nie mieści się obok wybranych atrakcji.");
        }
        return response(request, city, weather, schedule, warnings);
    }

    private void assemble(PlanRequest request, boolean optimize, List<Attraction> attractions, Weather weather,
                          MealBreak meal, Schedule schedule, RouteLookup routes) {
        var pending = new ArrayList<>(attractions);
        while (!pending.isEmpty()) {
            var attraction = optimize ? optimizer.next(pending, schedule, routes, weather) : pending.getFirst();
            pending.remove(attraction);
            var destination = Stop.of(attraction);
            var journey = routes.between(schedule.point(), destination);
            var arrival = afterTravel(schedule, journey);
            var opening = Schedule.firstOpening(arrival, attraction.durationMinutes(), attraction.openingHours());
            if (opening == null || opening.plusMinutes(attraction.durationMinutes()).isAfter(schedule.limit())) {
                throw PlanProblem.invalid("Atrakcja „" + attraction.name()
                        + "” nie mieści się w harmonogramie. Zmień kolejność, godziny lub wybór atrakcji.");
            }
            routes.reportUnavailable(schedule.point(), destination, journey);
            schedule.travel("Dojazd: " + attraction.name(), destination, journey);
            schedule.waitUntil(opening);
            schedule.add(attraction.id(), Type.ATTRACTION, attraction.name(), attraction.durationMinutes(),
                    attraction.priceGroszy(), attraction.location(), attraction.coordinates(), null);
            if (meal != null) meal.afterAttraction(schedule, routes, pending.isEmpty());
        }

        if (request.returnToStart()) {
            var destination = Stop.start(request.startLocation());
            var journey = routes.between(schedule.point(), destination);
            if (afterTravel(schedule, journey).isAfter(schedule.limit())) {
                throw PlanProblem.invalid("Powrót do punktu startowego wykracza poza czas planu.");
            }
            routes.reportUnavailable(schedule.point(), destination, journey);
            schedule.travel("Powrót do punktu startowego", destination, journey);
        }
    }

    private static LocalDateTime afterTravel(Schedule schedule, Journey journey) {
        return journey == null || journey.minutes() == null ? schedule.time() : schedule.time().plusMinutes(journey.minutes());
    }

    private PlanResponse response(PlanRequest request, String city, Weather weather, Schedule schedule, Set<String> warnings) {
        var items = schedule.items();
        boolean costComplete = items.stream().allMatch(item -> item.priceGroszy() != null);
        boolean durationComplete = items.stream().allMatch(item -> item.durationMinutes() != null);
        long cost = items.stream().map(PlanResponse.Item::priceGroszy).filter(Objects::nonNull)
                .mapToLong(Long::longValue).reduce(0, Math::addExact);
        if (!costComplete) warnings.add("Suma obejmuje wyłącznie znane ceny.");
        if (!durationComplete) warnings.add("Godziny są orientacyjne. Czas planu pomija przejazdy bez danych.");
        return new PlanResponse(city, request.date(), weather, cost, schedule.minutes(), costComplete, durationComplete,
                properties.demo(), request.startLocation(), items, List.copyOf(warnings));
    }

    private void validate(PlanRequest request) {
        if (request.date().isBefore(LocalDate.now(clock))) throw PlanProblem.invalid("Wybierz dzisiaj lub datę w przyszłości.");
        if (request.startTime().getSecond() != 0 || request.startTime().getNano() != 0
                || (request.endTime() != null && (request.endTime().getSecond() != 0 || request.endTime().getNano() != 0))) {
            throw PlanProblem.invalid("Podaj godziny z dokładnością do minuty.");
        }
        if (request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw PlanProblem.invalid("Godzina zakończenia musi być późniejsza niż godzina rozpoczęcia.");
        }
        if (!request.startLocation().usable()) throw PlanProblem.invalid("Sprawdź współrzędne punktu startowego.");
        if (new HashSet<>(request.attractionIds()).size() != request.attractionIds().size()) {
            throw PlanProblem.invalid("Każdą atrakcję można dodać do planu tylko raz.");
        }
    }

}
