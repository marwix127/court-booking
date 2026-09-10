package com.marwix127.court_booking.court;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    // Inyeccion por constructor: @RequiredArgsConstructor de Lombok genera el
    // constructor con todos los campos final, y Spring lo usa para inyectar.
    // Preferible a @Autowired sobre el campo, porque deja la dependencia
    // explicita e inmutable.
    private final CourtRepository courtRepository;

    @GetMapping
    public List<Court> findAll() {
        return courtRepository.findAll();
    }

    @GetMapping("/{id}")
    public Court findById(@PathVariable UUID id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Court not found: " + id));
    }
}
