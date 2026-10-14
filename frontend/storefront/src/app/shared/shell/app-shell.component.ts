import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShellComponent {
  readonly cartService = inject(CartService);
  readonly authService = inject(AuthService);
  readonly mobileNavOpen = signal(false);

  toggleNavigation(): void {
    this.mobileNavOpen.update((isOpen) => !isOpen);
  }

  closeNavigation(): void {
    this.mobileNavOpen.set(false);
  }

  signOut(): void {
    this.authService.logout();
    this.closeNavigation();
  }
}
