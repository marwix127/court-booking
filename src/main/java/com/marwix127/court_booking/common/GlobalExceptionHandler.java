package com.marwix127.court_booking.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
