package pl.planner.integration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.planner.dto.Attraction;
import pl.planner.dto.Attraction.OpeningWindow;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Condition;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.Journey;
import pl.planner.integration.PlannerSources.Venue;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "true")
public class DemoSources implements PlannerSources.Attractions, PlannerSources.Forecast,
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
    public Weather day(String city, LocalDate date) {
        return new Weather(BigDecimal.valueOf(18), "Słonecznie · prognoza przykładowa", Condition.FAVORABLE_OUTDOOR, true);
    }

    @Override
    public Journey leg(Place from, Place to, TransportMode mode) {
        double speed = switch (mode) {
            case WALK -> 4.5;
            case BIKE -> 14;
            case PUBLIC_TRANSPORT -> 17;
            case CAR -> 24;
        };
        double distance = from.distanceTo(to) * 1.25;
        Long cost = mode == TransportMode.PUBLIC_TRANSPORT ? 440L : 0L;
        return new Journey((int) Math.ceil(distance / speed * 60),
                BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP), cost, mode.name(), List.of());
    }

    @Override
    public List<Venue> venues(String city, LocalDate date) {
        Place point = city.equalsIgnoreCase("Kraków") ? new Place(50.0580, 19.9373) : new Place(52.2330, 21.0199);
        return List.of(new Venue("demo-lunch", "Bistro Podróżnika · przykład", city, point, "Bistro", 3900L, 45,
                List.of(new OpeningWindow(LocalTime.of(11, 30), LocalTime.of(21, 0)))));
    }

    private static Attraction attraction(String id, String name, String city, String description, String category,
                                         boolean indoor, long price, int duration, double lat, double lng) {
        return new Attraction(id, name, city, description, category, indoor, price, duration, new Place(lat, lng), List.of());
    }
}
