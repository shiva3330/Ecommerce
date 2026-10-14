import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { ApiService } from './api.service';
import { OrderService } from './order.service';

describe('OrderService', () => {
  let service: OrderService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OrderService, ApiService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(OrderService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('uses the idempotency key and reads the durable order after creation', () => {
    let confirmation: { id: string; status: string; createdAt: string } | undefined;

    service.placeOrder(
      {
        customerId: 'customer@example.com',
        items: [{ productId: '11111111-1111-1111-1111-111111111111', quantity: 2 }]
      },
      'checkout-key-1'
    ).subscribe((value) => (confirmation = value));

    const create = httpTesting.expectOne(`${environment.apiBaseUrl}/orders`);
    expect(create.request.method).toBe('POST');
    expect(create.request.headers.get('Idempotency-Key')).toBe('checkout-key-1');
    create.flush({ orderId: '22222222-2222-2222-2222-222222222222', status: 'PENDING' });

    const read = httpTesting.expectOne(`${environment.apiBaseUrl}/orders/22222222-2222-2222-2222-222222222222`);
    expect(read.request.method).toBe('GET');
    read.flush({
      orderId: '22222222-2222-2222-2222-222222222222',
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z'
    });

    expect(confirmation).toEqual({
      id: '22222222-2222-2222-2222-222222222222',
      status: 'processing',
      createdAt: '2026-01-01T00:00:00Z'
    });
  });
});
