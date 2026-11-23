package com.ch.prueba.herramientas.service;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.domain.Herramienta;
import com.ch.prueba.herramientas.exceptions.reglas.DuracionExcesivaException;
import com.ch.prueba.herramientas.exceptions.reglas.FechaEnPasadoException;
import com.ch.prueba.herramientas.exceptions.reglas.RangoFechasInvalidoException;
import com.ch.prueba.herramientas.repository.HerramientaRepository;
import com.ch.prueba.herramientas.web.dto.HerramientaDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios de {@link ToolService} (R01, R02).
 *
 * <p>Uso obligatorio de mocks (D19): repositorio mockeado,
 * {@link Clock} fijo a {@code 2026-11-23} para que las reglas de
 * "fecha en el pasado" sean deterministas sin depender del reloj del CI.</p>
 */
@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    /** Fecha fija "hoy" para todos los tests. */
    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Mock
    private HerramientaRepository repo;

    private ToolService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                HOY.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant(),
                ZoneId.of("Europe/Madrid"));
        service = new ToolService(repo, clock);
    }

    // ---- listarCatalogo --------------------------------------------------

    @Test
    @DisplayName("listarCatalogo sin filtro devuelve todas las herramientas ordenadas por nombre")
    void listarCatalogo_sinFiltro_devuelveTodasOrdenadas() {
        Herramienta h1 = nueva(1L, "Alfa",   Categoria.PERFORACION);
        Herramienta h2 = nueva(2L, "Beta",   Categoria.CORTE);
        given(repo.findAllByOrderByNombreAsc()).willReturn(List.of(h1, h2));

        List<HerramientaDto> resultado = service.listarCatalogo(null);

        assertThat(resultado).extracting(HerramientaDto::nombre).containsExactly("Alfa", "Beta");
        verify(repo, never()).findByCategoriaOrderByNombreAsc(Categoria.PERFORACION);
    }

    @Test
    @DisplayName("listarCatalogo con filtro delega en findByCategoria")
    void listarCatalogo_conFiltro_delegaEnFindByCategoria() {
        given(repo.findByCategoriaOrderByNombreAsc(Categoria.LIJADO))
                .willReturn(List.of(nueva(5L, "Lijadora", Categoria.LIJADO)));

        List<HerramientaDto> resultado = service.listarCatalogo(Categoria.LIJADO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).categoria()).isEqualTo(Categoria.LIJADO);
        verify(repo, never()).findAllByOrderByNombreAsc();
    }

    // ---- listarDisponibles: reglas de validación (R02) -------------------

    @Test
    @DisplayName("listarDisponibles con rango invertido lanza RangoFechasInvalidoException y no toca el repo")
    void listarDisponibles_rangoInvertido_lanzaRangoFechasInvalido() {
        LocalDate inicio = HOY.plusDays(5);
        LocalDate fin    = HOY.plusDays(2);

        assertThatThrownBy(() -> service.listarDisponibles(inicio, fin, null))
                .isInstanceOf(RangoFechasInvalidoException.class);

        verify(repo, never()).findDisponibles(isNull(), eq(inicio), eq(fin));
    }

    @Test
    @DisplayName("listarDisponibles con fecha de inicio en el pasado lanza FechaEnPasadoException")
    void listarDisponibles_inicioEnPasado_lanzaFechaEnPasado() {
        LocalDate inicio = HOY.minusDays(1);
        LocalDate fin    = HOY.plusDays(1);

        assertThatThrownBy(() -> service.listarDisponibles(inicio, fin, null))
                .isInstanceOf(FechaEnPasadoException.class);
    }

    @Test
    @DisplayName("listarDisponibles con duración de 8 días lanza DuracionExcesivaException")
    void listarDisponibles_duracionOchoDias_lanzaDuracionExcesiva() {
        // inicio hoy, fin hoy+7 → duración = 8 días (ambos inclusivos)
        LocalDate inicio = HOY;
        LocalDate fin    = HOY.plusDays(7);

        assertThatThrownBy(() -> service.listarDisponibles(inicio, fin, null))
                .isInstanceOf(DuracionExcesivaException.class);
    }

    // ---- listarDisponibles: happy path ----------------------------------

    @Test
    @DisplayName("listarDisponibles con rango válido delega al repo y mapea el resultado")
    void listarDisponibles_rangoValido_delegaYMapea() {
        LocalDate inicio = HOY;
        LocalDate fin    = HOY.plusDays(6); // duración = 7, exactamente el máximo
        Herramienta h = nueva(42L, "Taladro", Categoria.PERFORACION);
        given(repo.findDisponibles(null, inicio, fin)).willReturn(List.of(h));

        List<HerramientaDto> resultado = service.listarDisponibles(inicio, fin, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(42L);
        assertThat(resultado.get(0).nombre()).isEqualTo("Taladro");
    }

    // ---- helpers ---------------------------------------------------------

    /**
     * Constructor reflexivo: {@link Herramienta} no expone setter de id
     * (es IDENTITY, lo asigna JPA). En tests necesitamos controlarlo.
     */
    private static Herramienta nueva(Long id, String nombre, Categoria categoria) {
        Herramienta h = new Herramienta(nombre, categoria);
        try {
            var f = Herramienta.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(h, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("No se pudo asignar id por reflexión en el test", e);
        }
        return h;
    }
}
