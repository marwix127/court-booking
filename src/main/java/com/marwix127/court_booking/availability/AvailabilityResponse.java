package com.marwix127.court_booking.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tramos libres de una pista en un dia. Se devuelven tambien courtId, date y
 * slotMinutes para que la respuesta se entienda sin repetir la peticion, y
 * porque el tamanyo de tramo lo decide la pista, no quien pregunta.
 */
public record AvailabilityResponse(
        UUID courtId,
        LocalDate date,
        short slotMinutes,
        List<SlotResponse> slots) {
}
