export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ConflictType = 'WAR' | 'CIVIL_UNREST' | 'TERRORISM' | 'POLITICAL' | 'HUMANITARIAN' | 'BORDER_DISPUTE' | 'COUP' | 'OTHER';
export type ConflictStatus = 'ACTIVE' | 'MONITORING' | 'CEASEFIRE' | 'RESOLVED';
export type SentimentType = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
export type SourceCategory = 'MAINSTREAM' | 'INDEPENDENT' | 'GOVERNMENT' | 'WIRE_SERVICE' | 'UNKNOWN';

export interface ConflictMap {
  id: string;
  name: string;
  region: string;
  latitude: number;
  longitude: number;
  severity: Severity;
  conflictType: ConflictType;
  status: ConflictStatus;
  startDate: string;
  involvedParties: string;
  articleCount: number;
}

export interface ConflictDetail {
  id: string;
  name: string;
  region: string;
  countryCodes: string;
  latitude: number;
  longitude: number;
  severity: Severity;
  conflictType: ConflictType;
  status: ConflictStatus;
  startDate: string;
  summary: string;
  casualtyEstimate: number | null;
  displacedEstimate: number | null;
  involvedParties: string;
  thumbnailUrl: string | null;
  articleCount: number;
  eventCount: number;
  updatedAt: string;
}

export interface NewsArticle {
  id: string;
  conflictId: string;
  title: string;
  url: string;
  sourceName: string;
  sourceDomain: string;
  author: string | null;
  description: string | null;
  publishedAt: string;
  reliabilityScore: number;
  reliabilityLabel: 'High' | 'Medium' | 'Low';
  reliabilityColor: 'green' | 'yellow' | 'red';
  sentiment: SentimentType;
  imageUrl: string | null;
  fetchedAt: string;
}

export interface ConflictEvent {
  id: string;
  conflictId: string;
  eventDate: string;
  eventType: string;
  description: string;
  sourceUrl: string | null;
  latitude: number | null;
  longitude: number | null;
  fatalitiesReported: number | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ConflictStats {
  conflictId: string;
  totalArticles: number;
  totalEvents: number;
  articlesBySource: Record<string, number>;
  sentimentBreakdown: Record<string, number>;
  eventsByType: Record<string, number>;
  monthlyTrend: { month: string; incidents: number; casualties: number }[];
  averageReliabilityScore: number;
}
