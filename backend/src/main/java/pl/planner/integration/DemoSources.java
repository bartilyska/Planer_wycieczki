package pl.planner.integration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Condition;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.Category;
import pl.planner.integration.PlannerSources.DayData;
import pl.planner.integration.PlannerSources.DayProposal;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.LocationPair;
import pl.planner.integration.PlannerSources.MealProposal;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "true")
public class DemoSources implements PlannerSources.Attractions,
        PlannerSources.Transport, PlannerSources.Gastronomy {
    private static final List<Attraction> ATTRACTIONS = List.of(
            attraction("lazienki", "Łazienki Królewskie", "Warszawa", "Zespół parkowo-pałacowy z ogrodami i Pałacem na Wyspie.", "Przyroda", false, 0, 90, 52.2149, 21.0356),
            attraction("muzeum-narodowe", "Muzeum Narodowe", "Warszawa", "Galerie sztuki polskiej i światowej przy Alejach Jerozolimskich.", "Kultura", true, 2500, 90, 52.2317, 21.0248),
            attraction("stare-miasto", "Rynek Starego Miasta", "Warszawa", "Spacer po rynku i uliczkach warszawskiego Starego Miasta.", "Zabytki", false, 0, 60, 52.2498, 21.0122),
            attraction("kopernik", "Centrum Nauki Kopernik", "Warszawa", "Interaktywne wystawy naukowe na Powiślu.", "Nauka", true, 4500, 120, 52.2419, 21.0288),
            attraction("wawel", "Zamek Królewski na Wawelu", "Kraków", "Królewskie wnętrza i dziedziniec na Wzgórzu Wawelskim.", "Zabytki", true, 4900, 120, 50.0540, 19.9354),
            attraction("planty", "Planty", "Kraków", "Spacer zielonym pierścieniem wokół Starego Miasta.", "Przyroda", false, 0, 60, 50.0621, 19.9426));

    @Override
    public List<Attraction> list() {
        return ATTRACTIONS;
    }

    @Override
    public Attraction detail(String id) {
        return ATTRACTIONS.stream().filter(attraction -> attraction.id().equals(id)).findFirst()
                .orElseThrow(PlanProblem::attractionNotFound);
    }

    @Override
    public List<Category> categories() {
        return ATTRACTIONS.stream().map(Attraction::category).distinct().sorted()
                .map(name -> new Category(name, name)).toList();
    }

    @Override
    public DayData day(PlanRequest request) {
        var proposals = request.attractionIds().stream().map(this::detail).map(item ->
                new DayProposal(new Attraction(item.id(), item.name(), item.location(), item.description(), item.category(),
                        item.indoor(), item.priceGroszy() * request.groupSize(), item.durationMinutes(), item.coordinates(),
                        item.openingHours(), Map.of(), item.transportLocationId()), null, null)).toList();
        var weather = new Weather(BigDecimal.valueOf(18), "Słonecznie · prognoza przykładowa", Condition.FAVORABLE_OUTDOOR, true);
        return new DayData(weather, proposals, null);
    }

    @Override
    public Map<LocationPair, Journey> routes(Map<String, Place> locations, List<LocationPair> pairs,
                                             TransportMode mode, int people) {
        var journeys = new HashMap<LocationPair, Journey>();
        for (var pair : pairs) {
            var from = locations.get(pair.from());
            var to = locations.get(pair.to());
            if (from != null && to != null) journeys.put(pair, demoJourney(from, to, mode, people));
        }
        return journeys;
    }

    private Journey demoJourney(Place from, Place to, TransportMode mode, int people) {
        double speed = switch (mode) {
            case WALK -> 4.5;
            case BIKE -> 14;
            case PUBLIC_TRANSPORT -> 17;
            case CAR -> 24;
        };
        long cost = mode == TransportMode.PUBLIC_TRANSPORT ? 440L * people : 0L;
        double distance = from.distanceTo(to) * 1.25;
        return new Journey((int) Math.ceil(distance / speed * 60),
                BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP), cost, mode.name(), List.of());
    }

    @Override
    public List<MealProposal> proposals(String fromLocationId, PlanRequest request) {
        String locationId = "demo-lunch";
        Place point = request.city().equalsIgnoreCase("Kraków") ? new Place(50.0580, 19.9373) : new Place(52.2330, 21.0199);
        var from = PlannerSources.Transport.START.equals(fromLocationId)
                ? request.startLocation() : detail(fromLocationId).coordinates();
        var inbound = demoJourney(from, point, request.transportMode(), request.groupSize());
        return List.of(new MealProposal(locationId, "demo-meal", "Bistro Podróżnika · przykład", "Zupa warzywna",
                45, 3900L * request.groupSize(), inbound, false, point));
    }

    private static Attraction attraction(String id, String name, String city, String description, String category,
                                         boolean indoor, long price, int duration, double lat, double lng) {
        return new Attraction(id, name, city, description, category, indoor, price, duration, new Place(lat, lng),
                List.of(), Map.of(), id);
    }
}
