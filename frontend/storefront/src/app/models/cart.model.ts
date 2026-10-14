import { Product } from './product.model';

export interface CartItem {
  readonly product: Product;
  readonly quantity: number;
}

export interface CartSummary {
  readonly items: readonly CartItem[];
  readonly totalItems: number;
  readonly subtotal: number;
  readonly shipping: number;
  readonly total: number;
}
