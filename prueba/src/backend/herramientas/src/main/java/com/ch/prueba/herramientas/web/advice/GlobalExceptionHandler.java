package com.ch.prueba.herramientas.web.advice;

import com.ch.prueba.herramientas.exceptions.CancelacionNoPermitidaException;
import com.ch.prueba.herramientas.exceptions.HerramientaNoEncontradaException;
import com.ch.prueba.herramientas.exceptions.LimiteReservasActivasException;
import com.ch.prueba.herramientas.exceptions.ReservaNoEncontradaException;
import com.ch.prueba.herramientas.exceptions.SolapamientoReservaException;
import com.ch.prueba.herramientas.exceptions.reglas.ReglaValidacionException;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler centralizado de errores HTTP (D7 doc 01, §6-bis doc 03).
 *
 * <p>Convierte toda excepción propagada desde controladores en un
 * {@link ProblemDetail} RFC 7807 con {@code type} del catálogo cerrado
 * {@code http://localhost:8080/problems/&lt;slug&gt;}, {@code title} en
 * español (D18) y {@code detail} contextualizado a los datos reales del
 * error.</p>
 *
 * <p>Política de logging:
 * <ul>
 *   <li><strong>WARN</strong> para 4xx — errores operacionales esperables
 *       (regla de negocio, validación, recurso no encontrado). No requieren
 *       intervención; solo visibilidad.</li>
 *   <li><strong>ERROR</strong> para 5xx — bugs o fallos inesperados. Llevan
 *       {@code cause} para facilitar debugging sin filtrarlo al cliente
 *       (R08 doc 02).</li>
 * </ul></p>
 *
 * <p>Ningún controlador debe hacer {@code try/catch} para traducir errores
 * a HTTP — esa es exclusivamente responsabilidad de este handler.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Base URI del catálogo cerrado de tipos de problema.
     * Un solo punto de cambio si en producción el host cambia.
     */
    private static final String PROBLEM_TYPE_BASE = "http://localhost:8080/problems/";

    // ---------------------------------------------------------------------
    // 404 — Recursos no encontrados
    // ---------------------------------------------------------------------

    @ExceptionHandler(HerramientaNoEncontradaException.class)
    public ProblemDetail handleHerramientaNoEncontrada(HerramientaNoEncontradaException ex) {
        log.warn("Herramienta no encontrada: id={}", ex.getHerramientaId());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "No existe ninguna herramienta con id " + ex.getHerramientaId() + ".");
        problem.setType(problemType("not-found"));
        problem.setTitle("Herramienta no encontrada");
        problem.setProperty("herramientaId", ex.getHerramientaId());
        return problem;
    }

    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ProblemDetail handleReservaNoEncontrada(ReservaNoEncontradaException ex) {
        log.warn("Reserva no encontrada: id={}", ex.getReservaId());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "No existe ninguna reserva con id " + ex.getReservaId() + ".");
        problem.setType(problemType("not-found"));
        problem.setTitle("Reserva no encontrada");
        problem.setProperty("reservaId", ex.getReservaId());
        return problem;
    }

    // ---------------------------------------------------------------------
    // 422 — Validación de reglas de negocio sobre datos de entrada
    // ---------------------------------------------------------------------

    /**
     * Captura <strong>todas</strong> las subclases de
     * {@link ReglaValidacionException}: añadir un subtipo nuevo no requiere
     * tocar este handler.
     */
    @ExceptionHandler(ReglaValidacionException.class)
    public ProblemDetail handleReglaValidacion(ReglaValidacionException ex) {
        log.warn("Regla de validación incumplida: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage());
        problem.setType(problemType("validation-error"));
        problem.setTitle("Error de validación");
        return problem;
    }

    /**
     * Bean Validation sobre {@code @RequestBody} (p. ej. {@code @NotBlank},
     * {@code @Email}, {@code @NotNull} en el DTO de entrada).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("campo", fe.getField());
                    m.put("mensaje", fe.getDefaultMessage());
                    m.put("valorRechazado", fe.getRejectedValue());
                    return m;
                })
                .toList();
        log.warn("Bean Validation falló: {} error(es) de campo", errores.size());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Hay " + errores.size() + " error(es) de validación en los campos enviados.");
        problem.setType(problemType("validation-error"));
        problem.setTitle("Error de validación");
        problem.setProperty("errors", errores);
        return problem;
    }

    /**
     * Bean Validation sobre {@code @PathVariable} / {@code @RequestParam}
     * (p. ej. {@code @Min(1)} en un id).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errores = ex.getConstraintViolations().stream()
                .map(cv -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("campo", cv.getPropertyPath().toString());
                    m.put("mensaje", cv.getMessage());
                    m.put("valorRechazado", cv.getInvalidValue());
                    return m;
                })
                .toList();
        log.warn("Constraint violation: {} error(es)", errores.size());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Hay " + errores.size() + " error(es) de validación en los parámetros de la petición.");
        problem.setType(problemType("validation-error"));
        problem.setTitle("Error de validación");
        problem.setProperty("errors", errores);
        return problem;
    }

    // ---------------------------------------------------------------------
    // 400 — Petición malformada
    // ---------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Body de petición ilegible: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o no coincide con la estructura esperada.");
        problem.setType(problemType("malformed-request"));
        problem.setTitle("Petición malformada");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parámetro '{}' con tipo incorrecto: valor='{}'", ex.getName(), ex.getValue());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor que no se puede convertir al tipo esperado.");
        problem.setType(problemType("malformed-request"));
        problem.setTitle("Petición malformada");
        problem.setProperty("parametro", ex.getName());
        problem.setProperty("valorRechazado", String.valueOf(ex.getValue()));
        return problem;
    }

    // ---------------------------------------------------------------------
    // 409 — Conflictos de estado del dominio
    // ---------------------------------------------------------------------

    @ExceptionHandler(SolapamientoReservaException.class)
    public ProblemDetail handleSolapamiento(SolapamientoReservaException ex) {
        log.warn("Solapamiento de reserva: herramienta={}, rango=[{}, {}]",
                ex.getHerramientaId(), ex.getFechaInicio(), ex.getFechaFin());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Ya existe una reserva activa para esta herramienta que se solapa con el rango solicitado.");
        problem.setType(problemType("overlap-conflict"));
        problem.setTitle("Reserva solapada");
        problem.setProperty("herramientaId", ex.getHerramientaId());
        problem.setProperty("fechaInicio", ex.getFechaInicio().toString());
        problem.setProperty("fechaFin", ex.getFechaFin().toString());
        return problem;
    }

    @ExceptionHandler(LimiteReservasActivasException.class)
    public ProblemDetail handleLimiteActivas(LimiteReservasActivasException ex) {
        log.warn("Límite de reservas activas alcanzado: correo={}, actuales={}, máximo={}",
                ex.getCorreo(), ex.getActivasActuales(), ex.getMaximo());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "El correo '" + ex.getCorreo() + "' ya tiene " + ex.getActivasActuales()
                        + " reservas activas y el máximo permitido es " + ex.getMaximo() + ".");
        problem.setType(problemType("active-reservation-limit"));
        problem.setTitle("Límite de reservas activas alcanzado");
        problem.setProperty("correo", ex.getCorreo());
        problem.setProperty("activasActuales", ex.getActivasActuales());
        problem.setProperty("maximo", ex.getMaximo());
        return problem;
    }

    @ExceptionHandler(CancelacionNoPermitidaException.class)
    public ProblemDetail handleCancelacionNoPermitida(CancelacionNoPermitidaException ex) {
        log.warn("Cancelación no permitida: reserva={}, motivo={}", ex.getReservaId(), ex.getMotivo());
        String detalle = switch (ex.getMotivo()) {
            case YA_INICIADA   -> "No se puede cancelar: la reserva ya ha comenzado.";
            case YA_FINALIZADA -> "No se puede cancelar: la reserva ya ha finalizado.";
            case YA_CANCELADA  -> "No se puede cancelar: la reserva ya está cancelada.";
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detalle);
        problem.setType(problemType("cancellation-not-allowed"));
        problem.setTitle("Cancelación no permitida");
        problem.setProperty("reservaId", ex.getReservaId());
        problem.setProperty("motivo", ex.getMotivo().name());
        return problem;
    }

    // ---------------------------------------------------------------------
    // 500 — Catch-all para cualquier fallo inesperado
    // ---------------------------------------------------------------------

    /**
     * Última línea de defensa. Registra el stack trace completo en el log,
     * pero <strong>nunca</strong> lo expone al cliente (R08 doc 02).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado en el servidor. Inténtalo de nuevo más tarde.");
        problem.setType(problemType("internal-error"));
        problem.setTitle("Error interno del servidor");
        return problem;
    }

    // ---------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------

    private static URI problemType(String slug) {
        return URI.create(PROBLEM_TYPE_BASE + slug);
    }
}
