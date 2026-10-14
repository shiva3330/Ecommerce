import { HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';

import { CreateOrderRequest, OrderConfirmation } from '../models/checkout.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly api = inject(ApiService);

  placeOrder(request: CreateOrderRequest, idempotencyKey: string): Observable<OrderConfirmation> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });

    return this.api.post<{ orderId: string; status: string }>('/orders', request, { headers }).pipe(
      switchMap((response) => this.api.get<PersistedOrderResponse>(`/orders/${response.orderId}`)),
      map((response) => ({
        id: response.orderId,
        status: response.status.toLowerCase() === 'pending' ? ('processing' as const) : ('received' as const),
        createdAt: response.createdAt
      }))
    );
  }
}

interface PersistedOrderResponse {
  readonly orderId: string;
  readonly status: string;
  readonly createdAt: string;
}
