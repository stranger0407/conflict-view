-- Add new columns for advanced OSINT data (geo, fatalities, event type, confidence)
ALTER TABLE osint_resources ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE osint_resources ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE osint_resources ADD COLUMN fatalities INTEGER;
ALTER TABLE osint_resources ADD COLUMN event_type VARCHAR(100);
ALTER TABLE osint_resources ADD COLUMN confidence INTEGER;

-- Update CHECK constraint to include new resource types
ALTER TABLE osint_resources DROP CONSTRAINT IF EXISTS osint_resources_resource_type_check;
ALTER TABLE osint_resources ADD CONSTRAINT osint_resources_resource_type_check
    CHECK (resource_type IN ('VIDEO','IMAGE','MAP','INFOGRAPHIC','REPORT','SATELLITE','EVENT_DATA'));

-- Index for geo queries
CREATE INDEX idx_osint_geo ON osint_resources(latitude, longitude) WHERE latitude IS NOT NULL;
