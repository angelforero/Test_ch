package com.ch.prueba.herramientas.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests puros (sin mocks, sin Spring) de {@link EstadoReservaCalculador}.
 *
 * <p>Cada test cubre exactamente una rama del cálculo, incluido el caso
 * en que {@code cancelada} prevalece sobre un rango que ya pasó.</p>
 *
 * <p>Vive en paquete {@code domain/} \u2014 excluido del check de cobertura
 * \u2014 así que estos tests no cuentan contra el MAXIMUM 0.30.</p>
 */
class EstadoReservaCalculadorTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Test
    @DisplayName("Reserva con cancelada=true devuelve CANCELADA, incluso si las fechas ya pasaron")
    void cancelada_prevalece_sobre_fechas_pasadas() {
        Reserva r = nueva(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), true);
        assertThat(EstadoReservaCalculador.calcular(r, HOY)).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("Reserva cuyo fin es anterior a hoy devuelve FINALIZADA")
    void fin_antes_de_hoy_devuelve_FINALIZADA() {
        Reserva r = nueva(HOY.minusDays(5), HOY.minusDays(1), false);
        assertThat(EstadoReservaCalculador.calcular(r, HOY)).isEqualTo(EstadoReserva.FINALIZADA);
    }

    @Test
    @DisplayName("Reserva con hoy dentro del rango inclusivo devuelve EN_CURSO (incluye bordes)")
    void hoy_dentro_de_rango_devuelve_EN_CURSO() {
        // Caso borde: hoy == fechaInicio
        Reserva en_inicio = nueva(HOY, HOY.plusDays(3), false);
        assertThat(EstadoReservaCalculador.calcular(en_inicio, HOY)).isEqualTo(EstadoReserva.EN_CURSO);

        // Caso borde: hoy == fechaFin
        Reserva en_fin = nueva(HOY.minusDays(3), HOY, false);
        assertThat(EstadoReservaCalculador.calcular(en_fin, HOY)).isEqualTo(EstadoReserva.EN_CURSO);
    }

    @Test
    @DisplayName("Reserva cuyo inicio es futuro devuelve CONFIRMADA")
    void inicio_futuro_devuelve_CONFIRMADA() {
        Reserva r = nueva(HOY.plusDays(2), HOY.plusDays(4), false);
        assertThat(EstadoReservaCalculador.calcular(r, HOY)).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    // ---- helpers --------------------------------------------------------

    /**
     * Crea una {@link Reserva} con {@link Herramienta} dummy. Setea el
     * flag {@code cancelada} por reflexión (no hay setter público).
     */
    private static Reserva nueva(LocalDate inicio, LocalDate fin, boolean cancelada) {
        Herramienta h = new Herramienta("dummy", Categoria.OTROS);
        Reserva r = new Reserva(h, "Socio", "a@b.c", inicio, fin);
        if (cancelada) {
            try {
                Field f = Reserva.class.getDeclaredField("cancelada");
                f.setAccessible(true);
                f.setBoolean(r, true);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("No se pudo setear cancelada por reflexión", e);
            }
        }
        return r;
    }
}
