import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ApiRequestOptions {
  readonly headers?: HttpHeaders;
  readonly params?: HttpParams;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  get<T>(path: string, options: ApiRequestOptions = {}): Observable<T> {
    return this.http.get<T>(this.buildUrl(path), options);
  }

  post<T>(path: string, body: unknown, options: ApiRequestOptions = {}): Observable<T> {
    return this.http.post<T>(this.buildUrl(path), body, options);
  }

  private buildUrl(path: string): string {
    return `${environment.apiBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
  }
}
