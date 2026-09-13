package com.marwix127.court_booking.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marwix127.court_booking.common.ConstraintViolations;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppUser register(RegisterRequest request) {
        var user = new AppUser();
        user.setEmail(request.email());
        user.setName(request.fullName());

        // La contrasenya en claro solo existe aqui, en memoria, el tiempo que
        // tarda encode() en devolver el hash. Es lo unico que se persiste.
        user.setPassword(passwordEncoder.encode(request.password()));

        // Rol y estado los decide el servidor. Como RegisterRequest no tiene
        // estos campos, el cliente no puede influir en ellos.
        user.setRole(AppUserRole.USER);
        user.setEnabled(true);

        try {
            // saveAndFlush, no save: fuerza el INSERT ahora, dentro del try.
            // Con save() a secas Hibernate lo pospone hasta el commit, que
            // ocurre al salir del metodo, y la violacion del UNIQUE saltaria
            // fuera de este catch.
            return appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.isViolationOf(ex, "uq_users_email")) {
                throw new EmailAlreadyInUseException(request.email());
            }
            // Cualquier otra violacion no es "email duplicado": no la disfrazamos.
            throw ex;
        }
    }
}
