package com.marwix127.court_booking.user;

import java.util.UUID;

/**
 * Representacion publica de un usuario. Deliberadamente sin el hash de la
 * contrasenya: nunca debe salir de la aplicacion, ni siquiera hasheado.
 */
public record UserResponse(UUID id, String email, String fullName, AppUserRole role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
