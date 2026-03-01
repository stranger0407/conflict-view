-- V1: Initial schema

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Conflicts table
CREATE TABLE conflicts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    region VARCHAR(100),
    country_codes VARCHAR(50),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    conflict_type VARCHAR(30) NOT NULL CHECK (conflict_type IN ('WAR','CIVIL_UNREST','TERRORISM','POLITICAL','HUMANITARIAN','BORDER_DISPUTE','COUP','OTHER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','MONITORING','CEASEFIRE','RESOLVED')),
    start_date DATE,
    summary TEXT,
    casualty_estimate INTEGER,
    displaced_estimate INTEGER,
    thumbnail_url VARCHAR(1000),
    involved_parties VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- News sources table
CREATE TABLE news_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    domain VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    reliability_score INTEGER NOT NULL CHECK (reliability_score BETWEEN 0 AND 100),
    bias_rating VARCHAR(50),
    category VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN' CHECK (category IN ('MAINSTREAM','INDEPENDENT','GOVERNMENT','WIRE_SERVICE','UNKNOWN')),
    country VARCHAR(100),
    logo_url VARCHAR(500)
);

-- News articles table
CREATE TABLE news_articles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL UNIQUE,
    source_name VARCHAR(200),
    source_domain VARCHAR(200),
    author VARCHAR(200),
    description TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    reliability_score INTEGER CHECK (reliability_score BETWEEN 0 AND 100),
    sentiment VARCHAR(10) NOT NULL DEFAULT 'NEUTRAL' CHECK (sentiment IN ('POSITIVE','NEUTRAL','NEGATIVE')),
    image_url VARCHAR(1000),
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Conflict events table
CREATE TABLE conflict_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE CASCADE,
    event_date DATE NOT NULL,
    event_type VARCHAR(100),
    description TEXT,
    source_url VARCHAR(1000),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    fatalities_reported INTEGER
);

-- Indexes
CREATE INDEX idx_conflicts_status ON conflicts(status);
CREATE INDEX idx_conflicts_severity ON conflicts(severity);
CREATE INDEX idx_conflicts_region ON conflicts(region);
CREATE INDEX idx_articles_conflict_id ON news_articles(conflict_id);
CREATE INDEX idx_articles_published_at ON news_articles(published_at DESC);
CREATE INDEX idx_articles_source_domain ON news_articles(source_domain);
CREATE INDEX idx_events_conflict_id ON conflict_events(conflict_id);
CREATE INDEX idx_events_event_date ON conflict_events(event_date DESC);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_conflicts_updated_at
    BEFORE UPDATE ON conflicts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
