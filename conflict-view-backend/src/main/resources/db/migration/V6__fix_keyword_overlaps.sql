-- V6: Fix keyword overlaps between related conflicts

-- Remove "hezbollah" from Israel-Gaza (it belongs to Lebanon conflict)
UPDATE conflicts SET keywords = 'israel,gaza,hamas,palestinian,west bank,idf,netanyahu,tel aviv,jerusalem,rafah,khan younis,hostage,october 7,oct 7,ceasefire gaza,nakba,intifada,al-aqsa,iron dome,gaza war,gaza strip,jabalia,nuseirat'
WHERE name = 'Israel-Gaza Conflict';

-- Make Lebanon keywords more specific with multi-word phrases that score higher
UPDATE conflicts SET keywords = 'lebanon,lebanese,hezbollah,nasrallah,beirut,south lebanon,unifil,litani,pager explosion,northern israel rockets,tyre,sidon,baalbek,dahiyeh,nabatieh,hezbollah israel,hezbollah attack,hezbollah strike,lebanon war,lebanese border'
WHERE name = 'Lebanon-Hezbollah Conflict';

-- Split Yemen vs Red Sea: remove houthi ship/maritime from Yemen, add to Red Sea
UPDATE conflicts SET keywords = 'yemen,yemeni,sanaa,aden,saudi yemen,ansar allah,marib,yemen war,yemen famine,yemen crisis,taiz,hudaydah'
WHERE name = 'Yemen Civil War';

UPDATE conflicts SET keywords = 'red sea,houthi ship,houthi attack,bab el-mandeb,suez,aden gulf,shipping attack,commercial vessel,cargo ship attack,prosperity guardian,maritime security,aspides,houthi missile ship,houthi drone ship,houthi maritime,red sea shipping,red sea attack,houthi navy,merchant vessel,tanker attack'
WHERE name = 'Red Sea/Houthi Maritime Attacks';

-- Somalia: add keyword variations without hyphens
UPDATE conflicts SET keywords = 'somalia,somali,mogadishu,al-shabaab,al shabaab,alshabaab,shabaab,atmis,amisom,puntland,jubaland,somali bomb,somali attack,baidoa,kismayo'
WHERE name = 'Somalia Al-Shabaab Insurgency';

-- Myanmar: add more searchable terms
UPDATE conflicts SET keywords = 'myanmar,burma,burmese,junta myanmar,rohingya,rakhine,shan,kachin,chin,karenni,karen,pdf myanmar,resistance myanmar,naypyidaw,mandalay,yangon,myanmar coup,myanmar military,min aung hlaing,arakan army,myawaddy'
WHERE name = 'Myanmar Civil War';

-- Colombia: add more specific terms
UPDATE conflicts SET keywords = 'colombia,colombian,bogota,eln,farc,emc,petro,paz total,dissident farc,narco colombia,catatumbo,cauca,arauca,estado mayor central,guerrilla colombia,coca colombia'
WHERE name = 'Colombia Armed Groups';
