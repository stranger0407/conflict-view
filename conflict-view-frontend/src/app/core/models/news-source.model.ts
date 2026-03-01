import { SourceCategory } from './conflict.model';

export interface NewsSource {
  id: string;
  domain: string;
  name: string;
  reliabilityScore: number;
  reliabilityLabel: 'High' | 'Medium' | 'Low';
  reliabilityColor: 'green' | 'yellow' | 'red';
  biasRating: string;
  category: SourceCategory;
  country: string;
  logoUrl: string | null;
}
