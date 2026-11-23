/**
 * Barrel auxiliar para medici\u00f3n de cobertura.
 *
 * Por defecto, karma-coverage solo instrumenta los archivos que alg\u00fan
 * .spec.ts importa transitivamente. Eso infla artificialmente el
 * porcentaje y oculta c\u00f3digo sin tests (p.ej. placeholders, app.config).
 *
 * Al re-exportar aqu\u00ed cada m\u00f3dulo de la app y referenciar este archivo
 * desde el tsconfig.spec.json, forzamos al runtime de tests a cargar
 * TODOS los m\u00f3dulos aunque no tengan spec. As\u00ed el total de statements
 * medido refleja el proyecto real, igual que hace JaCoCo en el backend.
 *
 * Mantener alineado con la estructura: si se a\u00f1ade un nuevo fichero
 * .ts de producci\u00f3n, inclu\u00edrlo aqu\u00ed.
 */
export * from './app/app.component';
export * from './app/app.config';
export * from './app/app.routes';

export * from './app/core/api/api.config';
export * from './app/core/errors/app-error';
export * from './app/core/errors/problem-type';
export * from './app/core/interceptors/problem-detail.interceptor';
export * from './app/core/validators/date-range.validators';

export * from './app/features/tools/models/categoria';
export * from './app/features/tools/models/herramienta.model';
export * from './app/features/tools/services/tools.service';
export * from './app/features/tools/components/tools-list/tools-list.component';
export * from './app/features/tools/components/availability-form/availability-form.component';

export * from './app/features/reservations/models/estado-reserva';
export * from './app/features/reservations/models/reserva-creacion.model';
export * from './app/features/reservations/models/reserva.model';
export * from './app/features/reservations/services/reservations.service';
export * from './app/features/reservations/components/reservation-form/reservation-form.component';
export * from './app/features/reservations/components/reservation-success/reservation-success.component';
export * from './app/features/reservations/components/my-reservations/my-reservations.component';

export * from './app/shared/components/error-banner/error-banner.component';
