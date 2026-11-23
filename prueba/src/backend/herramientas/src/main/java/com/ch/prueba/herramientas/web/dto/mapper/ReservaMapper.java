package com.ch.prueba.herramientas.web.dto.mapper;

import com.ch.prueba.herramientas.domain.EstadoReservaCalculador;
import com.ch.prueba.herramientas.domain.Reserva;
import com.ch.prueba.herramientas.web.dto.ReservaDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Conversiones {@code Reserva} \u2192 {@code ReservaDto}.
 *
 * <p>No tiene estado, no es un bean. El "hoy" se recibe por parámetro
 * para que el estado derivado sea determinista bajo test (no llamamos
 * a {@code LocalDate.now()} aquí).</p>
 *
 * <p>Nunca mapeamos en sentido {@code ReservaDto \u2192 Reserva}: la entidad
 * la construye el servicio a partir del {@code ReservaCreacionDto}
 * resolviendo la {@code Herramienta} desde el repositorio \u2014 un mapper
 * no puede resolver esa dependencia.</p>
 */
public final class ReservaMapper {

    private ReservaMapper() {
        // Utilidad: no instanciable.
    }

    public static ReservaDto toDto(Reserva reserva, LocalDate hoy) {
        return new ReservaDto(
                reserva.getId(),
                reserva.getHerramienta().getId(),
                reserva.getHerramienta().getNombre(),
                reserva.getNombre(),
                reserva.getCorreo(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                EstadoReservaCalculador.calcular(reserva, hoy),
                reserva.isCancelada()
        );
    }

    public static List<ReservaDto> toDtoList(List<Reserva> reservas, LocalDate hoy) {
        return reservas.stream().map(r -> toDto(r, hoy)).toList();
    }
}
