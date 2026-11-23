import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { AppError } from '../errors/app-error';
import { ProblemType, problemTypeFromUrl } from '../errors/problem-type';

/**
 * Interceptor funcional (API moderna de Angular 19). Convierte
 * cualquier error HTTP en un {@link AppError} y lo re-lanza. De ese
 * modo los componentes NO ven HttpErrorResponse crudo, solo
 * AppError con `type`, `title`, `detail` y `extras` ya normalizados
 * desde el ProblemDetail del backend (doc 03 §6.bis).
 *
 * Tres caminos de error:
 *
 *  1. Respuesta HTTP con body = ProblemDetail JSON (caso feliz):
 *     extrae type/title/detail/status y todas las propiedades extra.
 *
 *  2. Respuesta HTTP sin ProblemDetail o con body != JSON (p.ej. 502
 *     de un proxy intermedio con HTML): se construye un AppError con
 *     type=InternalError (si status>=500) o Unknown, usando
 *     statusText como fallback.
 *
 *  3. Error de red / timeout (status===0): type=Unknown, mensaje
 *     "No se pudo contactar con el servidor".
 *
 * Nunca swallowea el error: siempre devuelve throwError(() => appError)
 * para que subscriptores y async pipe puedan reaccionar.
 */
export const problemDetailInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((err: unknown) => {
      const appError = toAppError(err);
      return throwError(() => appError);
    }),
  );
};

/** Expuesta para test unitario directo (no es parte de la API pública). */
export function toAppError(err: unknown): AppError {
  // Camino 3: error de red
  if (err instanceof HttpErrorResponse && err.status === 0) {
    return new AppError(
      ProblemType.Unknown,
      'Sin conexión',
      'No se pudo contactar con el servidor. Comprueba tu conexión e inténtalo de nuevo.',
      0,
    );
  }

  if (err instanceof HttpErrorResponse) {
    const body = err.error;

    // Camino 1: ProblemDetail JSON
    if (isProblemDetail(body)) {
      const { type, title, detail, status, ...extras } = body;
      return new AppError(
        problemTypeFromUrl(type),
        typeof title === 'string' ? title : 'Error',
        typeof detail === 'string' ? detail : (err.statusText || 'Error'),
        typeof status === 'number' ? status : err.status,
        extras as Record<string, unknown>,
      );
    }

    // Camino 2: HTTP error sin ProblemDetail
    return new AppError(
      err.status >= 500 ? ProblemType.InternalError : ProblemType.Unknown,
      'Error',
      err.statusText || `HTTP ${err.status}`,
      err.status,
    );
  }

  // Cualquier otra cosa inesperada (p.ej. un throw dentro de un tap)
  return new AppError(
    ProblemType.Unknown,
    'Error inesperado',
    err instanceof Error ? err.message : String(err),
    0,
  );
}

/**
 * Heurística simple: ¿el body se parece a un ProblemDetail RFC 7807?
 * No exigimos todos los campos — el backend de Spring los omite si
 * están vacíos. Basta con que sea un objeto plano con al menos
 * title o detail.
 */
function isProblemDetail(body: unknown): body is {
  type?: string;
  title?: string;
  detail?: string;
  status?: number;
  [k: string]: unknown;
} {
  if (body === null || typeof body !== 'object' || Array.isArray(body)) {
    return false;
  }
  const b = body as Record<string, unknown>;
  return typeof b['title'] === 'string' || typeof b['detail'] === 'string';
}
