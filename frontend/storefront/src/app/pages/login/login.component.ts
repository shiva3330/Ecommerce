import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPageComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');

  readonly loginForm = this.formBuilder.group({
    email: ['seller@example.com', [Validators.required, Validators.email]],
    password: ['demo123', [Validators.required, Validators.minLength(6)]]
  });

  submit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.isSubmitting.set(true);
    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (session) => {
        this.isSubmitting.set(false);
        void this.router.navigateByUrl(session.user.role === 'seller' ? '/seller' : '/');
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('We could not sign you in. Please check your details and try again.');
      }
    });
  }
}
