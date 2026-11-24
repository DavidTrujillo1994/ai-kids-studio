import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Catalog, Episode, EpisodeEdit, EpisodeSummary, Job, NewEpisodeRequest, Script, Series, StoryCharacter, StoryTemplate, Tool } from './models';

const BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class SeriesApi {
  private readonly http = inject(HttpClient);

  all(): Observable<Series[]> {
    return this.http.get<Series[]>(`${BASE}/series`);
  }

  update(id: string, name: string, description: string): Observable<Series> {
    return this.http.put<Series>(`${BASE}/series/${id}`, { name, description });
  }

  characters(id: string): Observable<StoryCharacter[]> {
    return this.http.get<StoryCharacter[]>(`${BASE}/series/${id}/characters`);
  }

  registerCharacter(id: string, assetKey: string, role: string): Observable<StoryCharacter> {
    return this.http.post<StoryCharacter>(`${BASE}/series/${id}/characters`, { assetKey, role });
  }

  updateCharacter(id: string, name: string, personality: string): Observable<StoryCharacter> {
    return this.http.put<StoryCharacter>(`${BASE}/characters/${id}`, { name, personality });
  }

  approveDesign(id: string): Observable<StoryCharacter> {
    return this.http.post<StoryCharacter>(`${BASE}/characters/${id}/design-approval`, null);
  }
}

@Injectable({ providedIn: 'root' })
export class EpisodeApi {
  private readonly http = inject(HttpClient);

  ofSeries(seriesId: string): Observable<EpisodeSummary[]> {
    return this.http.get<EpisodeSummary[]>(`${BASE}/series/${seriesId}/episodes`);
  }

  create(seriesId: string, request: NewEpisodeRequest): Observable<Episode> {
    return this.http.post<Episode>(`${BASE}/series/${seriesId}/episodes`, request);
  }

  get(id: string): Observable<Episode> {
    return this.http.get<Episode>(`${BASE}/episodes/${id}`);
  }

  save(id: string, edit: EpisodeEdit): Observable<Episode> {
    return this.http.put<Episode>(`${BASE}/episodes/${id}`, edit);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${BASE}/episodes/${id}`);
  }

  thumbnailUrl(e: EpisodeSummary): string {
    return `${BASE}/episodes/${e.id}/thumbnail?v=${encodeURIComponent(e.updatedAt)}`;
  }

  videoUrl(e: EpisodeSummary): string {
    return `${BASE}/episodes/${e.id}/preview?v=${encodeURIComponent(e.updatedAt)}`;
  }

  downloadUrl(e: EpisodeSummary): string {
    return `${BASE}/episodes/${e.id}/preview?download=true`;
  }
}

@Injectable({ providedIn: 'root' })
export class ProductionApi {
  private readonly http = inject(HttpClient);

  start(episodeId: string): Observable<Job> {
    return this.http.post<Job>(`${BASE}/episodes/${episodeId}/production`, null);
  }

  job(id: string): Observable<Job> {
    return this.http.get<Job>(`${BASE}/jobs/${id}`);
  }

  active(): Observable<Job[]> {
    return this.http.get<Job[]>(`${BASE}/jobs`);
  }

  cancel(id: string): Observable<Job> {
    return this.http.post<Job>(`${BASE}/jobs/${id}/cancel`, null);
  }

  review(episodeId: string, decision: 'APPROVE' | 'REJECT', comments: string): Observable<void> {
    return this.http.post<void>(`${BASE}/episodes/${episodeId}/approval`, { decision, comments });
  }

  export(episodeId: string): Observable<{ folder: string }> {
    return this.http.post<{ folder: string }>(`${BASE}/episodes/${episodeId}/export`, null);
  }
}

@Injectable({ providedIn: 'root' })
export class SystemApi {
  private readonly http = inject(HttpClient);

  catalog(): Observable<Catalog> {
    return this.http.get<Catalog>(`${BASE}/catalog`);
  }

  tools(): Observable<Tool[]> {
    return this.http.get<Tool[]>(`${BASE}/tools`);
  }
}

@Injectable({ providedIn: 'root' })
export class StoryApi {
  private readonly http = inject(HttpClient);

  templates(): Observable<StoryTemplate[]> {
    return this.http.get<StoryTemplate[]>(`${BASE}/story-templates`);
  }

  suggest(script: Script, environment: string): Observable<Script> {
    return this.http.post<Script>(`${BASE}/movement-suggestions`, { script, environment });
  }
}

export const portraitUrl = (assetKey: string): string => `${BASE}/catalog/characters/${assetKey}/portrait`;
export const placeUrl = (environment: string): string => `${BASE}/catalog/environments/${environment}/view`;

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No pude conectar con la aplicación.';
    }
    return error.error?.detail ?? 'Algo salió mal. Intenta de nuevo.';
  }
  return 'Algo salió mal. Intenta de nuevo.';
}
