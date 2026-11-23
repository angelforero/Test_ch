/**
 * Base URL del backend.
 *
 * Nota (D15 + decisión del commit 9): se deja relativo "/api" para
 * que en desarrollo entre por el proxy de Angular configurado en
 * proxy.conf.json (evita CORS y nos ahorra tocar el backend con
 * @CrossOrigin). En producción, el reverse proxy debería mapear
 * "/api/*" al backend, así que el mismo literal sirve en ambos
 * entornos sin cambios.
 *
 * Si en algún momento fuera necesario llamar a un backend remoto
 * directo desde el browser (sin proxy), aquí es donde se cambia
 * y nada más.
 */
export const API_BASE_URL = '/api';
