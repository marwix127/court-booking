package com.marwix127.court_booking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // API sin estado: no hay sesion ni cookie de sesion, asi que la
                // proteccion CSRF no aplica. Dejarla activa hace que todo
                // POST/PUT/DELETE responda 403 sin mensaje util.
                .csrf(csrf -> csrf.disable())

                // Nada de sesiones en servidor: cada peticion se autentica sola.
                // Necesario para que JWT funcione limpio mas adelante.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Consultar pistas es publico.
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/courts", "/api/courts/**")
                        .permitAll()
                        // Cualquier otra ruta exige autenticacion. Regla por
                        // defecto restrictiva: al anadir endpoints quedan
                        // protegidos salvo que se abran a proposito.
                        .anyRequest().authenticated())

                // Autenticacion basica provisional, hasta que exista el login
                // con JWT. Permite probar los endpoints protegidos con curl.
                .httpBasic(Customizer.withDefaults())

                .build();
    }

    /**
     * Codificador de contrasenyas. Rellena la columna password_hash al
     * registrar usuarios y las verifica al autenticar. BCrypt incorpora la sal
     * en el propio hash, asi que no hace falta guardarla aparte.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
