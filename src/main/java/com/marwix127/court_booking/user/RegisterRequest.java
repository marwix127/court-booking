package com.marwix127.court_booking.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el cliente puede enviar al registrarse. Solo estos tres campos:
 * lo que no esta aqui (rol, enabled, id) no puede venir en la peticion, y
 * lo fija el servidor.
 */
public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email, @NotBlank @Size(min = 8, max = 72) String password, @NotBlank @Size(max = 120) String fullName) {

    

}
