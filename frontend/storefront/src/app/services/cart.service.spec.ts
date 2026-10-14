import { Product } from '../models/product.model';
import { CartService } from './cart.service';

describe('CartService', () => {
  const product: Product = {
    id: 'test-product',
    name: 'Test product',
    description: 'A product used by the cart spec.',
    category: 'Test',
    price: 60,
    currency: 'USD',
    rating: 5,
    reviewCount: 1,
    inventory: 4,
    seller: { id: 'test-seller', name: 'Test seller', rating: 5 },
    tags: ['test'],
    featured: false
  };

  it('adds an item, merges repeated adds, and calculates shipping', () => {
    const service = new CartService();

    service.addItem(product);
    service.addItem(product);

    expect(service.totalItems()).toBe(2);
    expect(service.subtotal()).toBe(120);
    expect(service.shipping()).toBe(0);
    expect(service.total()).toBe(120);
  });

  it('removes an item when its quantity is reduced to zero', () => {
    const service = new CartService();

    service.addItem(product);
    service.updateQuantity(product.id, 0);

    expect(service.items()).toEqual([]);
    expect(service.totalItems()).toBe(0);
  });

  it('does not allow quantity to exceed available inventory', () => {
    const service = new CartService();

    service.addItem(product, 20);

    expect(service.items()[0]?.quantity).toBe(product.inventory);
  });
});
