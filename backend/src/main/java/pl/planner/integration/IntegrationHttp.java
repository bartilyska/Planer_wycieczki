package pl.planner.integration;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.planner.config.IntegrationProperties;
import pl.planner.exception.IntegrationFailure;
import pl.planner.exception.IntegrationFailure.Reason;
import pl.planner.exception.PlanProblem;
import reactor.core.publisher.Mono;

@Component
public class IntegrationHttp {
    private final WebClient.Builder builder;

    public IntegrationHttp(WebClient.Builder builder) {
        this.builder = builder;
    }

    Endpoint endpoint(String name, IntegrationProperties.Service config) {
        return new Endpoint(name, config);
    }

    public final class Endpoint {
        private final String name;
        private final IntegrationProperties.Service config;
        private final WebClient client;

        private Endpoint(String name, IntegrationProperties.Service config) {
            this.name = name;
            this.config = config;
            this.client = config.configured() ? builder.clone().baseUrl(config.baseUrl().strip()).build() : null;
        }

        public void requireConfigured() {
            if (client == null) throw new IntegrationFailure(name, Reason.NOT_CONFIGURED);
        }

        public IntegrationFailure waitingForContract() {
            return new IntegrationFailure(name, Reason.CONTRACT_PENDING);
        }

        public <T> T exchange(Function<WebClient, Mono<T>> request, boolean confirmedAttraction404) {
            requireConfigured();
            return apply(request, confirmedAttraction404).block();
        }

        private <T> Mono<T> apply(Function<WebClient, Mono<T>> request, boolean confirmedAttraction404) {
            return Mono.defer(() -> request.apply(client))
                    .timeout(config.timeout())
                    .switchIfEmpty(Mono.error(new IntegrationFailure(name, Reason.NO_DATA)))
                    .onErrorMap(TimeoutException.class, e -> new IntegrationFailure(name, Reason.TIMEOUT, e))
                    .onErrorMap(WebClientRequestException.class, e -> new IntegrationFailure(name, Reason.UNAVAILABLE, e))
                    .onErrorMap(e -> e instanceof CodecException || e instanceof DataBufferLimitException
                                    || e instanceof UnsupportedMediaTypeException,
                            e -> new IntegrationFailure(name, Reason.INVALID_RESPONSE, e))
                    .onErrorMap(WebClientResponseException.class, e -> responseError(e, confirmedAttraction404))
                    .onErrorMap(IllegalArgumentException.class, e -> new IntegrationFailure(name, Reason.INVALID_RESPONSE, e));
        }

        private RuntimeException responseError(WebClientResponseException error, boolean confirmedAttraction404) {
            HttpStatusCode status = error.getStatusCode();
            if (confirmedAttraction404 && status.value() == 404) return PlanProblem.attractionNotFound();
            Reason reason = switch (status.value()) {
                case 408, 504 -> Reason.TIMEOUT;
                case 429 -> Reason.UNAVAILABLE;
                default -> status.is5xxServerError() ? Reason.UNAVAILABLE : Reason.INVALID_RESPONSE;
            };
            return new IntegrationFailure(name, reason, error);
        }
    }
}
