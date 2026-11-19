import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { BusyService } from '../services/busy.service';

// Métodos que crean, editan o borran: son los que bloquean los botones
const MUTATING_METHODS = ['POST', 'PUT', 'PATCH', 'DELETE'];

// Para peticiones que corren en segundo plano y no deben bloquear la página
// (sincronizar el carrito, esperar al chatbot). Uso:
// this.http.post(url, body, { context: new HttpContext().set(SKIP_BUSY, true) })
export const SKIP_BUSY = new HttpContextToken<boolean>(() => false);

// Mientras haya un POST/PUT/PATCH/DELETE en curso, BusyService deshabilita los botones.
// finalize se ejecuta siempre: al responder, al fallar o si la petición se cancela.
export const busyInterceptor: HttpInterceptorFn = (req, next) => {
  if (!MUTATING_METHODS.includes(req.method) || req.context.get(SKIP_BUSY)) {
    return next(req);
  }

  const busy = inject(BusyService);
  busy.start();
  return next(req).pipe(finalize(() => busy.end()));
};
