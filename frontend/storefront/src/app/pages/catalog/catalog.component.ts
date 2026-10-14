import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { Product, ProductSort } from '../../models/product.model';

@Component({
  selector: 'app-catalog-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogPageComponent implements OnInit {
  private readonly catalogService = inject(CatalogService);
  readonly cartService = inject(CartService);

  readonly products = this.catalogService.products;
  readonly isLoading = this.catalogService.isLoading;
  readonly usingDevelopmentFallback = this.catalogService.usingDevelopmentFallback;
  readonly searchTerm = signal('');
  readonly selectedCategory = signal('all');
  readonly selectedSort = signal<ProductSort>('featured');
  readonly liveMessage = signal('');

  readonly categories = computed(() => [
    'all',
    ...new Set(this.products().map((product) => product.category))
  ]);

  readonly filteredProducts = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const category = this.selectedCategory();
    const sort = this.selectedSort();

    return [...this.products()]
      .filter((product) => {
        const matchesSearch =
          !term ||
          `${product.name} ${product.description} ${product.seller.name} ${product.tags.join(' ')}`
            .toLowerCase()
            .includes(term);
        const matchesCategory = category === 'all' || product.category === category;
        return matchesSearch && matchesCategory;
      })
      .sort((first, second) => {
        switch (sort) {
          case 'price-asc':
            return first.price - second.price;
          case 'price-desc':
            return second.price - first.price;
          case 'rating':
            return second.rating - first.rating;
          case 'featured':
            return Number(second.featured) - Number(first.featured) || second.rating - first.rating;
          default:
            return 0;
        }
      });
  });

  ngOnInit(): void {
    this.catalogService.loadProducts().subscribe();
  }

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  onCategoryChange(event: Event): void {
    this.selectedCategory.set((event.target as HTMLSelectElement).value);
  }

  onSortChange(event: Event): void {
    this.selectedSort.set((event.target as HTMLSelectElement).value as ProductSort);
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set('all');
    this.selectedSort.set('featured');
  }

  addToCart(product: Product): void {
    this.cartService.addItem(product);
    this.liveMessage.set(`${product.name} was added to your cart.`);
  }

  trackByProduct(_index: number, product: Product): string {
    return product.id;
  }
}
