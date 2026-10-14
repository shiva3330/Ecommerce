export type UserRole = 'buyer' | 'seller' | 'admin';

export interface User {
  readonly id: string;
  readonly name: string;
  readonly email: string;
  readonly role: UserRole;
}

export interface LoginCredentials {
  readonly email: string;
  readonly password: string;
}

export interface AuthSession {
  readonly accessToken: string;
  readonly user: User;
}
