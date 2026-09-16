package com.marwix127.court_booking.bookings;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
            Authentication authentication) {

        return BookingResponse.from(bookingService.create(request, currentUser(authentication)));
    }

    /**
     * Cancelar es una accion con nombre propio, no un PATCH del campo status.
     * Asi el cliente no puede llevar una reserva a un estado arbitrario: solo
     * puede pedir esta transicion concreta.
     */
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id, Authentication authentication) {
        return BookingResponse.from(bookingService.cancel(id, currentUser(authentication)));
    }

    /**
     * Recupera el AppUser real a partir de la identidad autenticada.
     *
     * Se usa Authentication.getName() y no @AuthenticationPrincipal porque el
     * principal cambia segun el mecanismo: un UserDetails con auth basica, un
     * Jwt con token. getName() devuelve el subject en los dos casos, asi que
     * el controlador no depende de como se autentico el cliente.
     */
    private AppUser currentUser(Authentication authentication) {
        return appUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(authentication.getName()));
    }
}
