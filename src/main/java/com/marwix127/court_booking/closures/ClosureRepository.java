package com.marwix127.court_booking.closures;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClosureRepository extends JpaRepository<Closure, UUID> {

    /**
     * Existe algun cierre de esta pista que solape con [startsAt, endsAt)?
     *
     * La condicion de solape de dos intervalos es "cada uno empieza antes de
     * que acabe el otro". Con desigualdades estrictas, un cierre que termina
     * justo cuando empieza la reserva no cuenta como solape.
     */
    boolean existsByCourtIdAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID courtId, Instant endsAt, Instant startsAt);
}
