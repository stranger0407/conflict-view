import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NewsSource } from '../models/news-source.model';

@Injectable({ providedIn: 'root' })
export class NewsSourceService {
  private http = inject(HttpClient);

  getSources(): Observable<NewsSource[]> {
    return this.http.get<NewsSource[]>('/api/sources');
  }
}
