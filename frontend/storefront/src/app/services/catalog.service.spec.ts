import { TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';

import { DEVELOPMENT_CATALOG } from '../data/development-catalog';
import { ApiService } from './api.service';
import { CatalogService } from './catalog.service';

describe('CatalogService', () => {
  it('uses the labeled development catalog when the API is unavailable', () => {
    const api = {
      get: jasmine.createSpy('get').and.returnValue(throwError(() => new Error('offline')))
    };
    TestBed.configureTestingModule({
      providers: [CatalogService, { provide: ApiService, useValue: api }]
    });
    const service = TestBed.inject(CatalogService);
    let loadedProducts = 0;

    service.loadProducts().subscribe((products) => {
      loadedProducts = products.length;
    });

    expect(api.get).toHaveBeenCalledWith('/products');
    expect(loadedProducts).toBe(DEVELOPMENT_CATALOG.length);
    expect(service.products()).toEqual(DEVELOPMENT_CATALOG);
    expect(service.usingDevelopmentFallback()).toBeTrue();
    expect(service.isLoading()).toBeFalse();
  });
});
