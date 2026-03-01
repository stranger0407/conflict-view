import { Pipe, PipeTransform } from '@angular/core';
import { formatDistanceToNow, parseISO } from 'date-fns';

@Pipe({
  name: 'timeAgo',
  standalone: true
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | null): string {
    if (!value) return 'Unknown';
    const date = typeof value === 'string' ? parseISO(value) : value;
    return formatDistanceToNow(date, { addSuffix: true });
  }
}
