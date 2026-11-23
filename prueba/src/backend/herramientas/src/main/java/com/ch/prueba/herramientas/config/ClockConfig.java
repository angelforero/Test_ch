package com.ch.prueba.herramientas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuración del reloj del sistema.
 *
 * <p>Inyectar {@link Clock} en servicios en lugar de llamar
 * {@code LocalDate.now()} directamente permite a los tests unitarios
 * sustituirlo por un {@code Clock.fixed(…)} controlable, sin tener que
 * mockear métodos estáticos.</p>
 *
 * <p>Zona: {@code Europe/Madrid} (D3 doc 01). Toda la aplicación opera
 * sobre esta zona — nunca UTC a nivel de negocio.</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Europe/Madrid"));
    }
}
