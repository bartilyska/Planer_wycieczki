package pl.planner.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import pl.planner.dto.Attraction;
import pl.planner.dto.Attraction.OpeningWindow;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.dto.PlanResponse.Condition;
import pl.planner.dto.PlanResponse.Weather;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.integration.AttractionPayload;
import pl.planner.integration.PlannerSources.DayData;
import pl.planner.integration.PlannerSources.DayProposal;
import pl.planner.integration.PlannerSources.Journey;

@Component
public class AttractionMapper {
    public Attraction catalog(AttractionPayload.CatalogItem source) {
        if (source == null) throw invalidResponse();
        validate(source.id(), source.name(), source.visitDurationMin());
        Map<DayOfWeek, OpeningWindow> hours = new EnumMap<>(DayOfWeek.class);
        if (source.openingHours() != null) {
            source.openingHours().forEach((day, value) -> {
                if (value != null) hours.put(day, window(value));
            });
        }
        return new Attraction(source.id(), source.name(), null, source.description(), source.category(), source.indoor(),
                groszy(source.pricePerPerson()), source.visitDurationMin(), coordinates(source.location()), List.of(),
                Map.copyOf(hours), source.location() == null ? null : source.location().id());
    }

    public DayData day(AttractionPayload.Day source, PlanRequest request) {
        if (!request.date().equals(source.date()) || source.proposals() == null) throw invalidResponse();
        var proposals = source.proposals().stream().map(value -> proposal(value, request.transportMode())).toList();
        var warning = source.transport() != null && "unavailable".equals(source.transport().source())
                ? "Atrakcje — brak danych o przejazdach z i do punktu startowego." : null;
        return new DayData(weather(source.weather()), proposals, warning);
    }

    private DayProposal proposal(AttractionPayload.Proposal source, TransportMode mode) {
        if (source == null) throw invalidResponse();
        validate(source.id(), source.name(), source.visitDurationMin());
        var hours = source.openingHours() == null ? List.<OpeningWindow>of() : List.of(window(source.openingHours()));
        var cost = source.ticket() == null ? null : groszy(source.ticket().total());
        var attraction = new Attraction(source.id(), source.name(), null, null, source.category(), source.indoor(),
                cost, source.visitDurationMin(), coordinates(source.location()), hours, Map.of(),
                source.location() == null ? null : source.location().id());
        return new DayProposal(attraction, leg(source.fromStart(), mode), leg(source.toStart(), mode));
    }

    private static Weather weather(AttractionPayload.Forecast source) {
        if (source == null || !"ok".equals(source.source()) || source.condition() == null) return Weather.unavailable();
        return switch (source.condition()) {
            case "sun" -> new Weather(source.tempC(), "Słonecznie", Condition.FAVORABLE_OUTDOOR, true);
            case "cloudy" -> new Weather(source.tempC(), "Pochmurno", Condition.NEUTRAL, true);
            case "rain" -> new Weather(source.tempC(), "Deszcz", Condition.ADVERSE_OUTDOOR, true);
            case "snow" -> new Weather(source.tempC(), "Śnieg", Condition.ADVERSE_OUTDOOR, true);
            case "storm" -> new Weather(source.tempC(), "Burza", Condition.ADVERSE_OUTDOOR, true);
            default -> Weather.unavailable();
        };
    }

    private static Journey leg(AttractionPayload.Leg value, TransportMode mode) {
        if (value == null) return null;
        if (value.timeMin() != null && value.timeMin() < 0) throw invalidResponse();
        return new Journey(value.timeMin(), null, groszy(value.costTotal()), mode.name(), List.of());
    }

    private static OpeningWindow window(AttractionPayload.Hours hours) {
        if (hours.open() == null || hours.close() == null) throw invalidResponse();
        return new OpeningWindow(hours.open(), hours.close());
    }

    private static Place coordinates(AttractionPayload.Coordinates location) {
        if (location == null) return null;
        var point = new Place(location.lat(), location.lng());
        if (!point.usable()) throw invalidResponse();
        return point;
    }

    private static void validate(String id, String name, Integer minutes) {
        if (id == null || id.isBlank() || name == null || name.isBlank() || minutes == null || minutes < 1) {
            throw invalidResponse();
        }
    }

    private static Long groszy(BigDecimal pln) {
        if (pln == null) return null;
        if (pln.signum() < 0) throw invalidResponse();
        try {
            return pln.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException tooLarge) {
            throw invalidResponse();
        }
    }

    private static IntegrationFailure invalidResponse() {
        return new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
    }
}
