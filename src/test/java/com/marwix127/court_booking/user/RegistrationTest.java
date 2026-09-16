package com.marwix127.court_booking.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.marwix127.court_booking.AbstractIntegrationTest;

@DisplayName("Registro de usuarios")
class RegistrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "claveSegura99";

    @Autowired
    private AppUserService appUserService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("la contrasenya se guarda hasheada, nunca en claro")
    void storesThePasswordHashed() {
        var user = appUserService.register(
                new RegisterRequest("ana@test.com", PASSWORD, "Ana Garcia"));

        assertThat(user.getPassword())
                .as("el hash no puede ser la contrasenya")
                .isNotEqualTo(PASSWORD);
        assertThat(user.getPassword())
                .as("BCrypt identifica su formato con este prefijo")
                .startsWith("$2a$");
        assertThat(passwordEncoder.matches(PASSWORD, user.getPassword()))
                .as("el hash debe verificar la contrasenya original")
                .isTrue();
    }

    @Test
    @DisplayName("el rol y el estado los decide el servidor")
    void theServerDecidesRoleAndStatus() {
        // RegisterRequest no tiene campos role ni enabled: el cliente no puede
        // influir en ellos ni enviandolos en el JSON.
        var user = appUserService.register(
                new RegisterRequest("ana@test.com", PASSWORD, "Ana Garcia"));

        assertThat(user.getRole()).isEqualTo(AppUserRole.USER);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("un email repetido es un conflicto, no un 500")
    void rejectsADuplicateEmail() {
        appUserService.register(new RegisterRequest("ana@test.com", PASSWORD, "Ana"));

        var duplicate = new RegisterRequest("ana@test.com", PASSWORD, "Otra Ana");

        // La deteccion la hace uq_users_email, no una consulta previa: asi no
        // hay ventana entre comprobar y escribir.
        assertThrows(EmailAlreadyInUseException.class, () -> appUserService.register(duplicate));
        assertThat(appUserRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("dos usuarios con la misma contrasenya tienen hashes distintos")
    void equalPasswordsProduceDifferentHashes() {
        // BCrypt genera una sal aleatoria por hash. Si los dos coincidieran,
        // seria senyal de que se esta hasheando sin sal.
        var first = appUserService.register(new RegisterRequest("a@test.com", PASSWORD, "A"));
        var second = appUserService.register(new RegisterRequest("b@test.com", PASSWORD, "B"));

        assertThat(first.getPassword()).isNotEqualTo(second.getPassword());
    }

    @Test
    @DisplayName("el usuario registrado se puede buscar por email")
    void theRegisteredUserIsFoundByEmail() {
        appUserService.register(new RegisterRequest("ana@test.com", PASSWORD, "Ana Garcia"));

        // findByEmail es lo que usa AppUserDetailsService para autenticar.
        assertThat(appUserRepository.findByEmail("ana@test.com"))
                .isPresent()
                .get()
                .extracting(AppUser::getName)
                .isEqualTo("Ana Garcia");
    }
}
