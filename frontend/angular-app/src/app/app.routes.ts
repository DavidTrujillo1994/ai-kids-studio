import { Routes } from '@angular/router';
import { unsavedGuard } from './pages/episode/unsaved.guard';

export const routes: Routes = [
  { path: '', title: 'Inicio · AI Kids Studio', loadComponent: () => import('./pages/home/home').then((m) => m.Home) },
  { path: 'nuevo', title: 'Nuevo episodio · AI Kids Studio', loadComponent: () => import('./pages/new-episode/new-episode').then((m) => m.NewEpisode) },
  {
    path: 'episodios/:id',
    title: 'Episodio · AI Kids Studio',
    canDeactivate: [unsavedGuard],
    loadComponent: () => import('./pages/episode/episode').then((m) => m.EpisodePage),
  },
  { path: 'personajes', title: 'Personajes · AI Kids Studio', loadComponent: () => import('./pages/characters/characters').then((m) => m.Characters) },
  { path: 'ajustes', title: 'Ajustes · AI Kids Studio', loadComponent: () => import('./pages/settings/settings').then((m) => m.Settings) },
  { path: '**', redirectTo: '' },
];
