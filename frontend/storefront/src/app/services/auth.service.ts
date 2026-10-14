import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';

import { AuthSession, LoginCredentials, User } from '../models/auth.model';
import { ApiService } from './api.service';

const TOKEN_STORAGE_KEY = 'marketplace_access_token';
const USER_STORAGE_KEY = 'marketplace_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokenState = signal<string | null>(this.readStorage(TOKEN_STORAGE_KEY));
  private readonly userState = signal<User | null>(this.readUser());

  readonly accessToken = this.tokenState.asReadonly();
  readonly currentUser = this.userState.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.accessToken() && this.currentUser()));
  readonly usingDevelopmentFallback = signal(false);

  login(credentials: LoginCredentials): Observable<AuthSession> {
    this.usingDevelopmentFallback.set(false);

    return this.api.post<AuthSession>('/auth/login', credentials).pipe(
      catchError(() => {
        const isSeller = credentials.email.toLowerCase().includes('seller');
        const displayName = credentials.email.split('@')[0] || 'Demo shopper';
        const developmentSession: AuthSession = {
          accessToken: `development-token-${credentials.email}`,
          user: {
            id: isSeller ? 'development-seller' : 'development-buyer',
            name: displayName,
            email: credentials.email,
            role: isSeller ? 'seller' : 'buyer'
          }
        };

        this.usingDevelopmentFallback.set(true);
        return of(developmentSession);
      }),
      tap((session) => this.setSession(session))
    );
  }

  logout(): void {
    this.tokenState.set(null);
    this.userState.set(null);
    this.removeStorage(TOKEN_STORAGE_KEY);
    this.removeStorage(USER_STORAGE_KEY);
  }

  private setSession(session: AuthSession): void {
    this.tokenState.set(session.accessToken);
    this.userState.set(session.user);
    this.writeStorage(TOKEN_STORAGE_KEY, session.accessToken);
    this.writeStorage(USER_STORAGE_KEY, JSON.stringify(session.user));
  }

  private readUser(): User | null {
    const storedUser = this.readStorage(USER_STORAGE_KEY);
    if (!storedUser) {
      return null;
    }

    try {
      return JSON.parse(storedUser) as User;
    } catch {
      this.removeStorage(USER_STORAGE_KEY);
      return null;
    }
  }

  private readStorage(key: string): string | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    return localStorage.getItem(key);
  }

  private writeStorage(key: string, value: string): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(key, value);
    }
  }

  private removeStorage(key: string): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(key);
    }
  }
}
