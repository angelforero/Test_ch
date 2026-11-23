/**
 * Catálogo cerrado de categorías de herramientas.
 *
 * Replica 1:1 el enum {@code com.ch.prueba.herramientas.domain.Categoria}
 * del backend (ver spec doc 01 §6 / commit c796c83).
 *
 * Decisión (D16, commit 10): se hardcodea en el frontend en vez de
 * exponer un {@code GET /api/categories} porque:
 *   - el enum es estable y cambiarlo exige deploy conjunto de front+back;
 *   - un endpoint extra sería sobre-ingeniería para una lista de 7 ítems;
 *   - al ser {@code enum} TypeScript, el compilador detectaría al vuelo
 *     cualquier desalineación si se tocaran los valores.
 *
 * Si en algún momento el catálogo pasa a ser dinámico (nuevas categorías
 * sin redeploy), se sustituye este enum por una llamada HTTP y un signal
 * cacheado en {@code ToolsService}.
 */
export enum Categoria {
  PERFORACION = 'PERFORACION',
  CORTE       = 'CORTE',
  LIJADO      = 'LIJADO',
  MEDICION    = 'MEDICION',
  ESCALERAS   = 'ESCALERAS',
  JARDIN      = 'JARDIN',
  OTROS       = 'OTROS',
}

/**
 * Etiquetas humanas para pintar en los {@code <select>} y columnas de la UI.
 * No se usan como clave — el valor del enum sigue siendo el que viaja en HTTP.
 */
export const CATEGORIA_LABEL: Record<Categoria, string> = {
  [Categoria.PERFORACION]: 'Perforación',
  [Categoria.CORTE]:       'Corte',
  [Categoria.LIJADO]:      'Lijado',
  [Categoria.MEDICION]:    'Medición',
  [Categoria.ESCALERAS]:   'Escaleras',
  [Categoria.JARDIN]:      'Jardín',
  [Categoria.OTROS]:       'Otros',
};

/** Lista estable para iterar en {@code <select>} (mismo orden que el enum backend). */
export const CATEGORIAS: readonly Categoria[] = [
  Categoria.PERFORACION,
  Categoria.CORTE,
  Categoria.LIJADO,
  Categoria.MEDICION,
  Categoria.ESCALERAS,
  Categoria.JARDIN,
  Categoria.OTROS,
];
