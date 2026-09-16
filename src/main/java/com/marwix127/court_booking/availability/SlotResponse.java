package com.marwix127.court_booking.availability;

import java.time.Instant;

/** Un tramo libre, en instantes absolutos: el cliente los muestra en su zona. */
public record SlotResponse(Instant startsAt, Instant endsAt) {
}
