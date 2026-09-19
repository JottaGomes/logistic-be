import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  errorMessage = '';
  passwordVisible = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (!this.authService.loginEnabled) {
      this.router.navigate(['/calculate-profit']);
    }
  }

  submit(): void {

    if (this.form.invalid) {
      return;
    }

    this.errorMessage = '';

    const { username, password } = this.form.value;

    this.authService.login(username!, password!).subscribe({
      next: () => this.router.navigate(['/calculate-profit']),
      error: () => (this.errorMessage = 'Invalid username or password'),
    });
  }
}
