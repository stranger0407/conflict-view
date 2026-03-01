import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConflictMap, ConflictDetail, NewsArticle, ConflictEvent, ConflictStats, PageResponse } from '../models/conflict.model';

@Injectable({ providedIn: 'root' })
export class ConflictService {
  private http = inject(HttpClient);
  private base = '/api/conflicts';

  getAllForMap(): Observable<ConflictMap[]> {
    return this.http.get<ConflictMap[]>(this.base);
  }

  getDetail(id: string): Observable<ConflictDetail> {
    return this.http.get<ConflictDetail>(`${this.base}/${id}`);
  }

  getNews(id: string, page = 0, size = 20, sentiment?: string, source?: string): Observable<PageResponse<NewsArticle>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (sentiment) params = params.set('sentiment', sentiment);
    if (source) params = params.set('source', source);
    return this.http.get<PageResponse<NewsArticle>>(`${this.base}/${id}/news`, { params });
  }

  getEvents(id: string): Observable<ConflictEvent[]> {
    return this.http.get<ConflictEvent[]>(`${this.base}/${id}/events`);
  }

  getStats(id: string): Observable<ConflictStats> {
    return this.http.get<ConflictStats>(`${this.base}/${id}/stats`);
  }

  search(q?: string, region?: string, severity?: string, type?: string, status?: string): Observable<ConflictMap[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (region) params = params.set('region', region);
    if (severity) params = params.set('severity', severity);
    if (type) params = params.set('type', type);
    if (status) params = params.set('status', status);
    return this.http.get<ConflictMap[]>(`${this.base}/search`, { params });
  }
}
