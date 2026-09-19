import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private readonly baseUrl = `${environment.apiUrl}/api/shipments`;

  constructor(private http: HttpClient) {}

  calculate(payload: { income: number; cost: number; additionalCost: number }): Observable<any> {
    return this.http.post<{ data: any }>(`${this.baseUrl}/calculate`, payload).pipe(
      map((res) => res.data)
    );
  }

  findAll(page = 0, size = 10): Observable<any> {
    return this.http.get<{ data: any }>(this.baseUrl, {
      params: { page: page.toString(), size: size.toString() },
    }).pipe(
      map((res) => res.data)
    );
  }
}
