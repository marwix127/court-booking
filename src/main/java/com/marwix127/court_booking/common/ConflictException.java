package com.marwix127.court_booking.common;

/**
 * Base para los errores que se traducen a HTTP 409: la peticion es valida
 * pero choca con el estado actual (email ya registrado, franja ya reservada).
 * Las excepciones concretas heredan de esta y el GlobalExceptionHandler las
 * mapea todas de golpe.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
