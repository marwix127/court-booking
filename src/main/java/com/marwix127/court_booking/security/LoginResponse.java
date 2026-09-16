package com.marwix127.court_booking.security;

/**
 * El token y cuantos segundos le quedan de vida. Se devuelve expiresIn para
 * que el cliente sepa cuando renovar sin tener que decodificar el JWT.
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse bearer(String token, long expiresInSeconds) {
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}
