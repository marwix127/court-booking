package com.marwix127.court_booking.opening_hours;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, UUID> {
    
    
}
