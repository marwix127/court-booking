package com.marwix127.court_booking.bookings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marwix127.court_booking.AbstractIntegrationTest;
import com.marwix127.court_booking.common.UnprocessableException;
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.court.CourtNotFoundException;
import com.marwix127.court_booking.user.AppUser;

/**
 * Reglas de negocio que se comprueban antes del INSERT. Todas terminan en 422:
 * la peticion es valida en forma, pero no se puede atender.
 *
 * El solape con otra reserva no se prueba aqui, porque no es una regla de
 * negocio sino una garantia de la base de datos: esta en BookingOverlapTest.
 */
@DisplayName("Reglas de una reserva")
class BookingRulesTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    private Court court;
    private AppUser user;
    private LocalDate date;

    @BeforeEach
    void setUpClub() {
        court = givenCourt((short) 60);
        givenOpeningHours(court, LocalTime.of(9, 0), LocalTime.of(22, 0));
        user = givenUser("ana@test.com");
        date = futureDate();
    }

    @Test
    @DisplayName("una franja dentro del horario y alineada al tramo se acepta")
    void acceptsAValidBooking() {
        var booking = bookingService.create(
                new BookingRequest(court.getId(), at(date, 10, 0), at(date, 11, 0)), user);

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getUser().getId()).isEqualTo(user.getId());
        // Las columnas con DEFAULT no se quedan a null: las rellena Hibernate.
        assertThat(booking.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("empezar antes de abrir se rechaza")
    void rejectsStartingBeforeOpening() {
        var tooEarly = new BookingRequest(court.getId(), at(date, 8, 0), at(date, 9, 0));

        var ex = assertThrows(UnprocessableException.class,
                () -> bookingService.create(tooEarly, user));
        assertThat(ex.getMessage()).contains("opening hours");
    }

    @Test
    @DisplayName("terminar despues de cerrar se rechaza")
    void rejectsEndingAfterClosing() {
        var tooLate = new BookingRequest(court.getId(), at(date, 22, 0), at(date, 23, 0));

        assertThrows(UnprocessableException.class, () -> bookingService.create(tooLate, user));
    }

    @Test
    @DisplayName("terminar justo a la hora de cierre se acepta")
    void acceptsEndingExactlyAtClosingTime() {
        var lastSlot = new BookingRequest(court.getId(), at(date, 21, 0), at(date, 22, 0));

        assertThat(bookingService.create(lastSlot, user).getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("una duracion que no es multiplo del tramo se rechaza")
    void rejectsDurationThatIsNotAMultipleOfTheSlot() {
        var ninetyMinutes = new BookingRequest(court.getId(), at(date, 10, 0), at(date, 11, 30));

        var ex = assertThrows(UnprocessableException.class,
                () -> bookingService.create(ninetyMinutes, user));
        assertThat(ex.getMessage()).contains("multiple of 60");
    }

    @Test
    @DisplayName("solapar con un cierre se rechaza")
    void rejectsOverlappingAClosure() {
        givenClosure(court, at(date, 12, 0), at(date, 14, 0));
        var duringClosure = new BookingRequest(court.getId(), at(date, 13, 0), at(date, 14, 0));

        var ex = assertThrows(UnprocessableException.class,
                () -> bookingService.create(duringClosure, user));
        assertThat(ex.getMessage()).contains("closed");
    }

    @Test
    @DisplayName("empezar justo cuando acaba un cierre se acepta")
    void acceptsStartingWhenAClosureEnds() {
        givenClosure(court, at(date, 12, 0), at(date, 14, 0));
        var afterClosure = new BookingRequest(court.getId(), at(date, 14, 0), at(date, 15, 0));

        assertThat(bookingService.create(afterClosure, user).getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("un dia sin horario de apertura se rechaza")
    void rejectsADayTheCourtIsClosed() {
        openingHoursRepository.deleteAll();
        givenOpeningHours(court, date.getDayOfWeek().plus(1),
                LocalTime.of(9, 0), LocalTime.of(22, 0));
        var closedDay = new BookingRequest(court.getId(), at(date, 10, 0), at(date, 11, 0));

        var ex = assertThrows(UnprocessableException.class,
                () -> bookingService.create(closedDay, user));
        assertThat(ex.getMessage()).contains("closed on");
    }

    @Test
    @DisplayName("una pista dada de baja se rechaza")
    void rejectsAnInactiveCourt() {
        court.setActive(false);
        courtRepository.save(court);
        var request = new BookingRequest(court.getId(), at(date, 10, 0), at(date, 11, 0));

        var ex = assertThrows(UnprocessableException.class,
                () -> bookingService.create(request, user));
        assertThat(ex.getMessage()).contains("not active");
    }

    @Test
    @DisplayName("el fin antes del inicio se rechaza")
    void rejectsAnInvertedInterval() {
        var inverted = new BookingRequest(court.getId(), at(date, 11, 0), at(date, 10, 0));

        assertThrows(UnprocessableException.class, () -> bookingService.create(inverted, user));
    }

    @Test
    @DisplayName("una franja que cruza la medianoche se rechaza")
    void rejectsABookingCrossingMidnight() {
        // opening_hours tiene una fila por dia, asi que una franja a caballo
        // entre dos dias tendria dos horarios aplicables. Se rechaza en lugar
        // de validarla contra el dia equivocado.
        openingHoursRepository.deleteAll();
        givenOpeningHours(court, LocalTime.of(0, 0), LocalTime.of(23, 59));
        var overMidnight = new BookingRequest(
                court.getId(), at(date, 23, 0), at(date.plusDays(1), 0, 0));

        var ex = assertThrows(UnprocessableException.class,
                () -> bookingService.create(overMidnight, user));
        assertThat(ex.getMessage()).contains("same day");
    }

    @Test
    @DisplayName("una pista inexistente es un 404")
    void rejectsAnUnknownCourt() {
        var missing = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var request = new BookingRequest(missing, at(date, 10, 0), at(date, 11, 0));

        assertThrows(CourtNotFoundException.class, () -> bookingService.create(request, user));
    }

    @Test
    @DisplayName("ninguna regla incumplida deja fila en la tabla")
    void rejectedBookingsLeaveNoRow() {
        var invalid = new BookingRequest(court.getId(), at(date, 8, 0), at(date, 9, 0));

        assertThrows(UnprocessableException.class, () -> bookingService.create(invalid, user));

        assertThat(bookingRepository.count()).isZero();
    }
}
