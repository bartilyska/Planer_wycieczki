package pl.planner.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import pl.planner.dto.Attraction;
import pl.planner.dto.Place;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.integration.AttractionPayload;

@Component
public class AttractionMapper {
    public Attraction map(AttractionPayload source) {
        if (source.id() == null || source.id().isBlank() || source.name() == null || source.name().isBlank()
                || source.durationMinutes() == null || source.durationMinutes() < 1
                || (source.priceGroszy() != null && source.priceGroszy() < 0)) {
            throw new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
        }
        Place place = source.coordinates() == null ? null : new Place(source.coordinates().lat(), source.coordinates().lng());
        if (place != null && !place.usable()) throw new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
        return new Attraction(source.id(), source.name(), source.location(), source.description(),
                source.category(), source.indoor(), source.priceGroszy(), source.durationMinutes(), place, List.of());
    }
}
