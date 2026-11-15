import { API_URL } from '../api-url';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);

  emailReport(
    to: string,
    subject: string,
    file: Blob,
    filename: string,
  ): Observable<{ message: string }> {
    const form = new FormData();
    form.append('to', to);
    form.append('subject', subject);
    form.append('file', file, filename);
    return this.http.post<{ message: string }>(
      `${API_URL}/reports/email`,
      form,
    );
  }
}
