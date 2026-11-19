package pl.planner.exception;

public class IntegrationFailure extends RuntimeException {
    public enum Reason { NOT_CONFIGURED, CONTRACT_PENDING, TIMEOUT, UNAVAILABLE, INVALID_RESPONSE, NO_DATA, AUTHORIZATION }

    private final Reason reason;

    public IntegrationFailure(String service, Reason reason) {
        this(service, reason, null);
    }

    public IntegrationFailure(String service, Reason reason, Throwable cause) {
        this(reason, service + " — " + switch (reason) {
            case NOT_CONFIGURED -> "brak adresu usługi.";
            case CONTRACT_PENDING -> "integracja oczekuje na kontrakt API.";
            case TIMEOUT -> "przekroczono czas oczekiwania.";
            case UNAVAILABLE -> "usługa jest niedostępna.";
            case INVALID_RESPONSE -> "usługa zwróciła nieprawidłową odpowiedź.";
            case NO_DATA -> "usługa nie zwróciła danych.";
            case AUTHORIZATION -> "usługa odrzuciła uwierzytelnienie. Sprawdź konfigurację dostępu.";
        }, cause);
    }

    public static IntegrationFailure unavailable(String detail) {
        return new IntegrationFailure(Reason.UNAVAILABLE, detail, null);
    }

    private IntegrationFailure(Reason reason, String detail, Throwable cause) {
        super(detail, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
