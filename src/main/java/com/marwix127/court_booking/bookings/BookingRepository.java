package com.marwix127.court_booking.bookings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Reservas en un estado dado que solapan con [startsAt, endsAt).
     *
     * La condicion de solape de dos intervalos es "cada uno empieza antes de
     * que acabe el otro": startsAt < ventanaFin AND endsAt > ventanaInicio.
     * Ojo al orden de los parametros, va cruzado respecto al nombre.
     */
    List<Booking> findByCourtIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID courtId, BookingStatus status, Instant windowEnd, Instant windowStart);
}
