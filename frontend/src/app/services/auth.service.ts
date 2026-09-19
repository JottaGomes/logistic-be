import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

interface LoginResponse {
  success: boolean;
  data: { token: string; username: string };
}

interface ConfigResponse {
  success: boolean;
  data: { loginEnabled: boolean };
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly baseUrl = `${environment.apiUrl}/api/auth`;
  private readonly tokenKey = 'auth_token';
  private readonly usernameKey = 'auth_username';

  loginEnabled = true;

  constructor(private http: HttpClient, private router: Router) {}

  loadConfig(): Promise<void> {
    return this.http.get<ConfigResponse>(`${this.baseUrl}/config`).toPromise().then(response => {
      this.loginEnabled = response?.data?.loginEnabled ?? true;
    }).catch(() => {
      // backend unreachable: still boot the app, and require login rather than
      // silently exposing the authenticated screens
      this.loginEnabled = true;
    });
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { username, password }).pipe(
      tap(response => {
        if (response.success) {
          localStorage.setItem(this.tokenKey, response.data.token);
          localStorage.setItem(this.usernameKey, response.data.username);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.usernameKey);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getUsername(): string | null {
    return localStorage.getItem(this.usernameKey);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired(token);
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}
