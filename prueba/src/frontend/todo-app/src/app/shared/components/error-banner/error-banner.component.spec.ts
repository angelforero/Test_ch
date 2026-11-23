import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppError } from '../../../core/errors/app-error';
import { ProblemType } from '../../../core/errors/problem-type';
import { ErrorBannerComponent } from './error-banner.component';

/**
 * Tests de {@link ErrorBannerComponent}. Mantenerlos m\u00ednimos (D19 +
 * cobertura m\u00e1x 30%): verificar que renderiza title/detail y que se
 * oculta cuando error es null.
 */
describe('ErrorBannerComponent', () => {
  let fixture: ComponentFixture<ErrorBannerComponent>;
  let comp: ErrorBannerComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorBannerComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ErrorBannerComponent);
    comp = fixture.componentInstance;
  });

  it('renderiza title y detail cuando error no es null', () => {
    comp.error = new AppError(
      ProblemType.OverlapConflict,
      'Reserva solapada',
      'Ya hay una reserva activa en ese rango.',
      409,
    );
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Reserva solapada');
    expect(text).toContain('Ya hay una reserva activa');
  });

  it('renderiza los errores de campo del 422', () => {
    comp.error = new AppError(
      ProblemType.ValidationError,
      'Datos inválidos',
      'Revisa los campos.',
      422,
      { errors: [{ campo: 'correo', mensaje: 'debe ser una dirección de correo válida' }] },
    );
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('correo');
    expect(text).toContain('dirección de correo válida');
  });

  it('no renderiza el banner cuando error es null', () => {
    comp.error = null;
    fixture.detectChanges();

    const banner = (fixture.nativeElement as HTMLElement).querySelector('.banner');
    expect(banner).toBeNull();
  });
});
