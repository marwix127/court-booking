package com.marwix127.court_booking.availability;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marwix127.court_booking.AbstractIntegrationTest;
import com.marwix127.court_booking.bookings.BookingRequest;
import com.marwix127.court_booking.bookings.BookingService;
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.court.CourtNotFoundException;

@DisplayName("Calculo de disponibilidad")
class AvailabilityServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AvailabilityService availabilityService;
    @Autowired
    private BookingService bookingService;

    private Court court;
    private LocalDate date;

    @BeforeEach
    void setUpCourt() {
        court = givenCourt((short) 60);
        givenOpeningHours(court, LocalTime.of(9, 0), LocalTime.of(22, 0));
        date = futureDate();
    }

    @Test
    @DisplayName("un dia libre de 09:00 a 22:00 con tramos de 60 min da 13 huecos")
    void splitsOpeningHoursIntoSlots() {
        var response = availabilityService.findFreeSlots(court.getId(), date);

        assertThat(response.slots()).hasSize(13);
        assertThat(response.slotMinutes()).isEqualTo((short) 60);
        assertThat(response.slots().get(0).startsAt()).isEqualTo(at(date, 9, 0));
        // El ultimo tramo termina exactamente a la hora de cierre.
        assertThat(response.slots().get(12).endsAt()).isEqualTo(at(date, 22, 0));
    }

    @Test
    @DisplayName("una reserva existente retira su tramo y solo el suyo")
    void excludesBookedSlots() {
        var user = givenUser("ana@test.com");
        bookingService.create(
                new BookingRequest(court.getId(), at(date, 11, 0), at(date, 12, 0)), user);

        var slots = availabilityService.findFreeSlots(court.getId(), date).slots();

        assertThat(slots).hasSize(12);
        assertThat(slots).noneMatch(s -> s.startsAt().equals(at(date, 11, 0)));
        assertThat(slots).anyMatch(s -> s.startsAt().equals(at(date, 10, 0)));
        assertThat(slots).anyMatch(s -> s.startsAt().equals(at(date, 12, 0)));
    }

    @Test
    @DisplayName("un cierre retira todos los tramos que solapa")
    void excludesClosedPeriods() {
        givenClosure(court, at(date, 12, 0), at(date, 14, 0));

        var slots = availabilityService.findFreeSlots(court.getId(), date).slots();

        assertThat(slots).hasSize(11);
        assertThat(slots).noneMatch(s -> s.startsAt().equals(at(date, 12, 0)));
        assertThat(slots).noneMatch(s -> s.startsAt().equals(at(date, 13, 0)));
        // El tramo que empieza cuando acaba el cierre sigue disponible.
        assertThat(slots).anyMatch(s -> s.startsAt().equals(at(date, 14, 0)));
    }

    @Test
    @DisplayName("una reserva cancelada no ocupa tramo")
    void cancelledBookingsDoNotBlockSlots() {
        var user = givenUser("ana@test.com");
        var booking = bookingService.create(
                new BookingRequest(court.getId(), at(date, 11, 0), at(date, 12, 0)), user);
        bookingService.cancel(booking.getId(), user);

        var slots = availabilityService.findFreeSlots(court.getId(), date).slots();

        assertThat(slots).hasSize(13);
        assertThat(slots).anyMatch(s -> s.startsAt().equals(at(date, 11, 0)));
    }

    @Test
    @DisplayName("un dia sin horario de apertura no ofrece nada")
    void noSlotsOnADayWithoutOpeningHours() {
        openingHoursRepository.deleteAll();
        givenOpeningHours(court, date.getDayOfWeek().plus(1),
                LocalTime.of(9, 0), LocalTime.of(22, 0));

        var response = availabilityService.findFreeSlots(court.getId(), date);

        assertThat(response.slots()).isEmpty();
    }

    @Test
    @DisplayName("una pista dada de baja no ofrece nada")
    void noSlotsForAnInactiveCourt() {
        court.setActive(false);
        courtRepository.save(court);

        var response = availabilityService.findFreeSlots(court.getId(), date);

        assertThat(response.slots()).isEmpty();
    }

    @Test
    @DisplayName("una fecha pasada no ofrece nada, porque los tramos ya empezaron")
    void noSlotsInThePast() {
        var response = availabilityService.findFreeSlots(
                court.getId(), LocalDate.now(clubZone()).minusDays(1));

        assertThat(response.slots()).isEmpty();
    }

    @Test
    @DisplayName("hoy solo ofrece los tramos que aun no han empezado")
    void todayOnlyOffersFutureSlots() {
        var today = LocalDate.now(clubZone());

        var slots = availabilityService.findFreeSlots(court.getId(), today).slots();

        Instant now = Instant.now();
        assertThat(slots).allMatch(s -> !s.startsAt().isBefore(now));
    }

    @Test
    @DisplayName("el ultimo tramo se descarta si no cabe completo antes del cierre")
    void dropsTheTrailingSlotThatDoesNotFit() {
        // 09:00-22:00 son 13 horas: con tramos de 90 min caben 8 completos
        // (hasta las 21:00) y sobraria media hora.
        var wide = givenCourt("Padel 2", (short) 90);
        givenOpeningHours(wide, LocalTime.of(9, 0), LocalTime.of(22, 0));

        var slots = availabilityService.findFreeSlots(wide.getId(), date).slots();

        assertThat(slots).hasSize(8);
        assertThat(slots.get(7).endsAt()).isEqualTo(at(date, 21, 0));
    }

    @Test
    @DisplayName("una pista inexistente es un 404, no una lista vacia")
    void unknownCourtIsNotFound() {
        var missing = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertThrows(CourtNotFoundException.class,
                () -> availabilityService.findFreeSlots(missing, date));
    }

    @Test
    @DisplayName("el dia de la semana se resuelve en hora del club, no en UTC")
    void weekdayIsResolvedInClubTime() {
        // 00:30 de un dia en Madrid son las 22:30 del dia anterior en UTC. Si
        // el servicio derivase el dia del instante en vez de la fecha local,
        // cruzaria la consulta con el horario del dia equivocado.
        openingHoursRepository.deleteAll();
        givenOpeningHours(court, date.getDayOfWeek(), LocalTime.of(0, 0), LocalTime.of(2, 0));

        var slots = availabilityService.findFreeSlots(court.getId(), date).slots();

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).startsAt()).isEqualTo(at(date, 0, 0));
    }

}
