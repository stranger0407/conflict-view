-- V8: Add OSINT resources table for videos, images, maps, infographics, reports

CREATE TABLE osint_resources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE CASCADE,
    resource_type VARCHAR(20) NOT NULL CHECK (resource_type IN ('VIDEO','IMAGE','MAP','INFOGRAPHIC','REPORT')),
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL UNIQUE,
    thumbnail_url VARCHAR(1000),
    description TEXT,
    source_platform VARCHAR(100) NOT NULL,
    author VARCHAR(200),
    published_at TIMESTAMP WITH TIME ZONE,
    duration VARCHAR(20),
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_osint_conflict_type ON osint_resources(conflict_id, resource_type);
CREATE INDEX idx_osint_conflict_published ON osint_resources(conflict_id, published_at DESC);
