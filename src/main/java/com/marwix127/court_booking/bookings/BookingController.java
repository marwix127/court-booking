package com.marwix127.court_booking.bookings;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.marwix127.court_booking.user.AppUser;
import com.marwix127.court_booking.user.AppUserRepository;
import com.marwix127.court_booking.user.UserNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final AppUserRepository appUserRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        return BookingResponse.from(bookingService.create(request, currentUser(principal)));
    }

    /**
     * Cancelar es una accion con nombre propio, no un PATCH del campo status.
     * Asi el cliente no puede llevar una reserva a un estado arbitrario: solo
     * puede pedir esta transicion concreta.
     */
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {

        return BookingResponse.from(bookingService.cancel(id, currentUser(principal)));
    }

    /**
     * El UserDetails que construye AppUserDetailsService no es la entidad,
     * solo lleva email, hash y roles. Las operaciones necesitan el AppUser
     * real por la clave foranea y por el rol, asi que se recupera por email.
     */
    private AppUser currentUser(UserDetails principal) {
        return appUserRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new UserNotFoundException(principal.getUsername()));
    }
}
