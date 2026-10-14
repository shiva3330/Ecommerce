import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { Product } from '../../models/product.model';
import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { CatalogPageComponent } from './catalog.component';

describe('CatalogPageComponent', () => {
  let fixture: ComponentFixture<CatalogPageComponent>;
  let component: CatalogPageComponent;
  const product: Product = {
    id: 'linen-throw',
    name: 'Linen throw',
    description: 'Soft woven linen for the sofa.',
    category: 'Home',
    price: 86,
    currency: 'USD',
    rating: 4.9,
    reviewCount: 10,
    inventory: 2,
    seller: { id: 'seller-one', name: 'Studio One', rating: 4.9 },
    tags: ['linen', 'home'],
    featured: true
  };
  const secondProduct: Product = {
    ...product,
    id: 'ceramic-mug',
    name: 'Ceramic mug',
    description: 'Hand-finished ceramic for morning coffee.',
    category: 'Kitchen',
    price: 24,
    tags: ['ceramic', 'kitchen']
  };
  const productState = signal<readonly Product[]>([product, secondProduct]);
  const catalogServiceMock = {
    products: productState,
    isLoading: signal(false),
    usingDevelopmentFallback: signal(true),
    loadProducts: jasmine.createSpy('loadProducts').and.returnValue(of(productState()))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogPageComponent],
      providers: [
        provideRouter([]),
        { provide: CatalogService, useValue: catalogServiceMock },
        CartService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the catalog and exposes category options', () => {
    expect(catalogServiceMock.loadProducts).toHaveBeenCalled();
    expect(component.categories()).toEqual(['all', 'Home', 'Kitchen']);
    expect(component.filteredProducts().length).toBe(2);
  });

  it('filters products by search text and category', () => {
    component.onSearch({ target: { value: 'linen' } } as unknown as Event);
    expect(component.filteredProducts().map((item) => item.id)).toEqual(['linen-throw']);

    component.onSearch({ target: { value: '' } } as unknown as Event);
    component.onCategoryChange({ target: { value: 'Kitchen' } } as unknown as Event);
    expect(component.filteredProducts().map((item) => item.id)).toEqual(['ceramic-mug']);
  });
});
