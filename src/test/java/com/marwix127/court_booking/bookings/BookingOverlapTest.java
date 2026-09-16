package com.marwix127.court_booking.bookings;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marwix127.court_booking.AbstractIntegrationTest;
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.user.AppUser;

/**
 * Comprueba que dos usuarios no pueden reservar la misma pista a la misma hora,
 * ni siquiera intentandolo a la vez.
 *
 * Corre contra un Postgres real porque la garantia no esta en el codigo Java
 * sino en la restriccion EXCLUDE booking_no_overlap, que crea la migracion V1.
 * Con H2 estos tests pasarian sin comprobar absolutamente nada.
 *
 * Ningun metodo lleva @Transactional: cada hilo necesita su propia transaccion
 * real, y una transaccion de test envolviendolo todo falsearia el escenario.
 */
@DisplayName("Reservas solapadas")
class BookingOverlapTest extends AbstractIntegrationTest {

    private static final int CONCURRENT_ATTEMPTS = 8;

    @Autowired
    private BookingService bookingService;

    private Court court;
    private List<AppUser> users;
    private Instant slotStart;
    private Instant slotEnd;

    @BeforeEach
    void setUpClub() {
        court = givenCourt((short) 60);
        givenOpeningHours(court, LocalTime.of(9, 0), LocalTime.of(22, 0));

        users = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
            users.add(givenUser("user" + i + "@test.com"));
        }

        var date = futureDate();
        slotStart = at(date, 10, 0);
        slotEnd = at(date, 11, 0);
    }

    @Test
    @DisplayName("de ocho intentos simultaneos por la misma franja, solo uno se confirma")
    void onlyOneOfManySimultaneousAttemptsSucceeds() throws Exception {
        var request = new BookingRequest(court.getId(), slotStart, slotEnd);

        // Todos los hilos se bloquean en el latch y arrancan de golpe, para que
        // sus transacciones se solapen de verdad.
        var startSignal = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
        List<Future<Outcome>> futures = new ArrayList<>();

        try {
            for (AppUser user : users) {
                futures.add(pool.submit(attempt(request, user, startSignal)));
            }
            startSignal.countDown();

            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS))
                    .as("los hilos deben terminar sin quedarse bloqueados")
                    .isTrue();
        } finally {
            pool.shutdownNow();
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            // get() relanza cualquier excepcion inesperada: si un hilo fallo
            // por algo distinto del solape, el test debe romperse aqui y no
            // esconderlo como un conflicto mas.
            outcomes.add(future.get());
        }

        assertThat(outcomes)
                .as("exactamente un intento se confirma")
                .filteredOn(Outcome.CONFIRMED::equals)
                .hasSize(1);
        assertThat(outcomes)
                .as("el resto se rechaza por solape, no por otro error")
                .filteredOn(Outcome.OVERLAP::equals)
                .hasSize(CONCURRENT_ATTEMPTS - 1);

        // La comprobacion que de verdad importa: la base de datos no permitio
        // dos reservas activas sobre la misma franja.
        assertThat(confirmedBookingsInSlot())
                .as("solo una fila confirmada en la franja")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("dos reservas contiguas no se consideran solape")
    void adjacentBookingsDoNotOverlap() {
        bookingService.create(new BookingRequest(court.getId(), slotStart, slotEnd), users.get(0));

        // El rango del EXCLUDE es '[)': el fin es exclusivo, asi que 11:00-12:00
        // empieza justo donde acaba 10:00-11:00 y no choca.
        var next = new BookingRequest(court.getId(), slotEnd, slotEnd.plusSeconds(3600));
        var booking = bookingService.create(next, users.get(1));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("cancelar libera la franja para otra persona")
    void cancellingFreesTheSlot() {
        var request = new BookingRequest(court.getId(), slotStart, slotEnd);
        var first = bookingService.create(request, users.get(0));

        bookingService.cancel(first.getId(), users.get(0));

        // La restriccion es parcial (WHERE status <> 'CANCELLED'), asi que la
        // fila cancelada deja de ocupar la franja pero sigue en la tabla.
        var second = bookingService.create(request, users.get(1));

        assertThat(second.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmedBookingsInSlot()).isEqualTo(1);
        assertThat(bookingRepository.count())
                .as("la cancelada se conserva como historico")
                .isEqualTo(2);
    }

    private Callable<Outcome> attempt(BookingRequest request, AppUser user, CountDownLatch start) {
        return () -> {
            start.await();
            try {
                bookingService.create(request, user);
                return Outcome.CONFIRMED;
            } catch (BookingOverlapException ex) {
                return Outcome.OVERLAP;
            }
        };
    }

    private long confirmedBookingsInSlot() {
        return bookingRepository
                .findByCourtIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                        court.getId(), BookingStatus.CONFIRMED, slotEnd, slotStart)
                .size();
    }

    private enum Outcome {
        CONFIRMED, OVERLAP
    }
}
