package com.marwix127.court_booking.user;

import com.marwix127.court_booking.common.ConflictException;

public class EmailAlreadyInUseException extends ConflictException {

    public EmailAlreadyInUseException(String email) {
        super("Email already in use: " + email);
    }
}
