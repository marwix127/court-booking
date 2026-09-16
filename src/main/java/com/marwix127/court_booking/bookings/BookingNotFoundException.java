package com.marwix127.court_booking.bookings;

import java.util.UUID;

import com.marwix127.court_booking.common.NotFoundException;

public class BookingNotFoundException extends NotFoundException {

    public BookingNotFoundException(UUID id) {
        super("Booking not found: " + id);
    }
}
