import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  // Prefix relative API calls with the configured base URL
  if (req.url.startsWith('/api')) {
    const apiReq = req.clone({
      url: `${environment.apiUrl}${req.url.replace('/api', '')}`
    });
    return next(apiReq);
  }
  return next(req);
};
