package pl.planner.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanRequest;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.Attractions;
import pl.planner.integration.PlannerSources.Category;
import pl.planner.integration.PlannerSources.DayData;
import pl.planner.integration.PlannerSources.DayProposal;

@Service
public class AttractionCatalog {
    private static final Pattern ID = Pattern.compile("[^/\\\\%?#\\p{Cntrl}]{1,150}");
    private final Attractions source;

    public AttractionCatalog(Attractions source) {
        this.source = source;
    }

    public List<Attraction> list(String city, String category) {
        return source.list().stream()
                .filter(value -> (value.location() == null || matches(value.location(), city)) && matches(value.category(), category))
                .toList();
    }

    public Attraction detail(String id) {
        validateId(id);
        return source.detail(id);
    }

    public List<Category> categories() {
        return source.categories();
    }

    public DayData day(PlanRequest request) {
        request.attractionIds().forEach(AttractionCatalog::validateId);
        var day = source.day(request);
        var byId = new LinkedHashMap<String, DayProposal>();
        for (var proposal : day.proposals()) {
            if (byId.putIfAbsent(proposal.attraction().id(), proposal) != null) {
                throw new IntegrationFailure("Atrakcje", Reason.INVALID_RESPONSE);
            }
        }
        var selected = request.attractionIds().stream().map(id -> {
            var proposal = byId.get(id);
            if (proposal != null) return proposal;
            var attraction = detail(id);
            throw PlanProblem.invalid("Atrakcja „" + attraction.name()
                    + "” nie jest dostępna w propozycjach dla tego dnia, punktu startu i środka transportu.");
        }).toList();
        return new DayData(day.weather(), selected, day.travelWarning());
    }

    private static void validateId(String id) {
        if (!ID.matcher(id).matches() || id.isBlank() || id.equals(".") || id.equals("..")) {
            throw PlanProblem.invalid("Nieprawidłowy identyfikator atrakcji.");
        }
    }

    static boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.strip().equalsIgnoreCase(filter.strip()));
    }
}
