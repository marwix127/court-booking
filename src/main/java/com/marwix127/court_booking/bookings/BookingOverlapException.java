package com.marwix127.court_booking.bookings;

import com.marwix127.court_booking.common.ConflictException;

/**
 * La franja pedida choca con otra reserva activa de la misma pista.
 * Nace de la restriccion booking_no_overlap de Postgres, no de una
 * comprobacion previa en Java: es la base de datos la que decide.
 */
public class BookingOverlapException extends ConflictException {

    public BookingOverlapException() {
        super("The requested time slot is already booked for this court");
    }
}
