import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../api-url';
import { ChatMessage, ChatResponse } from '../models/chat';

// Habla con el asistente del back (Gemini). El token lo agrega el interceptor.
@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);

  send(message: string, history: ChatMessage[]): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${API_URL}/clients/me/chat`, { message, history });
  }
}