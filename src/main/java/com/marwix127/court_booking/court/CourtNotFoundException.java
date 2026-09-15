package com.marwix127.court_booking.court;

import java.util.UUID;

import com.marwix127.court_booking.common.NotFoundException;

public class CourtNotFoundException extends NotFoundException {

    public CourtNotFoundException(UUID id) {
        super("Court not found: " + id);
    }
}
