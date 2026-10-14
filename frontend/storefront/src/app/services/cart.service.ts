import { Injectable, computed, signal } from '@angular/core';

import { CartItem, CartSummary } from '../models/cart.model';
import { Product } from '../models/product.model';

const FREE_SHIPPING_THRESHOLD = 100;
const STANDARD_SHIPPING = 12;

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemState = signal<readonly CartItem[]>([]);

  readonly items = this.itemState.asReadonly();
  readonly totalItems = computed(() => this.itemState().reduce((total, item) => total + item.quantity, 0));
  readonly subtotal = computed(() =>
    this.roundCurrency(this.itemState().reduce((total, item) => total + item.product.price * item.quantity, 0))
  );
  readonly shipping = computed(() => {
    const subtotal = this.subtotal();
    return subtotal === 0 || subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : STANDARD_SHIPPING;
  });
  readonly total = computed(() => this.roundCurrency(this.subtotal() + this.shipping()));
  readonly summary = computed<CartSummary>(() => ({
    items: this.items(),
    totalItems: this.totalItems(),
    subtotal: this.subtotal(),
    shipping: this.shipping(),
    total: this.total()
  }));

  addItem(product: Product, quantity = 1): void {
    if (product.inventory <= 0) {
      return;
    }

    const requestedQuantity = Number.isFinite(quantity) ? Math.max(1, Math.floor(quantity)) : 1;
    const existingItem = this.itemState().find((item) => item.product.id === product.id);

    if (existingItem) {
      this.updateQuantity(product.id, existingItem.quantity + requestedQuantity);
      return;
    }

    this.itemState.update((items) => [
      ...items,
      { product, quantity: Math.min(requestedQuantity, product.inventory) }
    ]);
  }

  updateQuantity(productId: string, quantity: number): void {
    const safeQuantity = Math.floor(quantity);
    if (safeQuantity <= 0) {
      this.removeItem(productId);
      return;
    }

    this.itemState.update((items) =>
      items.map((item) =>
        item.product.id === productId
          ? { ...item, quantity: Math.min(safeQuantity, item.product.inventory) }
          : item
      )
    );
  }

  removeItem(productId: string): void {
    this.itemState.update((items) => items.filter((item) => item.product.id !== productId));
  }

  clear(): void {
    this.itemState.set([]);
  }

  private roundCurrency(value: number): number {
    return Math.round(value * 100) / 100;
  }
}
