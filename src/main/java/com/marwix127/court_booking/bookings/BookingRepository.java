package com.marwix127.court_booking.bookings;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * Igual que findById pero trayendo court y user en la misma consulta.
     *
     * Las dos asociaciones son LAZY, que es lo correcto por defecto. Pero la
     * respuesta necesita el nombre de la pista, y si se accede a el despues de
     * cerrar la transaccion salta LazyInitializationException. El EntityGraph
     * las carga por adelantado, con un JOIN, solo donde hace falta.
     */
    @EntityGraph(attributePaths = { "court", "user" })
    Optional<Booking> findWithCourtAndUserById(UUID id);
}
