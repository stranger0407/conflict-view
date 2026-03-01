-- V5: Add new news sources for broader coverage

INSERT INTO news_sources (id, domain, name, reliability_score, bias_rating, category, country) VALUES
(uuid_generate_v4(), 'theguardian.com', 'The Guardian', 82, 'CENTER_LEFT', 'MAINSTREAM', 'United Kingdom'),
(uuid_generate_v4(), 'france24.com', 'France 24', 80, 'CENTER', 'MAINSTREAM', 'France'),
(uuid_generate_v4(), 'dw.com', 'Deutsche Welle', 82, 'CENTER', 'MAINSTREAM', 'Germany'),
(uuid_generate_v4(), 'timesofisrael.com', 'The Times of Israel', 72, 'CENTER_RIGHT', 'MAINSTREAM', 'Israel'),
(uuid_generate_v4(), 'middleeasteye.net', 'Middle East Eye', 68, 'CENTER_LEFT', 'INDEPENDENT', 'United Kingdom'),
(uuid_generate_v4(), 'africanews.com', 'Africanews', 72, 'CENTER', 'MAINSTREAM', 'International'),
(uuid_generate_v4(), 'kyivindependent.com', 'Kyiv Independent', 74, 'CENTER', 'INDEPENDENT', 'Ukraine'),
(uuid_generate_v4(), 'reliefweb.int', 'ReliefWeb (UN OCHA)', 90, 'CENTER', 'WIRE_SERVICE', 'International'),
(uuid_generate_v4(), 'crisisgroup.org', 'International Crisis Group', 88, 'CENTER', 'INDEPENDENT', 'International'),
(uuid_generate_v4(), 'insightcrime.org', 'InSight Crime', 78, 'CENTER', 'INDEPENDENT', 'International'),
(uuid_generate_v4(), 'scmp.com', 'South China Morning Post', 70, 'CENTER', 'MAINSTREAM', 'Hong Kong'),
(uuid_generate_v4(), 'iranintl.com', 'Iran International', 62, 'CENTER_RIGHT', 'INDEPENDENT', 'United Kingdom')
ON CONFLICT (domain) DO NOTHING;
