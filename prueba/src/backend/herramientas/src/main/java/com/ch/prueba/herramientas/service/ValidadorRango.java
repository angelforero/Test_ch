package com.ch.prueba.herramientas.service;

import com.ch.prueba.herramientas.exceptions.reglas.DuracionExcesivaException;
import com.ch.prueba.herramientas.exceptions.reglas.FechaEnPasadoException;
import com.ch.prueba.herramientas.exceptions.reglas.RangoFechasInvalidoException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Utilidad compartida para validar los 3 requisitos de rango de fechas
 * (R02 doc 02):
 *
 * <ol>
 *   <li>{@code fechaFin >= fechaInicio} \u2014 orden coherente.</li>
 *   <li>{@code fechaInicio >= hoy} \u2014 no se admiten fechas pasadas.</li>
 *   <li>{@code duracion = fin - inicio + 1 <= maxDias} \u2014 límite de
 *       duración (R02 fija {@code maxDias = 7}).</li>
 * </ol>
 *
 * <p>Clase {@code final} con un único método estático. No es un bean
 * Spring \u2014 lógica pura, sin estado, sin dependencias externas. Reusada
 * tanto por {@code ToolService.listarDisponibles} como por
 * {@code ReservationService.crear} para no duplicar las 3 reglas.</p>
 *
 * <p>Cada violación lanza su propia subclase de
 * {@link com.ch.prueba.herramientas.exceptions.reglas.ReglaValidacionException}
 * que el {@code GlobalExceptionHandler} mapea a HTTP 422.</p>
 */
public final class ValidadorRango {

    private ValidadorRango() {
        // Utilidad: no instanciable.
    }

    public static void validar(LocalDate fechaInicio,
                               LocalDate fechaFin,
                               LocalDate hoy,
                               int maxDias) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new RangoFechasInvalidoException(fechaInicio, fechaFin);
        }
        if (fechaInicio.isBefore(hoy)) {
            throw new FechaEnPasadoException(fechaInicio, hoy);
        }
        int duracionDias = (int) ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
        if (duracionDias > maxDias) {
            throw new DuracionExcesivaException(duracionDias, maxDias);
        }
    }
}
