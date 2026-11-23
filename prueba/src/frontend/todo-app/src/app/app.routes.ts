import { Routes } from '@angular/router';

/**
 * Rutas del MVP (doc 04 §6):
 *
 *   /herramientas     - catálogo + filtro por categoría + consulta
 *                       de disponibilidad (R01, R02).
 *   /nueva-reserva    - formulario para crear una reserva (R03, R04).
 *   /mis-reservas     - listado por correo + cancelación (R05).
 *
 * En este commit 9 (chore) los 3 placeholders están implementados
 * como componentes vacíos para que el router no reviente y podamos
 * ver el shell. Las vistas reales llegan en commits 10, 11 y 12.
 *
 * Se usa loadComponent (lazy) por coherencia con la práctica
 * moderna de Angular — aunque en una app de este tamaño el beneficio
 * es nulo, mantiene el patrón para cuando la app crezca.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'herramientas',
  },
  {
    path: 'herramientas',
    loadComponent: () =>
      import('./features/tools/components/tools-list/tools-list.component')
        .then(m => m.ToolsListComponent),
    title: 'Catálogo de herramientas',
  },
  {
    path: 'nueva-reserva',
    loadComponent: () =>
      import('./features/reservations/components/reservation-form/reservation-form.component')
        .then(m => m.ReservationFormComponent),
    title: 'Nueva reserva',
  },
  {
    path: 'mis-reservas',
    loadComponent: () =>
      import('./features/reservations/components/my-reservations/my-reservations.component')
        .then(m => m.MyReservationsComponent),
    title: 'Mis reservas',
  },
  // Fallback: cualquier otra ruta vuelve al catálogo.
  { path: '**', redirectTo: 'herramientas' },
];
