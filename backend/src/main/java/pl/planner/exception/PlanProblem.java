package pl.planner.exception;

import org.springframework.http.HttpStatus;

public class PlanProblem extends RuntimeException {
    private final HttpStatus status;

    private PlanProblem(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static PlanProblem invalid(String message) {
        return new PlanProblem(HttpStatus.BAD_REQUEST, message);
    }

    public static PlanProblem attractionNotFound() {
        return new PlanProblem(HttpStatus.NOT_FOUND, "Nie znaleziono wybranej atrakcji.");
    }

    public HttpStatus status() {
        return status;
    }
}
