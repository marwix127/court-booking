package com.marwix127.court_booking;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.marwix127.court_booking.bookings.BookingRepository;
import com.marwix127.court_booking.closures.Closure;
import com.marwix127.court_booking.closures.ClosureRepository;
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.court.CourtRepository;
import com.marwix127.court_booking.court.CourtType;
import com.marwix127.court_booking.opening_hours.OpeningHours;
import com.marwix127.court_booking.opening_hours.OpeningHoursRepository;
import com.marwix127.court_booking.user.AppUser;
import com.marwix127.court_booking.user.AppUserRepository;
import com.marwix127.court_booking.user.AppUserRole;

/**
 * Base de los tests de integracion.
 *
 * Todas las clases heredan las mismas anotaciones, y por eso Spring reutiliza
 * un unico contexto y un unico contenedor de Postgres para toda la suite. Si
 * cada clase declarara su propia combinacion, cada una levantaria su contenedor
 * y la suite tardaria varias veces mas.
 *
 * Aparte, reune los metodos de montaje de datos para que cada test solo
 * describa lo que le diferencia.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @Autowired
    protected BookingRepository bookingRepository;
    @Autowired
    protected ClosureRepository closureRepository;
    @Autowired
    protected OpeningHoursRepository openingHoursRepository;
    @Autowired
    protected CourtRepository courtRepository;
    @Autowired
    protected AppUserRepository appUserRepository;

    @Value("${app.club.timezone}")
    protected String clubTimezone;

    /**
     * Antes y despues de cada test. El contexto es compartido, asi que la base
     * de datos tambien: sin esto, el orden de ejecucion cambiaria resultados.
     */
    @BeforeEach
    @AfterEach
    protected void cleanDatabase() {
        bookingRepository.deleteAll();
        closureRepository.deleteAll();
        openingHoursRepository.deleteAll();
        courtRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    protected ZoneId clubZone() {
        return ZoneId.of(clubTimezone);
    }

    /** Fecha futura relativa a hoy, para que los tests no caduquen con el tiempo. */
    protected LocalDate futureDate() {
        return LocalDate.now(clubZone()).plusDays(30);
    }

    /** Convierte una hora local del club en instante, igual que hace el servicio. */
    protected Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(clubZone()).toInstant();
    }

    protected Court givenCourt(short slotMinutes) {
        return givenCourt("Padel 1", slotMinutes);
    }

    /** El nombre importa: courts tiene un UNIQUE sobre name. */
    protected Court givenCourt(String name, short slotMinutes) {
        var court = new Court();
        court.setName(name);
        court.setCourtType(CourtType.PADEL);
        court.setSlotMinutes(slotMinutes);
        court.setActive(true);
        return courtRepository.save(court);
    }

    /** Mismo horario los siete dias, para que la franja elegida valga cualquier dia. */
    protected void givenOpeningHours(Court court, LocalTime opensAt, LocalTime closesAt) {
        for (DayOfWeek day : DayOfWeek.values()) {
            givenOpeningHours(court, day, opensAt, closesAt);
        }
    }

    protected void givenOpeningHours(Court court, DayOfWeek day, LocalTime opensAt,
            LocalTime closesAt) {

        var hours = new OpeningHours();
        hours.setCourt(court);
        hours.setDayOfWeek(day);
        hours.setOpensAt(opensAt);
        hours.setClosesAt(closesAt);
        openingHoursRepository.save(hours);
    }

    protected Closure givenClosure(Court court, Instant startsAt, Instant endsAt) {
        var closure = new Closure();
        closure.setCourt(court);
        closure.setStartsAt(startsAt);
        closure.setEndsAt(endsAt);
        closure.setReason("Mantenimiento");
        return closureRepository.save(closure);
    }

    protected AppUser givenUser(String email) {
        return givenUser(email, AppUserRole.USER);
    }

    protected AppUser givenUser(String email, AppUserRole role) {
        var user = new AppUser();
        user.setEmail(email);
        user.setName(email);
        // Hash irrelevante: estos tests no pasan por la autenticacion.
        user.setPassword("{noop}not-used-here");
        user.setRole(role);
        user.setEnabled(true);
        return appUserRepository.save(user);
    }
}
