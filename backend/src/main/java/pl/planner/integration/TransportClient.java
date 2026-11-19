package pl.planner.integration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.planner.config.IntegrationProperties;
import pl.planner.dto.Place;
import pl.planner.dto.PlanRequest.TransportMode;
import pl.planner.integration.PlannerSources.Journey;

@Component
@ConditionalOnProperty(name = "planner.demo", havingValue = "false", matchIfMissing = true)
public class TransportClient implements PlannerSources.Transport {
    /** The implementation defines the endpoint, wire DTOs and mapping to Journey. */
    public interface Contract {
        Journey leg(IntegrationHttp.Endpoint endpoint, Place from, Place to, TransportMode mode);
    }

    private final IntegrationHttp.Endpoint endpoint;
    private final ObjectProvider<Contract> contracts;

    public TransportClient(IntegrationHttp http, IntegrationProperties properties, ObjectProvider<Contract> contracts) {
        this.endpoint = http.endpoint("transport", "Transport", properties.transport());
        this.contracts = contracts;
    }

    @Override
    public Journey leg(Place from, Place to, TransportMode mode) {
        endpoint.requireConfigured();
        var contract = contracts.getIfAvailable();
        if (contract == null) throw endpoint.waitingForContract();
        return contract.leg(endpoint, from, to, mode);
    }
}
