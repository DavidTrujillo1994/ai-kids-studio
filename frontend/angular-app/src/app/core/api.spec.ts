import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { ProductionApi, errorMessage } from './api';

describe('api', () => {
  it('envía la decisión explícita de la persona', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(ProductionApi).review('7', 'REJECT', 'Muy corto').subscribe();
    const request = http.expectOne('/api/v1/episodes/7/approval');
    expect(request.request.body).toEqual({ decision: 'REJECT', comments: 'Muy corto' });
    request.flush(null);
    http.verify();
  });

  it('muestra el mensaje del servidor y uno claro si no hay conexión', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 409, error: { detail: 'Ya está aprobado.' } }))).toBe('Ya está aprobado.');
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('conectar');
    expect(errorMessage(new Error('x'))).toContain('Intenta');
  });
});
