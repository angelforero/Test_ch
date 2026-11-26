package com.ch.prueba.herramientas.service;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.domain.Herramienta;
import com.ch.prueba.herramientas.domain.Reserva;
import com.ch.prueba.herramientas.exceptions.CancelacionNoPermitidaException;
import com.ch.prueba.herramientas.exceptions.CancelacionNoPermitidaException.Motivo;
import com.ch.prueba.herramientas.exceptions.HerramientaNoEncontradaException;
import com.ch.prueba.herramientas.exceptions.LimiteReservasActivasException;
import com.ch.prueba.herramientas.exceptions.ReservaNoEncontradaException;
import com.ch.prueba.herramientas.exceptions.SolapamientoReservaException;
import com.ch.prueba.herramientas.exceptions.reglas.RangoFechasInvalidoException;
import com.ch.prueba.herramientas.repository.HerramientaRepository;
import com.ch.prueba.herramientas.repository.ReservaRepository;
import com.ch.prueba.herramientas.web.dto.ReservaCreacionDto;
import com.ch.prueba.herramientas.web.dto.ReservaDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests unitarios de {@link ReservationService} (R02, R03, R04, R05).
 * Mockea ambos repositorios para aislar la lógica del servicio.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);
    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @Mock private ReservaRepository reservaRepository;
    @Mock private HerramientaRepository herramientaRepository;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(HOY.atStartOfDay(ZONA).toInstant(), ZONA);
        service = new ReservationService(reservaRepository, herramientaRepository, clock);
    }

    // ---- crear ----------------------------------------------------------

    @Test
    @DisplayName("crear ok devuelve ReservaDto con estado CONFIRMADA")
    void crear_ok_devuelveDtoConfirmada() {
        Herramienta h = herramientaConId(10L);
        given(herramientaRepository.findById(10L)).willReturn(Optional.of(h));
        given(reservaRepository.countActivasByCorreo("a@b.c", HOY)).willReturn(0L);
        given(reservaRepository.findSolapadas(anyLong(), any(), any())).willReturn(List.of());
        given(reservaRepository.save(any(Reserva.class))).willAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            setId(r, 1L);
            return r;
        });

        ReservaDto dto = service.crear(new ReservaCreacionDto(
                10L, "Ana", "a@b.c", HOY.plusDays(1), HOY.plusDays(3)));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.estado().name()).isEqualTo("CONFIRMADA");
        assertThat(dto.cancelada()).isFalse();
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("crear con herramienta inexistente lanza HerramientaNoEncontrada")
    void crear_herramientaInexistente_lanza() {
        given(herramientaRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new ReservaCreacionDto(
                999L, "Ana", "a@b.c", HOY.plusDays(1), HOY.plusDays(2))))
                .isInstanceOf(HerramientaNoEncontradaException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear con rango invertido lanza RangoFechasInvalido antes de tocar el repo")
    void crear_rangoInvertido_lanza() {
        assertThatThrownBy(() -> service.crear(new ReservaCreacionDto(
                10L, "Ana", "a@b.c", HOY.plusDays(5), HOY.plusDays(2))))
                .isInstanceOf(RangoFechasInvalidoException.class);

        verify(herramientaRepository, never()).findById(anyLong());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear con 3 reservas activas lanza LimiteReservasActivas y no busca solape")
    void crear_limiteActivas_lanza() {
        given(herramientaRepository.findById(10L)).willReturn(Optional.of(herramientaConId(10L)));
        given(reservaRepository.countActivasByCorreo("a@b.c", HOY)).willReturn(3L);

        assertThatThrownBy(() -> service.crear(new ReservaCreacionDto(
                10L, "Ana", "a@b.c", HOY.plusDays(1), HOY.plusDays(2))))
                .isInstanceOf(LimiteReservasActivasException.class);

        verify(reservaRepository, never()).findSolapadas(anyLong(), any(), any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CA-R02.9: crear persiste el correo normalizado (trim + minúsculas)")
    void crear_normalizaCorreo() {
        given(herramientaRepository.findById(10L)).willReturn(Optional.of(herramientaConId(10L)));
        given(reservaRepository.countActivasByCorreo("ada@example.com", HOY)).willReturn(0L);
        given(reservaRepository.findSolapadas(anyLong(), any(), any())).willReturn(List.of());
        given(reservaRepository.save(any(Reserva.class))).willAnswer(inv -> inv.getArgument(0));

        ReservaDto dto = service.crear(new ReservaCreacionDto(
                10L, "Ada", "  Ada@Example.COM  ", HOY.plusDays(1), HOY.plusDays(2)));

        assertThat(dto.correo()).isEqualTo("ada@example.com");
    }

    @Test
    @DisplayName("CA-R04.5: el límite de activas se cuenta con el correo normalizado")
    void crear_limiteActivas_correoConMayusculas_lanza() {
        given(herramientaRepository.findById(10L)).willReturn(Optional.of(herramientaConId(10L)));
        given(reservaRepository.countActivasByCorreo("ada@example.com", HOY)).willReturn(3L);

        assertThatThrownBy(() -> service.crear(new ReservaCreacionDto(
                10L, "Ada", "ADA@EXAMPLE.COM", HOY.plusDays(1), HOY.plusDays(2))))
                .isInstanceOf(LimiteReservasActivasException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarPorCorreo consulta con el correo normalizado")
    void listarPorCorreo_normalizaCorreo() {
        given(reservaRepository.findByCorreoOrderByFechaInicioDesc("ada@example.com"))
                .willReturn(List.of());

        assertThat(service.listarPorCorreo(" ADA@example.com ")).isEmpty();

        verify(reservaRepository).findByCorreoOrderByFechaInicioDesc("ada@example.com");
    }

    @Test
    @DisplayName("crear con solape lanza SolapamientoReserva")
    void crear_solape_lanza() {
        given(herramientaRepository.findById(10L)).willReturn(Optional.of(herramientaConId(10L)));
        given(reservaRepository.countActivasByCorreo(anyString(), any())).willReturn(0L);
        given(reservaRepository.findSolapadas(anyLong(), any(), any()))
                .willReturn(List.of(mock_reserva()));

        assertThatThrownBy(() -> service.crear(new ReservaCreacionDto(
                10L, "Ana", "a@b.c", HOY.plusDays(1), HOY.plusDays(2))))
                .isInstanceOf(SolapamientoReservaException.class);

        verify(reservaRepository, never()).save(any());
    }

    // ---- cancelar -------------------------------------------------------

    @Test
    @DisplayName("cancelar de reserva inexistente lanza ReservaNoEncontrada")
    void cancelar_inexistente_lanza() {
        given(reservaRepository.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelar(99L))
                .isInstanceOf(ReservaNoEncontradaException.class);
    }

    @Test
    @DisplayName("cancelar reserva ya cancelada lanza CancelacionNoPermitida con motivo YA_CANCELADA")
    void cancelar_yaCancelada_lanza() {
        Reserva r = reservaConFlag(HOY.plusDays(1), HOY.plusDays(3), true);
        setId(r, 7L);
        given(reservaRepository.findById(7L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancelar(7L))
                .isInstanceOf(CancelacionNoPermitidaException.class)
                .extracting(e -> ((CancelacionNoPermitidaException) e).getMotivo())
                .isEqualTo(Motivo.YA_CANCELADA);
    }

    @Test
    @DisplayName("cancelar reserva ya finalizada lanza CancelacionNoPermitida con motivo YA_FINALIZADA")
    void cancelar_yaFinalizada_lanza() {
        Reserva r = reservaConFlag(HOY.minusDays(5), HOY.minusDays(2), false);
        setId(r, 7L);
        given(reservaRepository.findById(7L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancelar(7L))
                .isInstanceOf(CancelacionNoPermitidaException.class)
                .extracting(e -> ((CancelacionNoPermitidaException) e).getMotivo())
                .isEqualTo(Motivo.YA_FINALIZADA);
    }

    @Test
    @DisplayName("cancelar reserva en curso lanza CancelacionNoPermitida con motivo YA_INICIADA")
    void cancelar_yaIniciada_lanza() {
        Reserva r = reservaConFlag(HOY.minusDays(1), HOY.plusDays(2), false);
        setId(r, 7L);
        given(reservaRepository.findById(7L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancelar(7L))
                .isInstanceOf(CancelacionNoPermitidaException.class)
                .extracting(e -> ((CancelacionNoPermitidaException) e).getMotivo())
                .isEqualTo(Motivo.YA_INICIADA);
    }

    @Test
    @DisplayName("cancelar reserva CONFIRMADA marca flag y devuelve DTO con estado CANCELADA")
    void cancelar_ok_marcaFlag() {
        Reserva r = reservaConFlag(HOY.plusDays(2), HOY.plusDays(4), false);
        setId(r, 7L);
        given(reservaRepository.findById(7L)).willReturn(Optional.of(r));
        given(reservaRepository.save(any(Reserva.class))).willAnswer(inv -> inv.getArgument(0));

        ReservaDto dto = service.cancelar(7L);

        assertThat(r.isCancelada()).isTrue();
        assertThat(dto.cancelada()).isTrue();
        assertThat(dto.estado().name()).isEqualTo("CANCELADA");
    }

    // ---- helpers --------------------------------------------------------

    private static Herramienta herramientaConId(Long id) {
        Herramienta h = new Herramienta("Dummy", Categoria.OTROS);
        setId(h, id);
        return h;
    }

    private static Reserva reservaConFlag(LocalDate inicio, LocalDate fin, boolean cancelada) {
        Reserva r = new Reserva(herramientaConId(10L), "Ana", "a@b.c", inicio, fin);
        if (cancelada) {
            try {
                Field f = Reserva.class.getDeclaredField("cancelada");
                f.setAccessible(true);
                f.setBoolean(r, true);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
        return r;
    }

    private static Reserva mock_reserva() {
        return new Reserva(herramientaConId(10L), "X", "x@y.z", HOY.plusDays(1), HOY.plusDays(2));
    }

    private static void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
