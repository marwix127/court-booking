package com.marwix127.court_booking.user;

import lombok.Setter;
import lombok.Getter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;


@Entity
@Getter
@Setter 
@Table (name = "users")
public class AppUser {
    @Id
    @UuidGenerator 
    private java.util.UUID id;

    private String email;

    @Column (name = "password_hash")
    private String password;

    @Column (name = "full_name")
    private String name;

    @Enumerated (EnumType.STRING)
    private AppUserRole role;

    private boolean enabled;

    @CreationTimestamp 
    private java.time.Instant createdAt;

    
}
