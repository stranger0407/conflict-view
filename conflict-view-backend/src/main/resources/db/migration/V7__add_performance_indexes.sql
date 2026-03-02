-- V7: Add composite indexes for common query patterns

CREATE INDEX IF NOT EXISTS idx_articles_sentiment ON news_articles(sentiment);
CREATE INDEX IF NOT EXISTS idx_articles_conflict_sentiment ON news_articles(conflict_id, sentiment);
CREATE INDEX IF NOT EXISTS idx_articles_conflict_published ON news_articles(conflict_id, published_at DESC);
