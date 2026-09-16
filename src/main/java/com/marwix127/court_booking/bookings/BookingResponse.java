package com.marwix127.court_booking.bookings;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID courtId,
        String courtName,
        UUID userId,
        Instant startsAt,
        Instant endsAt,
        BookingStatus status) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCourt().getId(),
                booking.getCourt().getName(),
                booking.getUser().getId(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.getStatus());
    }
}
