package com.marwix127.court_booking.bookings;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;

import com.marwix127.court_booking.court.Court;
import com.marwix127.court_booking.user.AppUser;

@Entity 
@Getter 
@Setter 
@Table (name = "bookings")
public class Booking{
    @Id 
    @UuidGenerator 
    private java.util.UUID id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "court_id", nullable = false)
    private Court court;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "user_id", nullable = false)
    private AppUser user;

    private java.time.Instant startsAt;

    private java.time.Instant endsAt;

    @Enumerated (EnumType.STRING)
    private BookingStatus status;

    @Version 
    private long version;

    @CreationTimestamp 
    private java.time.Instant createdAt;

    @UpdateTimestamp  
    private java.time.Instant updatedAt;

}
