package pl.planner.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import pl.planner.config.IntegrationProperties;
import pl.planner.config.PlannerProperties;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.Place;
import pl.planner.dto.PlannerOptions;
import pl.planner.dto.PlannerOptions.Mode;
import pl.planner.dto.PlannerOptions.Status;
import pl.planner.exception.IntegrationFailure;
import pl.planner.integration.GastronomyClient;

import static pl.planner.dto.PlanRequest.TransportMode.*;

@Service
public class OptionsService {
    private final AttractionCatalog catalog;
    private final PlannerProperties planner;
    private final IntegrationProperties integration;
    private final Clock clock;
    private final ObjectProvider<GastronomyClient.Contract> gastronomy;

    public OptionsService(AttractionCatalog catalog, PlannerProperties planner, IntegrationProperties integration,
                          Clock clock,
                          ObjectProvider<GastronomyClient.Contract> gastronomy) {
        this.catalog = catalog;
        this.planner = planner;
        this.integration = integration;
        this.clock = clock;
        this.gastronomy = gastronomy;
    }

    public PlannerOptions options() {
        var catalogInfo = catalogOptions();
        var modes = planner.demo()
                ? List.of(new Mode(WALK, "Pieszo"), new Mode(BIKE, "Rower"), new Mode(PUBLIC_TRANSPORT, "Komunikacja miejska"), new Mode(CAR, "Samochód"))
                : List.of(new Mode(WALK, "Pieszo"), new Mode(PUBLIC_TRANSPORT, "Komunikacja miejska"), new Mode(CAR, "Samochód"));
        return new PlannerOptions(planner.demo(), LocalDate.now(clock), PlanRequest.MAX_ATTRACTIONS,
                List.copyOf(catalogInfo.categories().keySet()), modes,
                Map.of("attractions", catalogInfo.status(),
                        "weather", catalogInfo.status(),
                        "transport", status(integration.transport(), true),
                        "gastronomy", status(integration.gastronomy(), gastronomy.getIfAvailable() != null)),
                true, true, catalogInfo.warning(), catalogInfo.categories(),
                planner.demo() ? null : new Place(52.2297, 21.0122));
    }

    private CatalogOptions catalogOptions() {
        try {
            var categories = new LinkedHashMap<String, String>();
            catalog.categories().forEach(category -> categories.putIfAbsent(category.code(), category.name()));
            return new CatalogOptions(categories, status(integration.attractions(), true), null);
        } catch (IntegrationFailure unavailable) {
            Status status = unavailable.reason() == IntegrationFailure.Reason.NOT_CONFIGURED
                    ? Status.NOT_CONFIGURED : Status.UNAVAILABLE;
            return new CatalogOptions(Map.of(), status, unavailable.getMessage());
        }
    }

    private Status status(IntegrationProperties.Service service, boolean contractReady) {
        if (planner.demo()) return Status.DEMO;
        if (!service.configured()) return Status.NOT_CONFIGURED;
        return contractReady ? Status.CONFIGURED : Status.CONTRACT_PENDING;
    }

    private record CatalogOptions(Map<String, String> categories, Status status, String warning) {}
}
