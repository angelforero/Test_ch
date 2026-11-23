import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../../../core/api/api.config';
import { ReservaCreacion } from '../models/reserva-creacion.model';
import { Reserva } from '../models/reserva.model';

@Injectable({ providedIn: 'root' })
export class ReservationsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/reservations`;

  crear(payload: ReservaCreacion): Promise<Reserva> {
    return firstValueFrom(this.http.post<Reserva>(this.url, payload));
  }

  cancelar(id: number): Promise<Reserva> {
    return firstValueFrom(this.http.delete<Reserva>(`${this.url}/${id}`));
  }

  listarPorCorreo(correo: string): Promise<Reserva[]> {
    const params = new HttpParams().set('correo', correo);
    return firstValueFrom(this.http.get<Reserva[]>(this.url, { params }));
  }
}
