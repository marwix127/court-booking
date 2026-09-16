package com.marwix127.court_booking.security;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Emite los tokens de acceso.
 *
 * El subject es el email, que es el identificador con el que
 * AppUserDetailsService busca al usuario. Los roles van en un claim propio
 * "roles" y no en "scope", porque son roles de la aplicacion y no ambitos
 * OAuth2 concedidos a un cliente externo.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.ttl-minutes}")
    private long ttlMinutes;

    public LoginResponse issue(Authentication authentication) {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(ttlMinutes);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                // Solo los roles de la aplicacion. Spring Security anade
                // tambien autoridades del tipo FACTOR_PASSWORD, que describen
                // como se autentico el usuario y no son roles: si se colaran
                // aqui, al leer el token volverian como ROLE_FACTOR_PASSWORD.
                .filter(a -> a.startsWith(ROLE_PREFIX))
                // Se guardan sin el prefijo: lo vuelve a poner el
                // JwtGrantedAuthoritiesConverter al leer el token.
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("court-booking")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return LoginResponse.bearer(token, ttl.toSeconds());
    }
}
