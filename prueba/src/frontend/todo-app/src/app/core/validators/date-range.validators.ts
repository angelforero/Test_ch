import { AbstractControl, ValidationErrors } from '@angular/forms';

export function orderedRangeValidator(control: AbstractControl): ValidationErrors | null {
  const start = control.get('fechaInicio')?.value as string;
  const end = control.get('fechaFin')?.value as string;
  if (!start || !end) return null;
  return start <= end ? null : { range: true };
}

export function noPastValidator(control: AbstractControl): ValidationErrors | null {
  const start = control.get('fechaInicio')?.value as string;
  if (!start) return null;
  const today = new Date().toISOString().slice(0, 10);
  return start >= today ? null : { past: true };
}

export function maxDurationValidator(control: AbstractControl): ValidationErrors | null {
  const start = control.get('fechaInicio')?.value as string;
  const end = control.get('fechaFin')?.value as string;
  if (!start || !end) return null;
  const days = (Date.parse(end) - Date.parse(start)) / 86_400_000;
  return days >= 0 && days <= 7 ? null : { tooLong: true };
}
