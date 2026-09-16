package com.marwix127.court_booking.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.marwix127.court_booking.opening_hours.OpeningHoursRepository;

import lombok.RequiredArgsConstructor;

/**
 * Expone el horario de apertura como un valor simple, sin entidad JPA.
 *
 * Asi AvailabilityService no depende de la entidad OpeningHours ni de su
 * relacion perezosa con Court: solo necesita dos horas.
 */
@Component
@RequiredArgsConstructor
public class OpeningHoursLookup {

    private final OpeningHoursRepository openingHoursRepository;

    public record Hours(LocalTime opensAt, LocalTime closesAt) {
    }

    public Optional<Hours> forCourtAndDay(UUID courtId, DayOfWeek dayOfWeek) {
        return openingHoursRepository.findByCourtIdAndDayOfWeek(courtId, dayOfWeek)
                .map(oh -> new Hours(oh.getOpensAt(), oh.getClosesAt()));
    }
}
