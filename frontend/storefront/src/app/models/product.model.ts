export type ProductSort = 'featured' | 'price-asc' | 'price-desc' | 'rating';

export interface SellerSummary {
  readonly id: string;
  readonly name: string;
  readonly rating: number;
}

export interface Product {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly category: string;
  readonly price: number;
  readonly currency: 'USD';
  readonly rating: number;
  readonly reviewCount: number;
  readonly inventory: number;
  readonly seller: SellerSummary;
  readonly tags: readonly string[];
  readonly featured: boolean;
}
