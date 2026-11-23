package com.ch.prueba.herramientas.web.dto.mapper;

import com.ch.prueba.herramientas.domain.Herramienta;
import com.ch.prueba.herramientas.web.dto.HerramientaDto;

import java.util.List;

/**
 * Conversiones {@code Herramienta} \u2194 {@code HerramientaDto}.
 *
 * <p>Clase {@code final} con métodos estáticos: no es un bean de Spring,
 * no requiere inyección y no tiene estado. Mantiene el código de mapeo
 * fuera de servicios y entidades.</p>
 *
 * <p>Si la cantidad de DTOs o la complejidad de las conversiones crece,
 * el sustituto natural sería MapStruct — pero YAGNI para esta prueba.</p>
 */
public final class HerramientaMapper {

    private HerramientaMapper() {
        // Utilidad: no instanciable.
    }

    public static HerramientaDto toDto(Herramienta entidad) {
        return new HerramientaDto(entidad.getId(), entidad.getNombre(), entidad.getCategoria());
    }

    public static List<HerramientaDto> toDtoList(List<Herramienta> entidades) {
        return entidades.stream().map(HerramientaMapper::toDto).toList();
    }
}
