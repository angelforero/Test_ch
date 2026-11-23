package com.ch.prueba.herramientas.web.controller;

import com.ch.prueba.herramientas.service.ReservationService;
import com.ch.prueba.herramientas.web.dto.ReservaCreacionDto;
import com.ch.prueba.herramientas.web.dto.ReservaDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints REST de reservas (R03, R04, R05 \u2014 doc 03 §4.3, §4.4, §4.5).
 *
 * <p>Zero try/catch: todas las excepciones (de dominio y de framework)
 * las captura {@code GlobalExceptionHandler} (D7).</p>
 */
@RestController
@RequestMapping("/api/reservations")
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * POST /api/reservations \u2014 crea una reserva.
     *
     * <p>Devuelve <strong>201 Created</strong> con header {@code Location:
     * /api/reservations/{id}} y body = {@link ReservaDto}. Convención
     * REST pura: el frontend puede navegar al recurso recién creado sin
     * un GET extra.</p>
     */
    @PostMapping
    public ResponseEntity<ReservaDto> crear(
            @Valid @RequestBody ReservaCreacionDto dto,
            UriComponentsBuilder ucb) {
        ReservaDto creada = reservationService.crear(dto);
        URI location = ucb.path("/api/reservations/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /**
     * DELETE /api/reservations/{id} \u2014 cancela lógicamente la reserva.
     *
     * <p>Devuelve <strong>200 OK</strong> con el DTO actualizado (estado
     * = CANCELADA, {@code cancelada = true}) en lugar de 204 vacío, para
     * que el frontend pueda refrescar su vista sin segundo GET.</p>
     */
    @DeleteMapping("/{id}")
    public ReservaDto cancelar(@PathVariable Long id) {
        return reservationService.cancelar(id);
    }

    /**
     * GET /api/reservations?correo=... \u2014 lista reservas de un socio
     * (activas, finalizadas y canceladas), ordenadas de más reciente a
     * más antigua por {@code fechaInicio}.
     *
     * <p>El parámetro {@code correo} es <strong>obligatorio</strong>: no
     * tiene sentido listar todas las reservas del sistema desde un
     * frontend público. Se valida formato con {@code @Email}.</p>
     */
    @GetMapping
    public List<ReservaDto> listarPorCorreo(
            @RequestParam @NotBlank @Email String correo) {
        return reservationService.listarPorCorreo(correo);
    }
}
