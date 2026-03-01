import { NewsArticle } from './conflict.model';
import { ConflictMap } from './conflict.model';

export interface DashboardStats {
  totalActiveConflicts: number;
  criticalConflicts: number;
  highConflicts: number;
  mediumConflicts: number;
  lowConflicts: number;
  monitoringConflicts: number;
  totalArticlesToday: number;
  totalArticlesAllTime: number;
  topConflictsBySeverity: ConflictMap[];
  latestArticles: NewsArticle[];
  conflictsByRegion: Record<string, number>;
  conflictsByType: Record<string, number>;
  monthlyCasualties: { month: string; incidents: number; casualties: number }[];
}
