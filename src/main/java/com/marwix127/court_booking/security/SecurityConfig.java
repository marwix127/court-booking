package com.marwix127.court_booking.security;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Secreto de firma. HMAC (clave simetrica) y no RSA porque esta misma
     * aplicacion emite y valida los tokens: no hay nadie mas que necesite
     * verificar la firma. Se pasaria a RSA si otro servicio tuviera que
     * validarlos sin poder emitirlos.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKeySpec signingKey() {
        return new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // API sin estado: no hay sesion ni cookie de sesion, asi que la
                // proteccion CSRF no aplica. Dejarla activa hace que todo
                // POST/PUT/DELETE responda 403 sin mensaje util.
                .csrf(csrf -> csrf.disable())

                // Nada de sesiones en servidor: cada peticion lleva su token.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Cuando una excepcion no manejada llega al contenedor,
                        // Spring Boot reenvia internamente a /error. Ese reenvio
                        // no debe exigir autenticacion: si no, cualquier error
                        // le llega a un cliente anonimo como 401 en vez de su
                        // codigo real (400, 500...). No abre ninguna ruta.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Autenticarse y registrarse son publicos por definicion.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // Consultar pistas y disponibilidad es publico.
                        .requestMatchers(HttpMethod.GET, "/api/courts", "/api/courts/**").permitAll()

                        // Cualquier otra ruta exige autenticacion. Regla por
                        // defecto restrictiva: al anadir endpoints quedan
                        // protegidos salvo que se abran a proposito.
                        .anyRequest().authenticated())

                // La validacion del token la hace la cadena de filtros: si es
                // invalido o ha caducado, la peticion no llega al controlador.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))

                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(signingKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Traduce el claim "roles" del token en autoridades de Spring Security.
     *
     * Por defecto el converter lee el claim "scope" y prefija "SCOPE_". Aqui
     * se cambian las dos cosas: claim "roles" y prefijo "ROLE_", que es lo que
     * espera hasRole(...).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * Necesario para el endpoint de login: es quien comprueba usuario y
     * contrasenya. Se construye explicitamente sobre el UserDetailsService y
     * el PasswordEncoder que ya existen, en lugar de depender de la
     * autoconfiguracion.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
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
