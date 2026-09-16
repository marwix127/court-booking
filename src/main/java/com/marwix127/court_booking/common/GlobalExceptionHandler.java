package com.marwix127.court_booking.common;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones de dominio a respuestas HTTP en un unico sitio, para que
 * los controladores no tengan que saber nada de codigos de estado.
 *
 * Devuelve ProblemDetail (RFC 9457): cuerpo estandar application/problem+json
 * con status, title y detail, en lugar de un JSON inventado por cada endpoint.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Bloqueo optimista: otra peticion modifico la misma fila entre nuestra
     * lectura y nuestra escritura, y la columna @Version ya no coincide.
     *
     * Es un 409 por el mismo motivo que el solape: la peticion era valida, el
     * estado cambio debajo. Quien la reciba solo tiene que volver a leer y
     * reintentar.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource was modified by another request; retry");
    }

    /**
     * Reglas de negocio incumplidas: la peticion es correcta en forma pero no
     * se puede atender. 422 y no 400, que queda reservado para errores de
     * sintaxis y de validacion de campos.
     */
    @ExceptionHandler(UnprocessableException.class)
    public ProblemDetail handleUnprocessable(UnprocessableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /**
     * Fallos de Bean Validation (@Valid) sobre el cuerpo de la peticion.
     * Ademas del 400, devuelve un mapa campo -> motivo en la propiedad
     * "errors", para que el cliente sepa exactamente que corregir.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : "invalid value";
            // Si un campo incumple varias restricciones, se concatenan.
            errors.merge(fieldError.getField(), message, (a, b) -> a + "; " + b);
        }

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
