package pl.planner.service;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import pl.planner.dto.Attraction;
import pl.planner.exception.PlanProblem;
import pl.planner.integration.PlannerSources.Attractions;

@Service
public class AttractionCatalog {
    private static final Pattern ID = Pattern.compile("[^/\\\\%?#\\p{Cntrl}]{1,150}");
    private final Attractions source;

    public AttractionCatalog(Attractions source) {
        this.source = source;
    }

    public List<Attraction> list(String city, String category) {
        return source.list().stream()
                .filter(value -> matches(value.location(), city) && matches(value.category(), category))
                .toList();
    }

    public Attraction detail(String id) {
        if (!ID.matcher(id).matches() || id.isBlank() || id.equals(".") || id.equals("..")) {
            throw PlanProblem.invalid("Nieprawidłowy identyfikator atrakcji.");
        }
        return source.detail(id);
    }

    static boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.strip().equalsIgnoreCase(filter.strip()));
    }
}
