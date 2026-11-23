export enum EstadoReserva {
  CONFIRMADA = 'CONFIRMADA',
  EN_CURSO = 'EN_CURSO',
  FINALIZADA = 'FINALIZADA',
  CANCELADA = 'CANCELADA',
}

export const ESTADO_RESERVA_LABEL: Record<EstadoReserva, string> = {
  [EstadoReserva.CONFIRMADA]: 'Confirmada',
  [EstadoReserva.EN_CURSO]: 'En curso',
  [EstadoReserva.FINALIZADA]: 'Finalizada',
  [EstadoReserva.CANCELADA]: 'Cancelada',
};
