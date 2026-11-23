package com.ch.prueba.herramientas.web.controller;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.service.ToolService;
import com.ch.prueba.herramientas.web.dto.HerramientaDto;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints REST del catálogo de herramientas y consulta de disponibilidad
 * (R01, doc 03 §4.1 y §4.2).
 *
 * <p>Zero try/catch en este controlador: todas las excepciones (de dominio
 * y de framework) viajan al {@code GlobalExceptionHandler} para ser
 * mapeadas a {@code ProblemDetail} (D7).</p>
 *
 * <p>{@code @Validated} a nivel de clase habilita Bean Validation sobre
 * los {@code @RequestParam} (p. ej. {@code @NotNull}); las violaciones
 * las captura el handler como {@code ConstraintViolationException} \u2192 422.</p>
 */
@RestController
@RequestMapping("/api/tools")
@Validated
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    /**
     * GET /api/tools — catálogo completo, opcionalmente filtrado por
     * categoría. CA-R01.1 y CA-R01.5.
     */
    @GetMapping
    public List<HerramientaDto> listar(
            @RequestParam(required = false) Categoria categoria) {
        return toolService.listarCatalogo(categoria);
    }

    /**
     * GET /api/tools/available — herramientas sin reservas activas que
     * solapen el rango {@code [fechaInicio, fechaFin]}, opcionalmente
     * filtradas por categoría. CA-R01.6 y CA-R01.7.
     *
     * <p>Fechas obligatorias ({@code @NotNull}); si falta cualquiera de
     * las dos, Spring lanza {@code MissingServletRequestParameterException}
     * que el handler mapea a 400. El servicio aplica las reglas de rango
     * (R02) que generan 422.</p>
     */
    @GetMapping("/available")
    public List<HerramientaDto> listarDisponibles(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Categoria categoria) {
        return toolService.listarDisponibles(fechaInicio, fechaFin, categoria);
    }
}
