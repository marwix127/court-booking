package com.marwix127.court_booking.closures;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;

import com.marwix127.court_booking.court.Court;

@Entity 
@Getter 
@Setter 
@Table (name = "closures")
public class Closure {
    @Id 
    @UuidGenerator 
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    private String reason;

    private java.time.Instant startsAt;
 
    private java.time.Instant endsAt;

}
