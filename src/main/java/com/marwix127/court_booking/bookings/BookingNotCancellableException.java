package com.marwix127.court_booking.bookings;

import com.marwix127.court_booking.common.UnprocessableException;

/**
 * La reserva ya ha empezado. La regla del club es que se puede cancelar en
 * cualquier momento antes de la hora de comienzo, no despues.
 */
public class BookingNotCancellableException extends UnprocessableException {

    public BookingNotCancellableException() {
        super("A booking can only be cancelled before its start time");
    }
}
