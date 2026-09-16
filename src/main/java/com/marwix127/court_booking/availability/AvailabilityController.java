package com.marwix127.court_booking.availability;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /**
     * Tramos libres de una pista en una fecha.
     * GET /api/courts/{courtId}/availability?date=2026-10-08
     *
     * Cuelga de la pista porque la disponibilidad no existe por si sola: es
     * siempre de una pista concreta.
     */
    @GetMapping("/api/courts/{courtId}/availability")
    public AvailabilityResponse availability(
            @PathVariable UUID courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return availabilityService.findFreeSlots(courtId, date);
    }
}
