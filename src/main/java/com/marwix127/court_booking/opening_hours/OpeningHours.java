package com.marwix127.court_booking.opening_hours;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import com.marwix127.court_booking.court.Court;
import java.time.DayOfWeek;


@Entity
@Getter 
@Setter
@Table(name = "opening_hours")
public class OpeningHours {
    @Id 
    @UuidGenerator
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;
 
    private DayOfWeek dayOfWeek;

    private java.time.LocalTime opensAt;

    private java.time.LocalTime closesAt;
    
}
