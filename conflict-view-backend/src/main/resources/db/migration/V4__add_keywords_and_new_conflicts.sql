-- V4: Add keywords column to conflicts for better article matching + add new conflicts

ALTER TABLE conflicts ADD COLUMN keywords TEXT;

-- Update all existing conflicts with comprehensive keyword sets
-- Each keyword set includes: country names, city names, key actors, alternate names, abbreviations

UPDATE conflicts SET keywords = 'russia,ukraine,kyiv,kiev,moscow,kremlin,zelensky,zelenskyy,putin,donbas,donbass,donetsk,luhansk,crimea,kherson,zaporizhzhia,bakhmut,avdiivka,kursk,drone strike ukraine,wagner,nato ukraine,russian invasion'
WHERE name = 'Russia-Ukraine War';

UPDATE conflicts SET keywords = 'israel,gaza,hamas,palestinian,west bank,idf,netanyahu,tel aviv,jerusalem,rafah,khan younis,hezbollah,hostage,october 7,oct 7,ceasefire gaza,genocide,nakba,intifada,al-aqsa,iron dome'
WHERE name = 'Israel-Gaza Conflict';

UPDATE conflicts SET keywords = 'syria,syrian,damascus,aleppo,assad,hts,idlib,sdf,kurdish syria,isis syria,turkey syria,russian syria'
WHERE name = 'Syrian Civil War';

UPDATE conflicts SET keywords = 'yemen,houthi,houthis,sanaa,aden,saudi yemen,ansar allah,red sea attacks,bab al-mandab,marib'
WHERE name = 'Yemen Civil War';

UPDATE conflicts SET keywords = 'iraq,iraqi,baghdad,isis iraq,militia iraq,pmu,hashd,erbil,kirkuk,mosul,iran iraq militia'
WHERE name = 'Iraq Instability';

UPDATE conflicts SET keywords = 'sudan,sudanese,khartoum,darfur,rsf,rapid support,hemedti,al-burhan,saf sudan,janjaweed,el-fasher,wad madani'
WHERE name = 'Sudan Civil War';

UPDATE conflicts SET keywords = 'congo,drc,congolese,goma,m23,kivu,fdlr,adf,monusco,tshisekedi,rwanda congo,bukavu,eastern congo'
WHERE name = 'DRC Eastern Conflict';

UPDATE conflicts SET keywords = 'ethiopia,ethiopian,tigray,amhara,fano,addis ababa,abiy ahmed,tplf,oromia,ola,eritrea ethiopia'
WHERE name = 'Ethiopia Tigray/Amhara';

UPDATE conflicts SET keywords = 'somalia,somali,mogadishu,al-shabaab,al shabaab,alshabaab,atmis,amisom,puntland,jubaland'
WHERE name = 'Somalia Al-Shabaab Insurgency';

UPDATE conflicts SET keywords = 'sahel,mali,malian,burkina faso,burkinabe,niger,nigerien,jnim,isis sahel,bamako,ouagadougou,niamey,wagner sahel,wagner africa,coup sahel'
WHERE name = 'Sahel Jihadist Insurgency';

UPDATE conflicts SET keywords = 'mozambique,cabo delgado,palma,insurgency mozambique,isis mozambique,ansar al-sunna,samim,rwandan forces mozambique,lng mozambique'
WHERE name = 'Mozambique Insurgency (Cabo Delgado)';

UPDATE conflicts SET keywords = 'myanmar,burma,burmese,junta myanmar,rohingya,rakhine,shan,kachin,pdf myanmar,resistance myanmar,sac myanmar,naypyidaw,mandalay,yangon coup'
WHERE name = 'Myanmar Civil War';

UPDATE conflicts SET keywords = 'afghanistan,afghan,taliban,kabul,isis-k,iskp,isis khorasan,nrf afghanistan,women afghanistan,panjshir,kandahar,helmand'
WHERE name = 'Afghanistan Taliban Rule';

UPDATE conflicts SET keywords = 'pakistan,ttp,tehrik-i-taliban,balochistan,bla,waziristan,karachi bombing,peshawar attack,isil pakistan,islamabad'
WHERE name = 'Pakistan TTP Insurgency';

UPDATE conflicts SET keywords = 'haiti,haitian,port-au-prince,gang haiti,viv ansanm,transitional council haiti,mss haiti,kenyan police haiti'
WHERE name = 'Haiti Political Violence';

UPDATE conflicts SET keywords = 'mexico,cartel,sinaloa,cjng,jalisco,fentanyl,narco,drug war,juarez,tijuana,michoacan,guanajuato,zetas'
WHERE name = 'Mexico Cartel Violence';

UPDATE conflicts SET keywords = 'kashmir,kashmiri,india pakistan,loc,line of control,jammu,srinagar,pahalgam,militant kashmir,ceasefire violation'
WHERE name = 'India-Pakistan Kashmir';

UPDATE conflicts SET keywords = 'taiwan,china taiwan,taiwan strait,pla,chinese military,tsai,lai ching-te,taipei,cross-strait,reunification,invasion taiwan'
WHERE name = 'China-Taiwan Tensions';

-- Add new conflicts that are missing

INSERT INTO conflicts (id, name, region, country_codes, latitude, longitude, severity, conflict_type, status, start_date, summary, casualty_estimate, displaced_estimate, involved_parties, keywords) VALUES

(uuid_generate_v4(), 'Iran-Israel-US Confrontation', 'Middle East', 'IR,IL,US', 32.4, 53.7, 'CRITICAL', 'WAR', 'ACTIVE', '2024-04-01',
 'Direct military confrontation between Iran, Israel, and the United States. Escalated from proxy conflicts through Iranian missile attacks on Israel and US-Israeli strikes on Iranian nuclear and military facilities in 2025-2026.',
 NULL, NULL, 'Iran, Israel, United States, IRGC, Hezbollah',
 'iran,iranian,tehran,isfahan,irgc,khamenei,raisi,pezeshkian,israel iran,us iran,strikes iran,nuclear iran,retaliation iran,iranian missile,iranian drone,supreme leader,revolutionary guard,natanz,fordow,bushehr,persian gulf'),

(uuid_generate_v4(), 'Lebanon-Hezbollah Conflict', 'Middle East', 'LB,IL', 33.9, 35.5, 'HIGH', 'WAR', 'ACTIVE', '2023-10-08',
 'Escalation along the Israel-Lebanon border following the Israel-Gaza war. Involves Israeli strikes on Hezbollah targets in Lebanon and Hezbollah rocket attacks on northern Israel. Pager explosions and senior leader assassinations in 2024.',
 5000, 1500000, 'Hezbollah, Israel, Lebanese Armed Forces, UNIFIL',
 'lebanon,lebanese,hezbollah,nasrallah,beirut,south lebanon,unifil,litani,pager explosion,northern israel,tyre,sidon,baalbek,dahiyeh,nabatieh'),

(uuid_generate_v4(), 'Red Sea/Houthi Maritime Attacks', 'Middle East', 'YE', 13.0, 42.5, 'HIGH', 'TERRORISM', 'ACTIVE', '2023-11-19',
 'Houthi attacks on commercial shipping in the Red Sea and Gulf of Aden. US and UK military operations against Houthi positions in Yemen in response. Major disruption to global trade.',
 NULL, NULL, 'Houthis, US Navy, UK Royal Navy, EU NAVFOR',
 'red sea,houthi ship,bab el-mandeb,suez,aden gulf,shipping attack,uss,commercial vessel,cargo ship attack,prosperity guardian,maritime security,aspides,yemen ship,houthi missile ship,houthi drone ship'),

(uuid_generate_v4(), 'West Africa Coups & Instability', 'Africa', 'GN,TD,GA,NE', 9.0, 2.0, 'HIGH', 'COUP', 'ACTIVE', '2020-09-05',
 'Series of military coups across West Africa (Guinea, Mali, Burkina Faso, Niger, Gabon, Chad). Departure of French forces, growing Russian/Wagner influence, democratic backsliding.',
 NULL, NULL, 'Military juntas, ECOWAS, Wagner Group, France',
 'coup africa,ecowas,military junta,guinea coup,chad coup,gabon coup,niger coup,french troops africa,wagner africa,russia africa,sahel governance'),

(uuid_generate_v4(), 'Philippines-China South China Sea', 'Asia', 'PH,CN', 11.0, 117.0, 'MEDIUM', 'BORDER_DISPUTE', 'ACTIVE', '2012-04-10',
 'Escalating confrontations between Philippine and Chinese vessels in the South China Sea, particularly around Second Thomas Shoal and Scarborough Shoal. Water cannon incidents and collisions.',
 NULL, NULL, 'Philippines, China, US (support), ASEAN',
 'south china sea,philippines china,second thomas shoal,scarborough shoal,spratly,ayungin,west philippine sea,chinese coast guard,marcos china,manila beijing,sierra madre'),

(uuid_generate_v4(), 'Colombia Armed Groups', 'Americas', 'CO', 4.6, -74.1, 'MEDIUM', 'CIVIL_UNREST', 'ACTIVE', '1964-05-27',
 'Ongoing violence involving ELN, FARC dissidents, and criminal organizations despite 2016 peace deal. Total Peace policy negotiations ongoing with mixed results.',
 NULL, 8000000, 'ELN, FARC dissidents (EMC), Colombian Military, Criminal groups',
 'colombia,colombian,bogota,eln,farc,emc,petro,total peace,paz total,dissident,narco colombia,catatumbo,cauca,arauca');
