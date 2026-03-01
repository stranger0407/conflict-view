-- V3: Seed active conflicts

INSERT INTO conflicts (id, name, region, country_codes, latitude, longitude, severity, conflict_type, status, start_date, summary, casualty_estimate, displaced_estimate, involved_parties) VALUES

-- Europe
(uuid_generate_v4(), 'Russia-Ukraine War', 'Eastern Europe', 'RU,UA', 49.0, 32.0, 'CRITICAL', 'WAR', 'ACTIVE', '2022-02-24',
 'Full-scale Russian invasion of Ukraine launched in February 2022 following years of escalating tensions. The conflict involves large-scale ground, air, and naval operations across eastern and southern Ukraine.',
 500000, 8000000, 'Russia, Ukraine, NATO allies (support)'),

-- Middle East
(uuid_generate_v4(), 'Israel-Gaza Conflict', 'Middle East', 'IL,PS', 31.5, 34.5, 'CRITICAL', 'WAR', 'ACTIVE', '2023-10-07',
 'Major escalation following Hamas attacks on Israel on October 7, 2023, resulting in ongoing Israeli military operations in Gaza. Significant humanitarian crisis with high civilian casualties.',
 50000, 1900000, 'Israel, Hamas, Palestinian Islamic Jihad'),

(uuid_generate_v4(), 'Syrian Civil War', 'Middle East', 'SY', 34.8, 39.0, 'HIGH', 'CIVIL_UNREST', 'ACTIVE', '2011-03-15',
 'Ongoing multi-sided conflict involving government forces, various rebel factions, Kurdish groups, and foreign powers. Large humanitarian crisis with millions displaced.',
 600000, 5500000, 'Syrian Government, HTS, SDF, Turkey, Russia, US'),

(uuid_generate_v4(), 'Yemen Civil War', 'Middle East', 'YE', 15.5, 48.0, 'HIGH', 'CIVIL_UNREST', 'ACTIVE', '2014-09-21',
 'Complex civil war between the internationally recognized government (backed by Saudi-led coalition) and Houthi forces. One of the world''s worst humanitarian crises.',
 450000, 4000000, 'Houthis, Yemeni Government, Saudi-led Coalition, UAE'),

(uuid_generate_v4(), 'Iraq Instability', 'Middle East', 'IQ', 33.0, 44.0, 'MEDIUM', 'TERRORISM', 'ACTIVE', '2013-01-01',
 'Ongoing security challenges including ISIS remnants, militia activities, and political instability following years of conflict. Periodic attacks and tensions.',
 NULL, 1200000, 'ISIS remnants, Iran-backed militias, Iraqi Security Forces, US forces'),

-- Africa
(uuid_generate_v4(), 'Sudan Civil War', 'Africa', 'SD', 15.5, 32.5, 'CRITICAL', 'CIVIL_UNREST', 'ACTIVE', '2023-04-15',
 'Conflict between the Sudanese Armed Forces and the Rapid Support Forces (RSF) that erupted in April 2023. One of the world''s fastest-growing humanitarian crises.',
 15000, 8000000, 'Sudanese Armed Forces (SAF), Rapid Support Forces (RSF)'),

(uuid_generate_v4(), 'DRC Eastern Conflict', 'Africa', 'CD', -1.5, 29.0, 'HIGH', 'CIVIL_UNREST', 'ACTIVE', '1996-01-01',
 'Long-running conflict in eastern Democratic Republic of Congo involving dozens of armed groups. Recent escalation with M23 rebel advances backed by Rwanda.',
 NULL, 7000000, 'DRC Government, M23, FDLR, ADF, Rwanda (alleged), MONUSCO'),

(uuid_generate_v4(), 'Ethiopia Tigray/Amhara', 'Africa', 'ET', 10.0, 38.5, 'HIGH', 'CIVIL_UNREST', 'MONITORING', '2020-11-03',
 'Conflict following the Tigray War (2020-2022) with ongoing instability. Peace deal signed in November 2022 but tensions and ethnic violence persist in Amhara and other regions.',
 500000, 2000000, 'Ethiopian Government, Tigray Forces, Amhara Fano, Oromo armed groups'),

(uuid_generate_v4(), 'Somalia Al-Shabaab Insurgency', 'Africa', 'SO', 6.0, 46.0, 'HIGH', 'TERRORISM', 'ACTIVE', '2006-01-01',
 'Al-Shabaab insurgency against the Somali Federal Government with regular attacks, assassinations, and territorial control in rural areas. Ongoing AMISOM/ATMIS support.',
 NULL, 3000000, 'Al-Shabaab, Somali Federal Government, ATMIS, US Forces'),

(uuid_generate_v4(), 'Sahel Jihadist Insurgency', 'Africa', 'ML,BF,NE', 15.0, -2.0, 'HIGH', 'TERRORISM', 'ACTIVE', '2012-01-01',
 'Jihadist insurgency spreading across Mali, Burkina Faso, and Niger. Involves JNIM and ISIS-Sahel. Military coups in all three countries have complicated regional response.',
 NULL, 2800000, 'JNIM, ISIS-Sahel, Mali Military, Burkina Faso Military, Niger Military, Wagner Group'),

(uuid_generate_v4(), 'Mozambique Insurgency (Cabo Delgado)', 'Africa', 'MZ', -12.0, 40.0, 'HIGH', 'TERRORISM', 'ACTIVE', '2017-10-05',
 'Islamist insurgency in northern Mozambique''s Cabo Delgado province, linked to ISIS. Significant displacement and humanitarian impact near major LNG projects.',
 4000, 1000000, 'ISIS-Mozambique (Ansar al-Sunna), Mozambique Government, SAMIM, Rwandan Forces'),

-- Asia
(uuid_generate_v4(), 'Myanmar Civil War', 'Asia', 'MM', 19.0, 96.0, 'HIGH', 'CIVIL_UNREST', 'ACTIVE', '2021-02-01',
 'Armed conflict following the February 2021 military coup. Growing People''s Defence Force resistance allied with ethnic armed organizations. Significant territory under rebel control.',
 50000, 2600000, 'Myanmar Military (SAC), People''s Defence Force, Ethnic Armed Organizations'),

(uuid_generate_v4(), 'Afghanistan Taliban Rule', 'Asia', 'AF', 33.0, 65.0, 'HIGH', 'POLITICAL', 'ACTIVE', '2021-08-15',
 'Taliban governance following US withdrawal in 2021. Ongoing ISIS-K attacks, economic collapse, severe restrictions on women, and intermittent operations against remnant resistance.',
 NULL, 3500000, 'Taliban Government, ISIS-K, NRF remnants'),

(uuid_generate_v4(), 'Pakistan TTP Insurgency', 'Asia', 'PK', 33.5, 70.5, 'MEDIUM', 'TERRORISM', 'ACTIVE', '2007-12-14',
 'Tehrik-i-Taliban Pakistan (TTP) insurgency with increased attacks following Taliban control of Afghanistan. Cross-border tensions with Afghanistan.',
 NULL, 500000, 'TTP, Pakistan Security Forces, BLA'),

-- Americas
(uuid_generate_v4(), 'Haiti Political Violence', 'Americas', 'HT', 19.0, -72.5, 'HIGH', 'CIVIL_UNREST', 'ACTIVE', '2021-07-07',
 'Severe gang violence and political instability following the assassination of President Moïse. Gang coalitions control large parts of Port-au-Prince causing a humanitarian crisis.',
 5000, 700000, 'Gang coalitions (Viv Ansanm), Haitian Police, MSS (Kenyan-led)'),

(uuid_generate_v4(), 'Mexico Cartel Violence', 'Americas', 'MX', 23.0, -102.0, 'HIGH', 'TERRORISM', 'ACTIVE', '2006-12-11',
 'Ongoing cartel conflicts causing high levels of homicide across multiple states. Major cartels including CJNG and Sinaloa fight for territorial control and trafficking routes.',
 NULL, NULL, 'CJNG, Sinaloa Cartel, Los Zetas remnants, Mexican Security Forces'),

-- South/Southeast Asia
(uuid_generate_v4(), 'India-Pakistan Kashmir', 'Asia', 'IN,PK', 34.3, 74.3, 'MEDIUM', 'BORDER_DISPUTE', 'MONITORING', '1947-08-14',
 'Long-standing territorial dispute over Kashmir between India and Pakistan, both nuclear-armed states. Periodic militant attacks, cross-border firing, and diplomatic tensions.',
 NULL, NULL, 'India, Pakistan, Kashmiri militants'),

(uuid_generate_v4(), 'China-Taiwan Tensions', 'Asia', 'CN,TW', 23.7, 120.9, 'MEDIUM', 'POLITICAL', 'MONITORING', '1949-12-07',
 'Escalating military tensions across the Taiwan Strait with increased PLA military exercises and incursions into Taiwan''s Air Defense Identification Zone.',
 NULL, NULL, 'China (PRC), Taiwan (ROC), US (support)');
