import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './seller-dashboard.component.html',
  styleUrl: './seller-dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SellerDashboardComponent {
  readonly authService = inject(AuthService);

  readonly metrics = [
    { label: 'Gross sales', value: '$0.00', detail: 'This month' },
    { label: 'Orders', value: '0', detail: 'Awaiting your first sale' },
    { label: 'Live listings', value: '0', detail: 'Ready to be created' }
  ];
}
