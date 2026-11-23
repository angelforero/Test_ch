package com.ch.prueba.herramientas.web.controller;

import com.ch.prueba.herramientas.domain.EstadoReserva;
import com.ch.prueba.herramientas.exceptions.CancelacionNoPermitidaException;
import com.ch.prueba.herramientas.exceptions.LimiteReservasActivasException;
import com.ch.prueba.herramientas.exceptions.SolapamientoReservaException;
import com.ch.prueba.herramientas.service.ReservationService;
import com.ch.prueba.herramientas.web.advice.GlobalExceptionHandler;
import com.ch.prueba.herramientas.web.dto.ReservaCreacionDto;
import com.ch.prueba.herramientas.web.dto.ReservaDto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test del {@link ReservationController}. Verifica que los
 * endpoints POST/DELETE funcionan y que el {@link GlobalExceptionHandler}
 * mapea las 3 excepciones de conflicto a {@code ProblemDetail} HTTP 409.
 */
@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired private MockMvc mockMvc;

    /**
     * {@link ObjectMapper} construido manualmente con soporte JSR-310
     * (LocalDate) — en Spring Boot 4.x el slice {@code @WebMvcTest} ya
     * no inyecta el ObjectMapper global por defecto, hay que crearlo aquí.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @MockitoBean
    private ReservationService reservationService;

    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIN    = LocalDate.of(2026, 12, 3);

    // ---- POST -----------------------------------------------------------

    @Test
    @DisplayName("POST happy path devuelve 201, header Location y DTO en body")
    void post_ok_devuelve201YLocation() throws Exception {
        ReservaCreacionDto body = new ReservaCreacionDto(10L, "Ana", "a@b.c", INICIO, FIN);
        ReservaDto creada = new ReservaDto(1L, 10L, "Taladro", "Ana", "a@b.c",
                INICIO, FIN, EstadoReserva.CONFIRMADA, false);
        given(reservationService.crear(any())).willReturn(creada);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated())
               .andExpect(header().string("Location", "http://localhost/api/reservations/1"))
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
               .andExpect(jsonPath("$.nombreHerramienta").value("Taladro"));
    }

    @Test
    @DisplayName("POST sin nombre devuelve 422 ProblemDetail validation-error con errors[]")
    void post_sinNombre_devuelve422() throws Exception {
        String json = """
            { "herramientaId": 10,
              "nombre": "",
              "correo": "a@b.c",
              "fechaInicio": "2026-12-01",
              "fechaFin":    "2026-12-03" }
            """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(content().contentType("application/problem+json"))
               .andExpect(jsonPath("$.type").value("http://localhost:8080/problems/validation-error"))
               .andExpect(jsonPath("$.errors[0].campo").value("nombre"));
    }

    @Test
    @DisplayName("POST con solapamiento devuelve 409 ProblemDetail overlap-conflict")
    void post_solape_devuelve409Overlap() throws Exception {
        ReservaCreacionDto body = new ReservaCreacionDto(10L, "Ana", "a@b.c", INICIO, FIN);
        given(reservationService.crear(any())).willThrow(
                new SolapamientoReservaException(10L, INICIO, FIN));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isConflict())
               .andExpect(content().contentType("application/problem+json"))
               .andExpect(jsonPath("$.type").value("http://localhost:8080/problems/overlap-conflict"))
               .andExpect(jsonPath("$.title").value("Reserva solapada"))
               .andExpect(jsonPath("$.herramientaId").value(10));
    }

    @Test
    @DisplayName("POST con l\u00edmite de activas devuelve 409 ProblemDetail active-reservation-limit")
    void post_limiteActivas_devuelve409Limit() throws Exception {
        ReservaCreacionDto body = new ReservaCreacionDto(10L, "Ana", "a@b.c", INICIO, FIN);
        given(reservationService.crear(any())).willThrow(
                new LimiteReservasActivasException("a@b.c", 3, 3));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.type").value("http://localhost:8080/problems/active-reservation-limit"))
               .andExpect(jsonPath("$.correo").value("a@b.c"))
               .andExpect(jsonPath("$.maximo").value(3));
    }

    // ---- DELETE ---------------------------------------------------------

    @Test
    @DisplayName("DELETE ok devuelve 200 con el DTO cancelado")
    void delete_ok_devuelve200() throws Exception {
        ReservaDto cancelada = new ReservaDto(7L, 10L, "Taladro", "Ana", "a@b.c",
                INICIO, FIN, EstadoReserva.CANCELADA, true);
        given(reservationService.cancelar(eq(7L))).willReturn(cancelada);

        mockMvc.perform(delete("/api/reservations/7"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(7))
               .andExpect(jsonPath("$.estado").value("CANCELADA"))
               .andExpect(jsonPath("$.cancelada").value(true));
    }

    @Test
    @DisplayName("DELETE de reserva ya cancelada devuelve 409 ProblemDetail cancellation-not-allowed")
    void delete_yaCancelada_devuelve409() throws Exception {
        given(reservationService.cancelar(eq(7L))).willThrow(
                new CancelacionNoPermitidaException(7L,
                        CancelacionNoPermitidaException.Motivo.YA_CANCELADA));

        mockMvc.perform(delete("/api/reservations/7"))
               .andExpect(status().isConflict())
               .andExpect(content().contentType("application/problem+json"))
               .andExpect(jsonPath("$.type").value("http://localhost:8080/problems/cancellation-not-allowed"))
               .andExpect(jsonPath("$.motivo").value("YA_CANCELADA"))
               .andExpect(jsonPath("$.reservaId").value(7));
    }
}
