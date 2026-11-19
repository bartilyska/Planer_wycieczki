package pl.planner.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import pl.planner.config.IntegrationProperties;
import pl.planner.config.PlannerProperties;
import pl.planner.dto.Attraction;
import pl.planner.dto.PlanRequest;
import pl.planner.dto.PlannerOptions;
import pl.planner.dto.PlannerOptions.Mode;
import pl.planner.dto.PlannerOptions.Status;
import pl.planner.exception.IntegrationFailure;
import pl.planner.integration.GastronomyClient;
import pl.planner.integration.TransportClient;
import pl.planner.integration.WeatherClient;

import static pl.planner.dto.PlanRequest.TransportMode.*;

@Service
public class OptionsService {
    private final AttractionCatalog catalog;
    private final PlannerProperties planner;
    private final IntegrationProperties integration;
    private final Clock clock;
    private final ObjectProvider<WeatherClient.ResponseMapper> weather;
    private final ObjectProvider<TransportClient.Contract> transport;
    private final ObjectProvider<GastronomyClient.Contract> gastronomy;

    public OptionsService(AttractionCatalog catalog, PlannerProperties planner, IntegrationProperties integration,
                          Clock clock, ObjectProvider<WeatherClient.ResponseMapper> weather,
                          ObjectProvider<TransportClient.Contract> transport, ObjectProvider<GastronomyClient.Contract> gastronomy) {
        this.catalog = catalog;
        this.planner = planner;
        this.integration = integration;
        this.clock = clock;
        this.weather = weather;
        this.transport = transport;
        this.gastronomy = gastronomy;
    }

    public PlannerOptions options() {
        var catalogInfo = catalogOptions();
        return new PlannerOptions(planner.demo(), LocalDate.now(clock), PlanRequest.MAX_ATTRACTIONS, catalogInfo.categories(),
                List.of(new Mode(WALK, "Pieszo"), new Mode(BIKE, "Rower"), new Mode(PUBLIC_TRANSPORT, "Komunikacja miejska"),
                        new Mode(CAR, "Samochód")),
                Map.of("attractions", catalogInfo.status(),
                        "weather", status(integration.weather(), weather.getIfAvailable() != null),
                        "transport", status(integration.transport(), transport.getIfAvailable() != null),
                        "gastronomy", status(integration.gastronomy(), gastronomy.getIfAvailable() != null)),
                true, true, catalogInfo.warning());
    }

    private CatalogOptions catalogOptions() {
        try {
            var categories = catalog.list(null, null).stream().map(Attraction::category)
                    .filter(Objects::nonNull).filter(category -> !category.isBlank()).distinct().sorted().toList();
            return new CatalogOptions(categories, status(integration.attractions(), true), null);
        } catch (IntegrationFailure unavailable) {
            Status status = unavailable.reason() == IntegrationFailure.Reason.NOT_CONFIGURED
                    ? Status.NOT_CONFIGURED : Status.UNAVAILABLE;
            return new CatalogOptions(List.of(), status, unavailable.getMessage());
        }
    }

    private Status status(IntegrationProperties.Service service, boolean contractReady) {
        if (planner.demo()) return Status.DEMO;
        if (!service.configured()) return Status.NOT_CONFIGURED;
        return contractReady ? Status.CONFIGURED : Status.CONTRACT_PENDING;
    }

    private record CatalogOptions(List<String> categories, Status status, String warning) {}
}
