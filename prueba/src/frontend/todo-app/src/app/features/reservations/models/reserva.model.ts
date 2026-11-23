import { EstadoReserva } from './estado-reserva';

export interface Reserva {
  id: number;
  herramientaId: number;
  nombreHerramienta: string;
  nombre: string;
  correo: string;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoReserva;
  cancelada: boolean;
}
