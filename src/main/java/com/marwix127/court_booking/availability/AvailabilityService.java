package com.marwix127.court_booking.availability;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marwix127.court_booking.bookings.BookingRepository;
import com.marwix127.court_booking.bookings.BookingStatus;
import com.marwix127.court_booking.closures.ClosureRepository;
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.court.CourtNotFoundException;
import com.marwix127.court_booking.court.CourtRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final CourtRepository courtRepository;
    private final OpeningHoursLookup openingHours;
    private final BookingRepository bookingRepository;
    private final ClosureRepository closureRepository;

    /**
     * Zona horaria del club. Las opening_hours son horas locales y las reservas
     * instantes absolutos: para cruzarlas hace falta una zona explicita. Nunca
     * ZoneId.systemDefault(), que cambia entre el portatil y el contenedor.
     */
    @Value("${app.club.timezone}")
    private String clubTimezone;

    /** Tramo cerrado-abierto [start, end), igual que el rango del EXCLUDE. */
    private record Interval(Instant start, Instant end) {

        boolean overlaps(Interval other) {
            // Dos intervalos solapan si cada uno empieza antes de que acabe el otro.
            return start.isBefore(other.end) && end.isAfter(other.start);
        }
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse findFreeSlots(UUID courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));

        short slotMinutes = court.getSlotMinutes();
        var zone = ZoneId.of(clubTimezone);

        // El dia de la semana sale de la fecha local del club, que es la misma
        // referencia con la que estan guardadas las opening_hours.
        var hours = openingHours.forCourtAndDay(courtId, date.getDayOfWeek());

        // Sin horario ese dia, o pista dada de baja: no hay nada que ofrecer.
        if (hours.isEmpty() || !court.isActive()) {
            return new AvailabilityResponse(courtId, date, slotMinutes, List.of());
        }

        List<Interval> candidates = buildSlots(
                date, hours.get().opensAt(), hours.get().closesAt(), slotMinutes, zone);

        if (candidates.isEmpty()) {
            return new AvailabilityResponse(courtId, date, slotMinutes, List.of());
        }

        // Una sola consulta por tabla para toda la ventana del dia, en vez de
        // una por tramo: con 13 tramos serian 26 consultas.
        Instant dayStart = candidates.get(0).start();
        Instant dayEnd = candidates.get(candidates.size() - 1).end();

        List<Interval> busy = new ArrayList<>();
        bookingRepository
                .findByCourtIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                        courtId, BookingStatus.CONFIRMED, dayEnd, dayStart)
                .forEach(b -> busy.add(new Interval(b.getStartsAt(), b.getEndsAt())));
        closureRepository
                .findByCourtIdAndStartsAtLessThanAndEndsAtGreaterThan(courtId, dayEnd, dayStart)
                .forEach(c -> busy.add(new Interval(c.getStartsAt(), c.getEndsAt())));

        Instant now = Instant.now();
        List<SlotResponse> free = new ArrayList<>();
        for (Interval slot : candidates) {
            // Un tramo ya empezado no se puede reservar: @Future lo rechazaria.
            if (slot.start().isBefore(now)) {
                continue;
            }
            if (busy.stream().noneMatch(slot::overlaps)) {
                free.add(new SlotResponse(slot.start(), slot.end()));
            }
        }

        return new AvailabilityResponse(courtId, date, slotMinutes, free);
    }

    /**
     * Parte el horario de apertura en tramos consecutivos de slotMinutes.
     * El ultimo solo entra si cabe completo antes del cierre: con apertura
     * 09:00-22:00 y tramos de 90 minutos, el que empezaria a las 21:00 no cabe
     * y se descarta.
     */
    private List<Interval> buildSlots(LocalDate date, LocalTime opensAt, LocalTime closesAt,
            short slotMinutes, ZoneId zone) {

        List<Interval> slots = new ArrayList<>();
        for (LocalTime from = opensAt; !from.plusMinutes(slotMinutes).isAfter(closesAt);
                from = from.plusMinutes(slotMinutes)) {

            LocalTime to = from.plusMinutes(slotMinutes);
            // La conversion a instante se hace tramo a tramo con la zona del
            // club, no con un desplazamiento fijo: asi un cambio de hora lo
            // resuelven las reglas de la zona y no nosotros a mano.
            slots.add(new Interval(
                    date.atTime(from).atZone(zone).toInstant(),
                    date.atTime(to).atZone(zone).toInstant()));
        }
        return slots;
    }
}
