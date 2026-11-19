package pl.planner.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import pl.planner.config.PlannerProperties;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanResponse;
import pl.planner.dto.PlanResponse.Type;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.DayProposal;
import pl.planner.integration.PlannerSources.Gastronomy;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.Transport;
import pl.planner.integration.PlannerSources.Venue;

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
        if (day.travelWarning() != null) warnings.add(day.travelWarning());
        List<Venue> venues = request.includeGastronomy()
                ? optional(() -> gastronomy.venues(city, request.date()), null, warnings) : null;

        var schedule = new Schedule(request);
        var routes = new RouteLookup(transport, request.transportMode(), warnings);
        for (var proposal : day.proposals()) {
            var point = proposal.attraction().coordinates();
            routes.remember(request.startLocation(), point, proposal.fromStart());
            routes.remember(point, request.startLocation(), proposal.toStart());
        }
        try {
            assemble(request, optimize, attractions, weather, venues, schedule, routes, warnings);
        } catch (PlanProblem failure) {
            if (schedule.items().stream().noneMatch(item -> item.type() == Type.GASTRONOMY)) throw failure;
            schedule = new Schedule(request);
            assemble(request, optimize, attractions, weather, null, schedule, routes, warnings);
            warnings.add("Przerwa na posiłek nie mieści się obok wybranych atrakcji.");
        }
        return response(request, city, weather, schedule, warnings);
    }

    private void assemble(PlanRequest request, boolean optimize, List<Attraction> attractions, Weather weather,
                          List<Venue> venues, Schedule schedule, RouteLookup routes, Set<String> warnings) {
        var pending = new ArrayList<>(attractions);
        while (!pending.isEmpty()) {
            var attraction = optimize ? optimizer.next(pending, schedule, routes, weather) : pending.getFirst();
            pending.remove(attraction);
            var journey = route(routes, schedule, attraction.coordinates());
            var arrival = afterTravel(schedule, journey);
            var opening = Schedule.firstOpening(arrival, attraction.durationMinutes(), attraction.openingHours());
            if (opening == null || opening.plusMinutes(attraction.durationMinutes()).isAfter(schedule.limit())) {
                throw PlanProblem.invalid("Atrakcja „" + attraction.name()
                        + "” nie mieści się w harmonogramie. Zmień kolejność, godziny lub wybór atrakcji.");
            }
            schedule.travel("Dojazd: " + attraction.name(), attraction.coordinates(), journey);
            schedule.waitUntil(opening);
            schedule.add(attraction.id(), Type.ATTRACTION, attraction.name(), attraction.durationMinutes(),
                    attraction.priceGroszy(), attraction.location(), attraction.coordinates(), null);
            if (venues != null && (!schedule.time().toLocalTime().isBefore(LocalTime.NOON) || pending.isEmpty())
                    && addMeal(schedule, routes, venues, pending.isEmpty(), request)) {
                venues = null;
            }
        }
        if (venues != null) {
            warnings.add(venues.isEmpty() ? "Gastronomia — brak propozycji dla wybranego miasta i dnia."
                    : "Żaden dostępny posiłek nie mieści się w harmonogramie.");
        }

        if (request.returnToStart()) {
            var journey = route(routes, schedule, request.startLocation());
            if (afterTravel(schedule, journey).isAfter(schedule.limit())) {
                throw PlanProblem.invalid("Powrót do punktu startowego wykracza poza czas planu.");
            }
            schedule.travel("Powrót do punktu startowego", request.startLocation(), journey);
        }
    }

    private boolean addMeal(Schedule schedule, RouteLookup routes, List<Venue> venues, boolean last, PlanRequest request) {
        for (Venue venue : venues) {
            if (venue.durationMinutes() == null || venue.durationMinutes() <= 0) continue;
            Journey journey = route(routes, schedule, venue.coordinates());
            LocalDateTime arrival = afterTravel(schedule, journey);
            var opening = Schedule.firstOpening(arrival, venue.durationMinutes(), venue.openingHours());
            if (opening == null || (!last && opening.isAfter(arrival))) continue;
            var finish = opening.plusMinutes(venue.durationMinutes());
            if (last && request.returnToStart() && venue.coordinates() != null
                    && !venue.coordinates().equals(request.startLocation())) {
                var returnLeg = routes.between(venue.coordinates(), request.startLocation());
                if (returnLeg != null && returnLeg.minutes() != null) finish = finish.plusMinutes(returnLeg.minutes());
            }
            if (finish.isAfter(schedule.limit())) continue;
            schedule.travel("Dojazd: " + venue.name(), venue.coordinates(), journey);
            schedule.waitUntil(opening);
            schedule.add(venue.id(), Type.GASTRONOMY, venue.name(), venue.durationMinutes(),
                    venue.priceGroszy(), venue.address(), venue.coordinates(), null);
            return true;
        }
        return false;
    }

    private static Journey route(RouteLookup routes, Schedule schedule, Place destination) {
        return Objects.equals(schedule.point(), destination) && destination != null
                ? null : routes.between(schedule.point(), destination);
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

    private static <T> T optional(Supplier<T> action, T fallback, Set<String> warnings) {
        try {
            return action.get();
        } catch (IntegrationFailure failure) {
            warnings.add(failure.getMessage());
            return fallback;
        }
    }
}
