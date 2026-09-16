package com.marwix127.court_booking.opening_hours;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, UUID> {

    /**
     * Horario de una pista para un dia concreto. Optional porque uq_opening_hours_court_day
     * garantiza como maximo una fila: si no hay, la pista no abre ese dia.
     */
    Optional<OpeningHours> findByCourtIdAndDayOfWeek(UUID courtId, DayOfWeek dayOfWeek);
}
