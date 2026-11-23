package com.ch.prueba.herramientas.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Payload de entrada para crear una reserva (POST /api/reservations).
 *
 * <p>Doc 03 §4.3. Record inmutable con Bean Validation en los campos.
 * Las violaciones las captura {@code GlobalExceptionHandler} como
 * {@code MethodArgumentNotValidException} \u2192 422 con {@code errors[]}
 * detallado por campo.</p>
 *
 * <p>Las reglas <em>de negocio</em> adicionales sobre las fechas
 * (orden, pasado, duración \u2264 7) viven en
 * {@link com.ch.prueba.herramientas.service.ValidadorRango} y se evalúan
 * en el servicio. Aquí solo garantizamos que los valores llegan.</p>
 */
public record ReservaCreacionDto(

        @NotNull
        Long herramientaId,

        @NotBlank
        @Size(max = 120)
        String nombre,

        @NotBlank
        @Email
        @Size(max = 254)
        String correo,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin
) {
}
