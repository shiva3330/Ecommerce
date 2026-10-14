import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, of, tap } from 'rxjs';

import { DEVELOPMENT_CATALOG } from '../data/development-catalog';
import { Product } from '../models/product.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly api = inject(ApiService);

  readonly products = signal<readonly Product[]>([]);
  readonly isLoading = signal(false);
  readonly usingDevelopmentFallback = signal(false);

  loadProducts(): Observable<readonly Product[]> {
    this.isLoading.set(true);

    return this.api.get<readonly Product[]>('/products').pipe(
      tap((products) => {
        this.products.set(products);
        this.usingDevelopmentFallback.set(false);
      }),
      catchError(() => {
        this.products.set(DEVELOPMENT_CATALOG);
        this.usingDevelopmentFallback.set(true);
        return of(DEVELOPMENT_CATALOG);
      }),
      finalize(() => this.isLoading.set(false))
    );
  }
}
