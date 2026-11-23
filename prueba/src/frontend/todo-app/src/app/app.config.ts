import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { problemDetailInterceptor } from './core/interceptors/problem-detail.interceptor';

/**
 * Configuración raíz de la app.
 *
 *  - provideZoneChangeDetection con eventCoalescing para menos ciclos
 *    de change detection.
 *  - provideRouter con las 3 rutas del MVP. withComponentInputBinding()
 *    permite a los componentes recibir @Input() desde los params de
 *    ruta (lo usamos p.ej. en una posible navegación con
 *    ?correo=... en el futuro).
 *  - provideHttpClient con un único interceptor funcional (la API
 *    moderna de Angular 19 reemplaza la clase-basada por funciones).
 *    El interceptor transforma los ProblemDetail del backend en
 *    AppError y re-lanza para que el componente decida qué mostrar.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([problemDetailInterceptor])),
  ],
};
