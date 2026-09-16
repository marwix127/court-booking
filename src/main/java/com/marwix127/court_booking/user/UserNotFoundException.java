package com.marwix127.court_booking.user;

import com.marwix127.court_booking.common.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String email) {
        super("User not found: " + email);
    }
}
