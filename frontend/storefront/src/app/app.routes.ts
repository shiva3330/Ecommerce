import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Browse products | Ecom',
    loadComponent: () => import('./pages/catalog/catalog.component').then((module) => module.CatalogPageComponent)
  },
  {
    path: 'cart',
    title: 'Your cart | Ecom',
    loadComponent: () => import('./pages/cart/cart.component').then((module) => module.CartPageComponent)
  },
  {
    path: 'checkout',
    title: 'Checkout | Ecom',
    loadComponent: () => import('./pages/checkout/checkout.component').then((module) => module.CheckoutPageComponent)
  },
  {
    path: 'login',
    title: 'Sign in | Ecom',
    loadComponent: () => import('./pages/login/login.component').then((module) => module.LoginPageComponent)
  },
  {
    path: 'seller',
    title: 'Seller dashboard | Ecom',
    loadComponent: () =>
      import('./pages/seller-dashboard/seller-dashboard.component').then(
        (module) => module.SellerDashboardComponent
      )
  },
  { path: '**', redirectTo: '' }
];
