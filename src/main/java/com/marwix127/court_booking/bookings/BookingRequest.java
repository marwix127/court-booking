package com.marwix127.court_booking.bookings;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/**
 * Peticion de reserva. No lleva userId a proposito: la identidad sale del
 * usuario autenticado, no del cuerpo de la peticion. Si viniera aqui,
 * cualquiera podria reservar a nombre de otro.
 */
public record BookingRequest(
        @NotNull UUID courtId,
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt) {
}
