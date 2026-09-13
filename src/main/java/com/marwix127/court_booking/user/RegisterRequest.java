package com.marwix127.court_booking.user;

/**
 * Datos que el cliente puede enviar al registrarse. Solo estos tres campos:
 * lo que no esta aqui (rol, enabled, id) no puede venir en la peticion, y
 * lo fija el servidor.
 */
public record RegisterRequest(String email, String password, String fullName) {
}
