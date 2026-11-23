/**
 * Slugs de los "type" del ProblemDetail emitidos por el backend
 * (doc 03 §6.bis + commit c16c948 del backend).
 *
 * El backend emite URLs completas tipo
 *   http://localhost:8080/problems/overlap-conflict
 * pero el frontend solo necesita el slug final (la parte después
 * de "/problems/") para decidir qué mensaje mostrar y qué campos
 * extras extraer.
 *
 * Este enum es la lista cerrada de los slugs que existen. Si el
 * backend añade uno nuevo, se añade aquí también.
 */
export enum ProblemType {
  OverlapConflict           = 'overlap-conflict',
  ActiveReservationLimit    = 'active-reservation-limit',
  CancellationNotAllowed    = 'cancellation-not-allowed',
  ToolNotFound              = 'tool-not-found',
  ReservationNotFound       = 'reservation-not-found',
  ValidationError           = 'validation-error',
  RuleViolation             = 'rule-violation',       // familia ReglaValidacionException
  MalformedRequest          = 'malformed-request',    // JSON inválido / tipo erróneo
  MissingParameter          = 'missing-parameter',    // query string faltante
  InternalError             = 'internal-error',       // 500 genérico
  Unknown                   = 'unknown',              // fallback cliente
}

/**
 * Extrae el slug final de una URL de ProblemDetail del backend.
 *
 *   problemTypeFromUrl('http://localhost:8080/problems/overlap-conflict')
 *     === 'overlap-conflict'
 *
 * Si la URL no sigue el patrón esperado, devuelve 'unknown' para
 * que el cliente pueda tratarlo genéricamente sin estallar.
 */
export function problemTypeFromUrl(typeUrl: string | undefined | null): ProblemType {
  if (!typeUrl) return ProblemType.Unknown;
  const marker = '/problems/';
  const idx = typeUrl.indexOf(marker);
  if (idx < 0) return ProblemType.Unknown;
  const slug = typeUrl.substring(idx + marker.length).replace(/[/?#].*$/, '');
  // Aceptamos solo los slugs conocidos; cualquier cosa rara → Unknown.
  const known = Object.values(ProblemType) as string[];
  return (known.includes(slug) ? slug : ProblemType.Unknown) as ProblemType;
}
