package com.marwix127.court_booking.court;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints de consulta de pistas.
 *
 * De momento devuelve la entidad Court directamente. Funciona porque Court no
 * tiene relaciones perezosas ni campos sensibles, pero es algo a sustituir por
 * un DTO en cuanto la respuesta deba diferir del modelo interno.
 */
@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtRepository courtRepository;

    @GetMapping
    public List<Court> findAll() {
        return courtRepository.findAll();
    }

    @GetMapping("/{id}")
    public Court findById(@PathVariable UUID id) {
        // El controlador solo dice "no existe"; el codigo HTTP lo decide
        // GlobalExceptionHandler. Aqui no hay nada de HTTP.
        return courtRepository.findById(id)
                .orElseThrow(() -> new CourtNotFoundException(id));
    }
}
