package pl.planner.exception;

public class IntegrationFailure extends RuntimeException {
    public enum Reason { NOT_CONFIGURED, CONTRACT_PENDING, TIMEOUT, UNAVAILABLE, INVALID_RESPONSE, NO_DATA }

    private final Reason reason;

    public IntegrationFailure(String service, Reason reason) {
        this(service, reason, null);
    }

    public IntegrationFailure(String service, Reason reason, Throwable cause) {
        super(service + " — " + switch (reason) {
            case NOT_CONFIGURED -> "brak adresu usługi.";
            case CONTRACT_PENDING -> "integracja oczekuje na kontrakt API.";
            case TIMEOUT -> "przekroczono czas oczekiwania.";
            case UNAVAILABLE -> "usługa jest niedostępna.";
            case INVALID_RESPONSE -> "usługa zwróciła nieprawidłową odpowiedź.";
            case NO_DATA -> "usługa nie zwróciła danych.";
        }, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
