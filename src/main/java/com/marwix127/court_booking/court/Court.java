package com.marwix127.court_booking.court;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;


@Entity
@Getter 
@Setter 
@Table(name = "courts")
public class Court {

    @Id
    @UuidGenerator 
    private java.util.UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private CourtType courtType;

    private Short slotMinutes;

    private boolean active;

    @CreationTimestamp 
    private java.time.Instant createdAt;

}
