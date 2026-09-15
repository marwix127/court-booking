package com.marwix127.court_booking.common;

/**
 * Base para los errores que se traducen a HTTP 404: el recurso pedido no
 * existe. Las excepciones concretas heredan de esta y el
 * GlobalExceptionHandler las mapea todas de golpe.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
