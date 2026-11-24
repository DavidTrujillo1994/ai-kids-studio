import { CanDeactivateFn } from '@angular/router';

export interface SavesBeforeLeaving {
  /** Guarda lo pendiente; falso si no se pudo y la persona prefiere quedarse. */
  canLeave(): Promise<boolean>;
}

export const unsavedGuard: CanDeactivateFn<SavesBeforeLeaving> = (component) => component.canLeave();
