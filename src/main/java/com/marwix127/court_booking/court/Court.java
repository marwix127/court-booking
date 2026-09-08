package com.marwix127.court_booking.court;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
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

    @Column (name = "court_type")
    private @Enumerated(EnumType.STRING) CourtType courtType;

    @Column (name = "slot_minutes")
    private Short slotMinutes;

    private boolean active;

    @Column (name = "created_at")
    @CreationTimestamp 
    private java.time.Instant createdAt;

}
