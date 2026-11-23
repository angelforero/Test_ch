import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstadoReserva } from '../../models/estado-reserva';
import { ReservationSuccessComponent } from './reservation-success.component';

describe('ReservationSuccessComponent', () => {
  let fixture: ComponentFixture<ReservationSuccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ReservationSuccessComponent] }).compileComponents();
    fixture = TestBed.createComponent(ReservationSuccessComponent);
  });

  it('renderiza id, herramienta, fechas y estado', () => {
    fixture.componentInstance.reserva = {
      id: 42,
      herramientaId: 1,
      nombreHerramienta: 'Taladro percutor',
      nombre: 'Ana Pérez',
      correo: 'ana@example.com',
      fechaInicio: '2026-12-01',
      fechaFin: '2026-12-03',
      estado: EstadoReserva.CONFIRMADA,
      cancelada: false,
    };
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('#42');
    expect(text).toContain('Taladro percutor');
    expect(text).toContain('2026-12-01');
    expect(text).toContain('Confirmada');
  });
});
