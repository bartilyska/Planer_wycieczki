package pl.planner.integration;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.planner.config.IntegrationProperties;
import pl.planner.integration.PlannerSources.Venue;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class GastronomyClient implements PlannerSources.Gastronomy {
    /** The implementation defines the endpoint, wire DTOs and mapping to Venue. */
    public interface Contract {
        List<Venue> venues(IntegrationHttp.Endpoint endpoint, String city, LocalDate date);
    }

    private final IntegrationHttp.Endpoint endpoint;
    private final ObjectProvider<Contract> contracts;

    public GastronomyClient(IntegrationHttp http, IntegrationProperties properties, ObjectProvider<Contract> contracts) {
        this.endpoint = http.endpoint("gastronomy", "Gastronomia", properties.gastronomy());
        this.contracts = contracts;
    }

    @Override
    public List<Venue> venues(String city, LocalDate date) {
        endpoint.requireConfigured();
        var contract = contracts.getIfAvailable();
        if (contract == null) throw endpoint.waitingForContract();
        return contract.venues(endpoint, city, date);
    }
}
