package com.marwix127.court_booking.common;

/**
 * Base para los errores que se traducen a HTTP 422: la peticion esta bien
 * formada y sus tipos son correctos, pero incumple una regla de negocio
 * (fuera de horario, pista cerrada, duracion no permitida).
 *
 * Se distingue del 400, que es para peticiones sintacticamente invalidas, y
 * del 409, que es para choques con el estado actual de los datos.
 */
public class UnprocessableException extends RuntimeException {

    public UnprocessableException(String message) {
        super(message);
    }
}
