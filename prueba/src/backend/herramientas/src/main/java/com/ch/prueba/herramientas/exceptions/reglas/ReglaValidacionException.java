package com.ch.prueba.herramientas.exceptions.reglas;

import com.ch.prueba.herramientas.exceptions.DomainException;

/**
 * Raíz abstracta para fallos de <strong>validación de reglas de negocio
 * sobre datos de entrada</strong> (R02 del doc 02): fechas en el pasado,
 * duración excesiva, rangos invertidos, etc.
 *
 * <p>Se distingue de las excepciones hermanas en {@code exceptions/} (que
 * expresan "entidad no existe" o "regla de estado del dominio"):
 * <ul>
 *   <li>Todas las subclases de {@code ReglaValidacionException} se mapean a
 *       <strong>HTTP 422</strong> (Unprocessable Entity) con type
 *       {@code validation-error}.</li>
 *   <li>El handler puede capturarlas con un único {@code @ExceptionHandler}
 *       sobre la clase base — añadir un subtipo nuevo no requiere
 *       actualizar el handler, solo documentarlo.</li>
 * </ul></p>
 */
public abstract class ReglaValidacionException extends DomainException {

    protected ReglaValidacionException(String message) {
        super(message);
    }
}
