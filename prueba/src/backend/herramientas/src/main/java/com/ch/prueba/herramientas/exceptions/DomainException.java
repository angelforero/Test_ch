package com.ch.prueba.herramientas.exceptions;

/**
 * Raíz abstracta de todas las excepciones de dominio de la aplicación.
 *
 * <p>Doc 01 D7-bis. Diseño:
 * <ul>
 *   <li>Es {@link RuntimeException} — no checked — para no obligar a
 *       propagar {@code throws} por toda la capa de servicio.</li>
 *   <li>Es {@code abstract} para que nadie la instancie directamente:
 *       cada condición de error tiene su subclase con los datos
 *       necesarios para componer el mensaje al usuario.</li>
 *   <li><strong>No conoce HTTP</strong> — ni status, ni URIs, ni headers.
 *       La traducción a {@code ProblemDetail} ocurre exclusivamente en
 *       {@code GlobalExceptionHandler} (D7). Los servicios solo expresan
 *       intención de negocio.</li>
 * </ul></p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
