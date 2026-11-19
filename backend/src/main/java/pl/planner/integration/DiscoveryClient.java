package pl.planner.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import pl.planner.config.DiscoveryProperties;
import pl.planner.config.PlannerProperties;
import reactor.core.publisher.Mono;

@Component
public class DiscoveryClient {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private final WebClient client;
    private final Map<String, Mono<String>> entries = new ConcurrentHashMap<>();

    public DiscoveryClient(WebClient.Builder builder, DiscoveryProperties discovery, PlannerProperties planner) {
        client = discovery.enabled() && !planner.demo() && StringUtils.hasText(discovery.baseUrl())
                ? builder.clone().baseUrl(discovery.baseUrl().strip()).build() : null;
    }

    Optional<String> lookup(String name) {
        if (client == null) return Optional.empty();
        return entries.computeIfAbsent(name, key -> Mono.defer(() -> fetch(key)).cache(CACHE_TTL)).blockOptional();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerPlanner() {
        if (client == null) return;
        client.post().uri("/services")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "planner"))
                .retrieve()
                .toBodilessEntity()
                .timeout(TIMEOUT)
                .subscribe(result -> LOG.info("Zarejestrowano planner w discovery (HTTP {}).", result.getStatusCode().value()),
                        failure -> LOG.warn("Rejestracja planner w discovery nie powiodła się ({}).",
                                failure.getClass().getSimpleName()));
    }

    private Mono<String> fetch(String name) {
        return client.get().uri("/services/{name}", name)
                .retrieve().bodyToMono(Entry.class)
                .timeout(TIMEOUT)
                .<String>handle((entry, sink) -> {
                    if (name.equalsIgnoreCase(entry.name()) && entry.ip() != null) {
                        sink.next(InetAddress.ofLiteral(entry.ip()).getHostAddress());
                    }
                })
                .onErrorResume(error -> {
                    LOG.debug("Discovery nie zwróciło adresu dla {} ({}).", name, error.getClass().getSimpleName());
                    return Mono.empty();
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String name, String ip) {}
}
