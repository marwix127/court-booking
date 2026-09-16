package com.marwix127.court_booking.bookings;

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
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.user.AppUser;
import com.marwix127.court_booking.user.AppUserRole;

@DisplayName("Cancelacion de una reserva")
class BookingCancellationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    private Court court;
    private AppUser owner;
    private AppUser stranger;
    private AppUser admin;
    private LocalDate date;

    @BeforeEach
    void setUpClub() {
        court = givenCourt((short) 60);
        givenOpeningHours(court, LocalTime.of(9, 0), LocalTime.of(22, 0));
        owner = givenUser("ana@test.com");
        stranger = givenUser("luis@test.com");
        admin = givenUser("admin@test.com", AppUserRole.ADMIN);
        date = futureDate();
    }

    @Test
    @DisplayName("la dueña puede cancelar")
    void theOwnerCanCancel() {
        var booking = givenBooking(owner, 10);

        var cancelled = bookingService.cancel(booking.getId(), owner);

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("un administrador puede cancelar una reserva ajena")
    void anAdminCanCancelSomeoneElsesBooking() {
        var booking = givenBooking(owner, 10);

        var cancelled = bookingService.cancel(booking.getId(), admin);

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("otro usuario cualquiera no puede")
    void aStrangerCannotCancel() {
        var booking = givenBooking(owner, 10);

        assertThrows(NotBookingOwnerException.class,
                () -> bookingService.cancel(booking.getId(), stranger));

        assertThat(bookingRepository.findById(booking.getId()))
                .get()
                .extracting(Booking::getStatus)
                .as("el intento fallido no cambia el estado")
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("cancelar dos veces no es un error")
    void cancellingTwiceIsIdempotent() {
        var booking = givenBooking(owner, 10);

        bookingService.cancel(booking.getId(), owner);
        var again = bookingService.cancel(booking.getId(), owner);

        assertThat(again.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("una reserva ya empezada no se puede cancelar")
    void cannotCancelABookingThatHasAlreadyStarted() {
        var booking = givenBooking(owner, 10);

        // La regla del club es "antes de la hora de comienzo". Se mueve la
        // franja al pasado en lugar de esperar, que es lo unico practico.
        booking.setStartsAt(Instant.now().minusSeconds(600));
        booking.setEndsAt(Instant.now().plusSeconds(3000));
        bookingRepository.saveAndFlush(booking);

        assertThrows(BookingNotCancellableException.class,
                () -> bookingService.cancel(booking.getId(), owner));
    }

    @Test
    @DisplayName("el limite es la hora de comienzo, no la de fin")
    void theDeadlineIsTheStartTimeNotTheEndTime() {
        var booking = givenBooking(owner, 10);

        // Empieza en un segundo y termina dentro de una hora: aun no ha
        // empezado, asi que todavia se puede cancelar.
        booking.setStartsAt(Instant.now().plusSeconds(1));
        booking.setEndsAt(Instant.now().plusSeconds(3600));
        bookingRepository.saveAndFlush(booking);

        assertThat(bookingService.cancel(booking.getId(), owner).getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelar deja la franja libre y conserva la fila")
    void cancellingFreesTheSlotAndKeepsTheRow() {
        var booking = givenBooking(owner, 10);
        bookingService.cancel(booking.getId(), owner);

        var reused = givenBooking(stranger, 10);

        assertThat(reused.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingRepository.count())
                .as("la cancelada se conserva como historico")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("una reserva inexistente es un 404")
    void unknownBookingIsNotFound() {
        var missing = UUID.fromString("11111111-1111-1111-1111-111111111111");

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.cancel(missing, owner));
    }

    private Booking givenBooking(AppUser user, int hour) {
        return bookingService.create(
                new BookingRequest(court.getId(), at(date, hour, 0), at(date, hour + 1, 0)),
                user);
    }
}
