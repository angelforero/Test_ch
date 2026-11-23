import { Categoria } from './categoria';

/**
 * Representación cliente de una herramienta.
 *
 * Mapea 1:1 el {@code HerramientaDto} del backend (record Java:
 * {@code (Long id, String nombre, Categoria categoria)}). Los
 * tipos TypeScript son suficientes — no se añade ninguna clase ni
 * validación runtime porque los datos vienen del interceptor
 * tipado y, en caso de corrupción, el banner mostraría el error.
 */
export interface Herramienta {
  id:        number;
  nombre:    string;
  categoria: Categoria;
}
