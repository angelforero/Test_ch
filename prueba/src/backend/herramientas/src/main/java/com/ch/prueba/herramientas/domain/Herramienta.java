package com.ch.prueba.herramientas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Herramienta del catálogo prestable por la biblioteca comunitaria.
 *
 * <p>Doc 03 §5.1. El catálogo es estático en runtime (S3 doc 01): solo se
 * precarga vía {@code DataSeeder} al arrancar — no hay CRUD de catálogo
 * expuesto en la API MVP.</p>
 *
 * <p>Diseño:
 * <ul>
 *   <li>Constructor sin argumentos <em>protected</em> solo para JPA
 *       (Hibernate exige acceso sin-args pero preferimos no exponerlo al
 *       resto del código).</li>
 *   <li>Sin setters públicos: el objeto se crea completo y no se muta
 *       después, lo que evita estados inválidos.</li>
 *   <li>{@code equals}/{@code hashCode} basados en {@code id} siguiendo el
 *       patrón seguro para entidades JPA con identidad generada: dos
 *       entidades transitorias (ambas {@code id == null}) no son iguales
 *       salvo que sean la misma referencia.</li>
 * </ul></p>
 */
@Entity
@Table(name = "herramientas")
public class Herramienta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    /** Constructor requerido por JPA. No usar desde código de aplicación. */
    protected Herramienta() {
        // JPA only
    }

    /** Constructor de uso general (seeder, tests). */
    public Herramienta(String nombre, Categoria categoria) {
        this.nombre = nombre;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Herramienta other)) {
            return false;
        }
        // Dos entidades son iguales solo si ambas ya tienen id persistido.
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Hash estable independiente del estado persistente.
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        return "Herramienta{id=" + id + ", nombre='" + nombre + "', categoria=" + categoria + '}';
    }
}
