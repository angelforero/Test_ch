import { ProblemType } from './problem-type';

/**
 * Error de dominio del frontend. Lo construye el
 * problem-detail.interceptor a partir del ProblemDetail del backend
 * (RFC 7807) o a partir de errores de red / errores inesperados.
 *
 * Los componentes reciben SIEMPRE un AppError (no un HttpErrorResponse
 * crudo). Eso les permite hacer switch(error.type) sobre el enum
 * ProblemType y mostrar mensajes específicos en español, o caer al
 * mensaje genérico "No se pudo contactar con el servidor".
 *
 * `extras` guarda las propiedades extra que vienen en el ProblemDetail
 * (p.ej. herramientaId, fechaInicio, fechaFin para overlap;
 * campo+mensaje+valorRechazado dentro de errors[] para validation).
 * Se tipa como `Record<string, unknown>` porque es dinámico.
 */
export interface ValidationFieldError {
  campo: string;
  mensaje: string;
  valorRechazado?: unknown;
}

export class AppError extends Error {
  constructor(
    /** Enum cerrado — el componente hace switch sobre esto. */
    public readonly type: ProblemType,
    /** Título corto en español, apto para header de un banner. */
    public readonly title: string,
    /** Detalle en español, apto para el cuerpo del mensaje. */
    public readonly detail: string,
    /** Código HTTP que devolvió el backend (0 si hubo error de red). */
    public readonly status: number,
    /** Propiedades extra del ProblemDetail (sin los campos estándar). */
    public readonly extras: Record<string, unknown> = {},
  ) {
    super(detail);
    this.name = 'AppError';
    // Preserva el prototype en targets ES5 (Angular 19 compila ES2022
    // pero no está de más por si alguien baja el target).
    Object.setPrototypeOf(this, AppError.prototype);
  }

  /**
   * Devuelve los errores de campo cuando type === ValidationError,
   * o un array vacío. Facilita el consumo desde formularios reactivos.
   */
  get fieldErrors(): ValidationFieldError[] {
    const arr = this.extras['errors'];
    return Array.isArray(arr) ? (arr as ValidationFieldError[]) : [];
  }
}
