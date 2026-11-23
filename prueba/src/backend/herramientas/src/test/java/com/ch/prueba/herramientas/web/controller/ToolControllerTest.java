package com.ch.prueba.herramientas.web.controller;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.exceptions.reglas.RangoFechasInvalidoException;
import com.ch.prueba.herramientas.service.ToolService;
import com.ch.prueba.herramientas.web.advice.GlobalExceptionHandler;
import com.ch.prueba.herramientas.web.dto.HerramientaDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test del {@link ToolController} (@WebMvcTest).
 *
 * <p>Verifica que:
 * <ul>
 *   <li>Los endpoints de R01 responden con estructura JSON correcta.</li>
 *   <li>Los parámetros se propagan correctamente al servicio (ArgumentCaptor).</li>
 *   <li>El {@link GlobalExceptionHandler} mapea las excepciones de dominio
 *       a {@code ProblemDetail} con los campos especificados en doc 03
 *       §6-bis. Esto es la validación de que la integración
 *       controlador↔advice funciona, no de la lógica del servicio en sí.</li>
 * </ul></p>
 *
 * <p>Se importa explícitamente el {@link GlobalExceptionHandler} porque
 * {@code @WebMvcTest(ToolController.class)} solo carga el controller y
 * sus dependencias directas \u2014 el advice no vendría por defecto.</p>
 */
@WebMvcTest(ToolController.class)
@Import(GlobalExceptionHandler.class)
class ToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToolService toolService;

    // ---- GET /api/tools --------------------------------------------------

    @Test
    @DisplayName("GET /api/tools devuelve 200 y la lista en JSON")
    void getTools_devuelve200YLista() throws Exception {
        given(toolService.listarCatalogo(isNull())).willReturn(List.of(
                new HerramientaDto(1L, "Alfa", Categoria.PERFORACION),
                new HerramientaDto(2L, "Beta", Categoria.CORTE)));

        mockMvc.perform(get("/api/tools"))
               .andExpect(status().isOk())
               .andExpect(content().contentType("application/json"))
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].nombre").value("Alfa"))
               .andExpect(jsonPath("$[1].categoria").value("CORTE"));
    }

    @Test
    @DisplayName("GET /api/tools?categoria=LIJADO propaga el filtro al servicio")
    void getTools_conCategoria_pasaFiltroAlServicio() throws Exception {
        given(toolService.listarCatalogo(any())).willReturn(List.of());

        mockMvc.perform(get("/api/tools").param("categoria", "LIJADO"))
               .andExpect(status().isOk());

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(toolService).listarCatalogo(captor.capture());
        assertThat(captor.getValue()).isEqualTo(Categoria.LIJADO);
    }

    // ---- GET /api/tools/available ----------------------------------------

    @Test
    @DisplayName("GET /api/tools/available con rango inválido devuelve 422 ProblemDetail validation-error")
    void getAvailable_rangoInvalido_devuelve422ProblemDetail() throws Exception {
        given(toolService.listarDisponibles(any(), any(), any()))
                .willThrow(new RangoFechasInvalidoException(
                        LocalDate.of(2026, 12, 5),
                        LocalDate.of(2026, 12, 1)));

        mockMvc.perform(get("/api/tools/available")
                        .param("fechaInicio", "2026-12-05")
                        .param("fechaFin", "2026-12-01"))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(content().contentType("application/problem+json"))
               .andExpect(jsonPath("$.type").value("http://localhost:8080/problems/validation-error"))
               .andExpect(jsonPath("$.title").value("Error de validación"))
               .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("GET /api/tools/available sin fechaInicio devuelve 400 ProblemDetail malformed-request")
    void getAvailable_sinFechaInicio_devuelve400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/tools/available")
                        .param("fechaFin", "2026-12-01"))
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType("application/problem+json"))
               .andExpect(jsonPath("$.type").value("http://localhost:8080/problems/malformed-request"))
               .andExpect(jsonPath("$.title").value("Petición malformada"))
               .andExpect(jsonPath("$.parametro").value("fechaInicio"));
    }

    @Test
    @DisplayName("GET /api/tools/available rango válido delega al servicio con los 3 parámetros")
    void getAvailable_rangoValido_delegaAlServicio() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 12, 1);
        LocalDate fin    = LocalDate.of(2026, 12, 3);
        given(toolService.listarDisponibles(eq(inicio), eq(fin), eq(Categoria.PERFORACION)))
                .willReturn(List.of(new HerramientaDto(1L, "Taladro", Categoria.PERFORACION)));

        mockMvc.perform(get("/api/tools/available")
                        .param("fechaInicio", "2026-12-01")
                        .param("fechaFin", "2026-12-03")
                        .param("categoria", "PERFORACION"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].nombre").value("Taladro"));

        verify(toolService).listarDisponibles(inicio, fin, Categoria.PERFORACION);
    }
}
