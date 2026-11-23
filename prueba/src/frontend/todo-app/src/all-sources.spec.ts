/**
 * Spec vac\u00edo cuyo \u00fanico prop\u00f3sito es forzar a karma a cargar
 * TODOS los m\u00f3dulos de la app para que la medici\u00f3n de cobertura
 * refleje el proyecto real y no s\u00f3lo lo que los tests tocan
 * (ver all-sources.ts y D19 doc 04).
 *
 * Sin este import, archivos como app.config.ts, app.routes.ts o los
 * placeholders no entrar\u00edan en el denominador y el porcentaje
 * reportado ser\u00eda artificialmente alto.
 */
import * as _allSources from './all-sources';

describe('all-sources barrel', () => {
  it('se importa correctamente (puente para coverage global)', () => {
    expect(_allSources).toBeTruthy();
  });
});
