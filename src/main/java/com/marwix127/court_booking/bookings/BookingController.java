package com.marwix127.court_booking.bookings;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

        // El UserDetails que construye AppUserDetailsService no es la entidad,
        // solo lleva email, hash y roles. La reserva necesita el AppUser real
        // por la clave foranea, asi que se recupera por email.
        var user = appUserRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new UserNotFoundException(principal.getUsername()));

        return BookingResponse.from(bookingService.create(request, user));
    }
}
