package com.aikids.entrypoints.rest.exception;

import com.aikids.application.shared.NotFoundException;
import com.aikids.domain.script.InvalidScriptException;
import com.aikids.domain.shared.DomainException;
import com.aikids.domain.shared.InvalidStateException;
import com.aikids.entrypoints.rest.mapper.ResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Un solo formato de error (RFC 9457) para toda la API. */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidScriptException.class)
    ProblemDetail invalidScript(InvalidScriptException e) {
        ProblemDetail detail = problem(HttpStatus.UNPROCESSABLE_CONTENT, e.code(), e.getMessage());
        detail.setProperty("problems", ResponseMapper.problems(e.problems()));
        return detail;
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException e) {
        HttpStatus status = switch (e) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case InvalidStateException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return problem(status, e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).distinct()
                .reduce((a, b) -> a + "; " + b).orElse("revisa los datos");
        return problem(HttpStatus.BAD_REQUEST, "DATO_INVALIDO", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail unreadable(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "DATO_INVALIDO", "La solicitud no tiene el formato esperado.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail missing(NoResourceFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "No existe esa dirección.");
    }

    /** El navegador cerró la conexión (pasa al adelantar un video): no hay a quién responder. */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void clientLeft() {
        LOG.debug("El navegador cerró la conexión");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        LOG.error("Error inesperado", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INESPERADO", "Ocurrió un error inesperado. Intenta de nuevo.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }
}
