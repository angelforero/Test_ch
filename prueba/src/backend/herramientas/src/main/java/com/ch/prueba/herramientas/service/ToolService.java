package com.ch.prueba.herramientas.service;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.repository.HerramientaRepository;
import com.ch.prueba.herramientas.web.dto.HerramientaDto;
import com.ch.prueba.herramientas.web.dto.mapper.HerramientaMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio del catálogo de herramientas (R01 doc 02).
 *
 * <p>Dos operaciones públicas:
 * <ul>
 *   <li>{@link #listarCatalogo(Categoria)} — listado estático con filtro
 *       opcional por categoría (CA-R01.1 y CA-R01.5).</li>
 *   <li>{@link #listarDisponibles(LocalDate, LocalDate, Categoria)} —
 *       disponibilidad por rango de fechas (CA-R01.6/7), con las 3 reglas
 *       de validación aplicadas antes de tocar la BD.</li>
 * </ul></p>
 *
 * <p>El {@link Clock} se inyecta para que los tests puedan controlar "hoy"
 * sin tener que mockear {@link LocalDate#now()}. En producción Spring Boot
 * provee un bean {@code Clock.systemDefaultZone()} si lo registramos, o
 * caemos al fallback {@code Clock.systemDefaultZone()} que creamos aquí si
 * no hay bean explícito. En este proyecto creamos el bean en
 * {@code ClockConfig} (siguiente iteración si fuese necesario); de momento
 * usamos el constructor por defecto que toma {@code Clock.systemDefaultZone()}.</p>
 */
@Service
public class ToolService {

    /** Duración máxima permitida para una reserva en días (R02). */
    public static final int MAX_DURACION_DIAS = 7;

    private final HerramientaRepository herramientaRepository;
    private final Clock clock;

    public ToolService(HerramientaRepository herramientaRepository, Clock clock) {
        this.herramientaRepository = herramientaRepository;
        this.clock = clock;
    }

    /**
     * Devuelve todas las herramientas del catálogo ordenadas por nombre.
     * Si {@code categoria} no es {@code null}, filtra por ella.
     *
     * <p>Transacción de solo lectura — Spring omite flush/commit.</p>
     */
    @Transactional(readOnly = true)
    public List<HerramientaDto> listarCatalogo(Categoria categoria) {
        List<com.ch.prueba.herramientas.domain.Herramienta> encontradas =
                (categoria == null)
                        ? herramientaRepository.findAllByOrderByNombreAsc()
                        : herramientaRepository.findByCategoriaOrderByNombreAsc(categoria);
        return HerramientaMapper.toDtoList(encontradas);
    }

    /**
     * Devuelve las herramientas disponibles en el rango
     * {@code [fechaInicio, fechaFin]} (inclusivo en ambos extremos).
     *
     * <p>Antes de consultar la BD valida las 3 reglas de rango
     * (R02): orden, duración ≤ {@value #MAX_DURACION_DIAS} días, y fecha
     * de inicio no anterior a hoy. Cada regla lanza su propia subclase de
     * {@code ReglaValidacionException} que el {@code GlobalExceptionHandler}
     * mapea a HTTP 422 con {@code type=validation-error}.</p>
     */
    @Transactional(readOnly = true)
    public List<HerramientaDto> listarDisponibles(LocalDate fechaInicio,
                                                  LocalDate fechaFin,
                                                  Categoria categoria) {
        ValidadorRango.validar(fechaInicio, fechaFin, LocalDate.now(clock), MAX_DURACION_DIAS);
        return HerramientaMapper.toDtoList(
                herramientaRepository.findDisponibles(categoria, fechaInicio, fechaFin));
    }
}
