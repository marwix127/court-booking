package com.marwix127.court_booking.bookings;

import com.marwix127.court_booking.common.ForbiddenException;

/**
 * Se devuelve 403 y no 404 a proposito: confirma que la reserva existe, pero
 * en una aplicacion de club no hay nada sensible en ese hecho, y el mensaje
 * honesto ayuda mas que ocultarlo.
 */
public class NotBookingOwnerException extends ForbiddenException {

    public NotBookingOwnerException() {
        super("You can only cancel your own bookings");
    }
}
