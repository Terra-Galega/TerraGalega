import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { SKIP_BUSY } from '../interceptors/busy.interceptor';
import { Observable } from 'rxjs';
import { API_URL } from '../api-url';
import { ChatMessage, ChatResponse } from '../models/chat';

// Habla con el asistente del back (Gemini). El token lo agrega el interceptor.
@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);

  send(message: string, history: ChatMessage[]): Observable<ChatResponse> {
    // La respuesta del asistente puede tardar varios segundos: no bloquea el resto de la
    // página (el chat ya desactiva su propio botón de enviar con `sending`).
    return this.http.post<ChatResponse>(
      `${API_URL}/clients/me/chat`,
      { message, history },
      { context: new HttpContext().set(SKIP_BUSY, true) },
    );
  }
}