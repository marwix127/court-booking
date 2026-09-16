package com.marwix127.court_booking.bookings;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marwix127.court_booking.closures.ClosureRepository;
import com.marwix127.court_booking.common.ConstraintViolations;
import com.marwix127.court_booking.common.UnprocessableException;
import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.court.CourtNotFoundException;
import com.marwix127.court_booking.court.CourtRepository;
import com.marwix127.court_booking.opening_hours.OpeningHours;
import com.marwix127.court_booking.opening_hours.OpeningHoursRepository;
import com.marwix127.court_booking.user.AppUser;
import com.marwix127.court_booking.user.AppUserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private final ClosureRepository closureRepository;

    /**
     * Zona horaria del club. Las reservas son instantes absolutos y los
     * horarios de apertura son horas locales: para cruzarlos hace falta una
     * zona explicita. Nunca ZoneId.systemDefault(), que cambia entre el
     * portatil de desarrollo y el contenedor de produccion.
     */
    @Value("${app.club.timezone}")
    private String clubTimezone;

    @Transactional
    public Booking create(BookingRequest request, AppUser user) {
        var court = courtRepository.findById(request.courtId())
                .orElseThrow(() -> new CourtNotFoundException(request.courtId()));

        validateRules(request, court);

        var booking = new Booking();
        booking.setCourt(court);
        booking.setUser(user);
        booking.setStartsAt(request.startsAt());
        booking.setEndsAt(request.endsAt());
        booking.setStatus(BookingStatus.CONFIRMED);

        try {
            // saveAndFlush, no save: fuerza el INSERT ahora, dentro del try.
            // Con save() Hibernate lo pospone al commit, que ocurre al salir
            // del metodo, y la violacion de booking_no_overlap saltaria fuera
            // de este catch.
            //
            // No se consulta antes si la franja esta libre: entre esa consulta
            // y el INSERT cabe otra peticion. La restriccion EXCLUDE de la
            // base de datos es la unica comprobacion sin ventana de carrera.
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.isViolationOf(ex, "booking_no_overlap")) {
                throw new BookingOverlapException();
            }
            throw ex;
        }
    }

    /**
     * Cancela una reserva. No borra la fila: pasa el estado a CANCELLED, con
     * lo que queda fuera del indice parcial de booking_no_overlap y la franja
     * vuelve a estar libre, pero el historico se conserva.
     *
     * Puede cancelar el dueño de la reserva o un administrador, y solo antes
     * de la hora de comienzo.
     *
     * Es idempotente: cancelar algo ya cancelado no cambia nada y no es un
     * error, el estado final pedido por el cliente es el que hay.
     */
    @Transactional
    public Booking cancel(UUID bookingId, AppUser actor) {
        // Con court y user cargados: la respuesta se construye fuera de esta
        // transaccion y ambos son LAZY.
        var booking = bookingRepository.findWithCourtAndUserById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        boolean owner = booking.getUser().getId().equals(actor.getId());
        boolean admin = actor.getRole() == AppUserRole.ADMIN;
        if (!owner && !admin) {
            throw new NotBookingOwnerException();
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        // El limite es la hora de comienzo, no la de fin: una reserva en curso
        // ya no se cancela.
        if (!booking.getStartsAt().isAfter(Instant.now())) {
            throw new BookingNotCancellableException();
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // saveAndFlush para que el UPDATE, y con el la comprobacion de @Version,
        // ocurra aqui y no al cerrar la transaccion. Si otra peticion cancelo
        // esta misma reserva entre la lectura y este punto, salta
        // OptimisticLockingFailureException y el handler la traduce a 409.
        return bookingRepository.saveAndFlush(booking);
    }

    /**
     * Reglas de negocio previas al INSERT. Todas producen 422: la peticion es
     * valida en forma, pero no se puede atender.
     */
    private void validateRules(BookingRequest request, Court court) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new UnprocessableException("endsAt must be after startsAt");
        }
        if (!court.isActive()) {
            throw new UnprocessableException("Court is not active: " + court.getName());
        }

        var zone = ZoneId.of(clubTimezone);
        var start = request.startsAt().atZone(zone);
        var end = request.endsAt().atZone(zone);

        validateDuration(request, court);
        validateWithinOpeningHours(court, start, end);
        validateNotClosed(request, court);
    }

    private void validateDuration(BookingRequest request, Court court) {
        var minutes = Duration.between(request.startsAt(), request.endsAt()).toMinutes();
        short slot = court.getSlotMinutes();

        if (minutes % slot != 0) {
            throw new UnprocessableException(
                    "Booking duration must be a multiple of " + slot + " minutes");
        }
    }

    private void validateWithinOpeningHours(Court court, ZonedDateTime start, ZonedDateTime end) {
        // El dia de la semana se calcula en hora del club, no en UTC: una
        // reserva a las 00:30 del viernes en Madrid son las 22:30 del jueves
        // en UTC, y cruzarla con el horario del jueves seria un error.
        var dayOfWeek = start.getDayOfWeek();

        OpeningHours hours = openingHoursRepository
                .findByCourtIdAndDayOfWeek(court.getId(), dayOfWeek)
                .orElseThrow(() -> new UnprocessableException(
                        "Court " + court.getName() + " is closed on " + dayOfWeek));

        // Una reserva que cruza la medianoche caeria en otro dia, con otro
        // horario: se rechaza en lugar de validarla contra el dia equivocado.
        if (!end.toLocalDate().equals(start.toLocalDate())) {
            throw new UnprocessableException("Booking must start and end on the same day");
        }

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        if (startTime.isBefore(hours.getOpensAt()) || endTime.isAfter(hours.getClosesAt())) {
            throw new UnprocessableException("Booking must be within opening hours ("
                    + hours.getOpensAt() + " - " + hours.getClosesAt() + ")");
        }
    }

    private void validateNotClosed(BookingRequest request, Court court) {
        boolean closed = closureRepository
                .existsByCourtIdAndStartsAtLessThanAndEndsAtGreaterThan(
                        court.getId(), request.endsAt(), request.startsAt());

        if (closed) {
            throw new UnprocessableException("Court is closed during the requested period");
        }
    }
}
