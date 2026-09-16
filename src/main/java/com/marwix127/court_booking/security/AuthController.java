package com.marwix127.court_booking.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    /**
     * Comprueba las credenciales y devuelve un token.
     *
     * La verificacion no se hace aqui a mano: se delega en el
     * AuthenticationManager, que usa el mismo AppUserDetailsService y el mismo
     * PasswordEncoder que ya habia. Si fallan, lanza AuthenticationException y
     * el handler la traduce a 401.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        return tokenService.issue(authentication);
    }
}
