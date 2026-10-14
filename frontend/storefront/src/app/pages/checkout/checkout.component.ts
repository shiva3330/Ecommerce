import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CheckoutFormValue, CreateOrderRequest, OrderConfirmation, PaymentMethod } from '../../models/checkout.model';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-checkout-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CheckoutPageComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  readonly cartService = inject(CartService);
  readonly orderService = inject(OrderService);
  readonly cart = this.cartService.summary;
  readonly isSubmitting = signal(false);
  readonly orderConfirmation = signal<OrderConfirmation | null>(null);
  readonly errorMessage = signal('');
  private checkoutIdempotencyKey: string | null = null;

  readonly checkoutForm = this.formBuilder.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    address: ['', Validators.required],
    city: ['', Validators.required],
    postalCode: ['', Validators.required],
    paymentMethod: this.formBuilder.control<PaymentMethod>('demo-card')
  });

  placeOrder(): void {
    if (this.isSubmitting() || this.checkoutForm.invalid || this.cart().items.length === 0) {
      this.checkoutForm.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.isSubmitting.set(true);
    const customer = this.checkoutForm.getRawValue() as CheckoutFormValue;
    const request: CreateOrderRequest = {
      customerId: customer.email,
      items: this.cart().items.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity
      }))
    };

    const idempotencyKey = this.checkoutIdempotencyKey ??= createIdempotencyKey();
    this.orderService.placeOrder(request, idempotencyKey).subscribe({
      next: (confirmation) => {
        this.orderConfirmation.set(confirmation);
        this.cartService.clear();
        this.isSubmitting.set(false);
      },
      error: () => {
        this.errorMessage.set('We could not confirm that order. Please try again; your checkout key will safely prevent a duplicate.');
        this.isSubmitting.set(false);
      }
    });
  }
}

function createIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `checkout-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
