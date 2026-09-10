package com.marwix127.court_booking.closures;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ClosureRepository extends JpaRepository<Closure, UUID> {
    
}
