import { Pipe, PipeTransform } from '@angular/core';
import { ConflictType } from '../../core/models/conflict.model';

const LABELS: Record<ConflictType, string> = {
  WAR: 'War / Armed Conflict',
  CIVIL_UNREST: 'Civil Unrest',
  TERRORISM: 'Terrorism / Insurgency',
  POLITICAL: 'Political Crisis',
  HUMANITARIAN: 'Humanitarian Crisis',
  BORDER_DISPUTE: 'Border Dispute',
  COUP: 'Coup / Government Crisis',
  OTHER: 'Other'
};

@Pipe({ name: 'conflictTypeLabel', standalone: true })
export class ConflictTypeLabelPipe implements PipeTransform {
  transform(value: ConflictType | string): string {
    return LABELS[value as ConflictType] ?? value?.replace('_', ' ') ?? '';
  }
}
