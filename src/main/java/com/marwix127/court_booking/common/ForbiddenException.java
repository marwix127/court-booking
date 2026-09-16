package com.marwix127.court_booking.common;

/**
 * Base para los errores que se traducen a HTTP 403: el cliente esta
 * autenticado, sabemos quien es, y no tiene permiso sobre este recurso.
 *
 * Distinto del 401, que es "no se quien eres".
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
