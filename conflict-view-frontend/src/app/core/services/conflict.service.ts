import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, shareReplay, timer, switchMap } from 'rxjs';
import { ConflictMap, ConflictDetail, NewsArticle, ConflictEvent, ConflictStats, PageResponse, OsintResource, OsintSummary } from '../models/conflict.model';

@Injectable({ providedIn: 'root' })
export class ConflictService {
  private http = inject(HttpClient);
  private base = '/api/conflicts';
  private mapCache$?: Observable<ConflictMap[]>;

  getAllForMap(): Observable<ConflictMap[]> {
    if (!this.mapCache$) {
      this.mapCache$ = this.http.get<ConflictMap[]>(this.base).pipe(
        shareReplay({ bufferSize: 1, refCount: true })
      );
      // Invalidate cache after 5 minutes
      timer(300_000).subscribe(() => { this.mapCache$ = undefined; });
    }
    return this.mapCache$;
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

  getOsint(id: string, page = 0, size = 20, type?: string): Observable<PageResponse<OsintResource>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (type) params = params.set('type', type);
    return this.http.get<PageResponse<OsintResource>>(`${this.base}/${id}/osint`, { params });
  }

  getOsintSummary(id: string): Observable<OsintSummary> {
    return this.http.get<OsintSummary>(`${this.base}/${id}/osint/summary`);
  }
}
