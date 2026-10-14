import { Product } from './product.model';

export type PaymentMethod = 'demo-card' | 'invoice';

export interface CheckoutFormValue {
  readonly fullName: string;
  readonly email: string;
  readonly address: string;
  readonly city: string;
  readonly postalCode: string;
  readonly paymentMethod: PaymentMethod;
}

export interface OrderLine {
  readonly productId: Product['id'];
  readonly quantity: number;
  readonly unitPrice: number;
}

export interface CreateOrderRequest {
  /** Matches backend/order-service CreateOrderRequest. */
  readonly customerId: string;
  readonly items: readonly Pick<OrderLine, 'productId' | 'quantity'>[];
}

export interface OrderConfirmation {
  readonly id: string;
  readonly status: 'received' | 'processing';
  readonly createdAt: string;
}
