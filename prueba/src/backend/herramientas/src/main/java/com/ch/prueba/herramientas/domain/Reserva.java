package com.ch.prueba.herramientas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reserva de una herramienta por un socio durante un rango de fechas.
 *
 * <p>Doc 03 §5.2. Reglas clave del dominio (R02–R05 doc 02):
 * <ul>
 *   <li>El rango {@code [fechaInicio, fechaFin]} es <strong>inclusivo</strong>
 *       en ambos extremos. Duración = fin − inicio + 1 (máx 7, R02).</li>
 *   <li>Horaria: solo {@code LocalDate} (sin horas), zona Europe/Madrid (D3).</li>
 *   <li>El estado (CONFIRMADA / EN_CURSO / FINALIZADA / CANCELADA) es
 *       <strong>derivado</strong> por {@code EstadoReservaCalculador} a partir
 *       de las fechas y del flag {@code cancelada}. Solo persistimos el
 *       booleano — nunca el enum derivado.</li>
 * </ul></p>
 *
 * <p>Índices (R09 CA-R09.2 — consulta de disponibilidad &lt; 500 ms con 5 000
 * reservas):
 * <ul>
 *   <li>{@code idx_reservas_herramienta} — filtro directo por herramienta.</li>
 *   <li>{@code idx_reservas_rango} (compuesto) — barrido del solapamiento:
 *       {@code herramienta_id} + ventana temporal.</li>
 *   <li>{@code idx_reservas_correo} — búsqueda de reservas activas por
 *       socio para R04 (límite de 3 activas).</li>
 * </ul></p>
 *
 * <p>No se añade un {@code CHECK (fecha_fin >= fecha_inicio)} a nivel de
 * tabla: Hibernate 6 lo soporta pero la combinación H2 (MODE=PostgreSQL) +
 * {@code @Check} suele dar fricción. La validación de orden la hará el
 * servicio (regla {@code RangoFechasInvalidoException}).</p>
 */
@Entity
@Table(
    name = "reservas",
    indexes = {
        @Index(name = "idx_reservas_herramienta", columnList = "herramienta_id"),
        @Index(name = "idx_reservas_rango",       columnList = "herramienta_id,fecha_inicio,fecha_fin"),
        @Index(name = "idx_reservas_correo",      columnList = "correo")
    }
)
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "herramienta_id", nullable = false)
    private Herramienta herramienta;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String correo;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Flag de cancelación lógica. Las reservas no se borran — se marcan.
     * Arranca en {@code false} por defecto (tipo primitivo).
     */
    @Column(nullable = false)
    private boolean cancelada;

    /** Constructor requerido por JPA. No usar desde código de aplicación. */
    protected Reserva() {
        // JPA only
    }

    /**
     * Constructor de uso general. {@code cancelada} arranca en {@code false}.
     * La validación de orden de fechas y duración máxima la realiza el
     * servicio antes de construir la instancia.
     */
    public Reserva(Herramienta herramienta,
                   String nombre,
                   String correo,
                   LocalDate fechaInicio,
                   LocalDate fechaFin) {
        this.herramienta = herramienta;
        this.nombre = nombre;
        this.correo = correo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.cancelada = false;
    }

    public Long getId() {
        return id;
    }

    public Herramienta getHerramienta() {
        return herramienta;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    /**
     * Marca la reserva como cancelada. La <strong>precondición</strong> de
     * si se puede cancelar (no iniciada, no finalizada, no ya cancelada —
     * R05) la verifica el servicio antes de invocar este método; aquí solo
     * se persiste el flag.
     */
    public void cancelar() {
        this.cancelada = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reserva other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        // Evita incluir el objeto Herramienta completo (LAZY, puede disparar carga).
        Long herramientaId = (herramienta != null ? herramienta.getId() : null);
        return "Reserva{id=" + id
                + ", herramientaId=" + herramientaId
                + ", correo='" + correo + "'"
                + ", rango=[" + fechaInicio + ", " + fechaFin + "]"
                + ", cancelada=" + cancelada + '}';
    }
}
