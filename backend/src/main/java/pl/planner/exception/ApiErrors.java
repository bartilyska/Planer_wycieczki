package pl.planner.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(IntegrationFailure.class)
    ProblemDetail integration(IntegrationFailure failure) {
        LOG.warn("{}", failure.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, failure.getMessage());
        problem.setProperty("code", failure.reason().name());
        return problem;
    }

    @ExceptionHandler(PlanProblem.class)
    ProblemDetail plan(PlanProblem failure) {
        return ProblemDetail.forStatusAndDetail(failure.status(), failure.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException failure) {
        Map<String, String> fields = new LinkedHashMap<>();
        failure.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Sprawdź dane wejściowe.");
        problem.setProperty("fields", fields);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail invalidBody() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Nieprawidłowy format żądania. Sprawdź datę, godzinę i środek transportu.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception failure) {
        if (failure instanceof ErrorResponse response) {
            return response.getBody();
        }
        LOG.error("Nieoczekiwany błąd planera", failure);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Wystąpił nieoczekiwany błąd planera.");
    }
}
