package pl.planner.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import pl.planner.dto.PlanRequest.TransportMode;

public record PlannerOptions(boolean demo, LocalDate earliestDate, int maxAttractions,
                             List<String> categories, List<Mode> transportModes, Map<String, Status> integrations,
                             boolean supportsEndTime, boolean supportsReturnToStart, String catalogWarning) {
    public record Mode(TransportMode value, String label) {}
    public enum Status { DEMO, NOT_CONFIGURED, CONFIGURED, CONTRACT_PENDING, UNAVAILABLE }
}
