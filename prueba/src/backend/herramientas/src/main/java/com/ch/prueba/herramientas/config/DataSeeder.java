package com.ch.prueba.herramientas.config;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.domain.Herramienta;
import com.ch.prueba.herramientas.repository.HerramientaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carga inicial del catálogo de herramientas.
 *
 * <p>Cumple <strong>R01 CA-R01.8</strong> (doc 02): al arrancar el backend
 * con la tabla {@code herramientas} vacía, se insertan las 10 herramientas
 * de la tabla del doc 03 §5.3. Si ya hay datos (reinicios en modo file o
 * perfil {@code perf} con volcado de 200 herramientas) el seeder no hace
 * nada — es <strong>idempotente</strong>.</p>
 *
 * <p>No se ejecuta bajo el perfil {@code test} para no contaminar slices
 * como {@code @DataJpaTest} que tienen su propia preparación de datos
 * (D13 doc 01).</p>
 *
 * <p>Las 10 herramientas cubren 6 de las 7 categorías del enum
 * {@link Categoria} (todas menos — sin uso — la propia enum queda holgada
 * para catálogos futuros), haciendo útil el filtro por categoría de R01.</p>
 */
@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final HerramientaRepository herramientaRepository;

    public DataSeeder(HerramientaRepository herramientaRepository) {
        this.herramientaRepository = herramientaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existentes = herramientaRepository.count();
        if (existentes > 0) {
            log.info("Seed omitido: la tabla 'herramientas' ya contiene {} registro(s).", existentes);
            return;
        }

        List<Herramienta> catalogoSemilla = List.of(
            new Herramienta("Taladro percutor Bosch GSB 13 RE",      Categoria.PERFORACION),
            new Herramienta("Taladro atornillador Makita DF333D",    Categoria.PERFORACION),
            new Herramienta("Sierra circular Einhell TE-CS 190",     Categoria.CORTE),
            new Herramienta("Cizalla de palanca Stanley",            Categoria.CORTE),
            new Herramienta("Lijadora orbital Makita BO5041",        Categoria.LIJADO),
            new Herramienta("Lijadora de banda Bosch PBS 75 A",      Categoria.LIJADO),
            new Herramienta("Nivel láser Bosch GLL 2-10",            Categoria.MEDICION),
            new Herramienta("Escalera telescópica 3,8 m",            Categoria.ESCALERAS),
            new Herramienta("Desbrozadora Stihl FS 55",              Categoria.JARDIN),
            new Herramienta("Multiherramienta oscilante Dewalt",     Categoria.OTROS)
        );

        herramientaRepository.saveAll(catalogoSemilla);
        log.info("Seed completado: {} herramientas cargadas en el catálogo.", catalogoSemilla.size());
    }
}
