package com.conflictview.service;

import com.conflictview.model.Conflict;
import com.conflictview.model.OsintResource;
import com.conflictview.model.enums.ConflictStatus;
import com.conflictview.model.enums.ResourceType;
import com.conflictview.repository.ConflictRepository;
import com.conflictview.repository.OsintResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fetches conflict background intelligence from Wikipedia REST API.
 * Creates REPORT-type OSINT resources with summaries and lead images.
 * Free, no API key required - just needs User-Agent header.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WikipediaSummaryOsintService {

    private final RestTemplate restTemplate;
    private final ConflictRepository conflictRepository;
    private final OsintResourceRepository osintResourceRepository;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";

    // Map conflict names to Wikipedia article titles
    private static final Map<String, String[]> CONFLICT_ARTICLES = Map.ofEntries(
            Map.entry("Russia-Ukraine", new String[]{"Russian_invasion_of_Ukraine", "Russo-Ukrainian_War", "Siege_of_Mariupol", "Battle_of_Bakhmut"}),
            Map.entry("Israel-Gaza", new String[]{"Israel–Hamas_war", "Gaza_Strip", "2023_Hamas-led_attack_on_Israel", "Israeli_blockade_of_the_Gaza_Strip"}),
            Map.entry("Syria", new String[]{"Syrian_civil_war", "Syrian_Democratic_Forces", "Iranian_intervention_in_the_Syrian_civil_war"}),
            Map.entry("Sudan", new String[]{"War_in_Sudan_(2023–present)", "Rapid_Support_Forces", "Sudanese_Armed_Forces"}),
            Map.entry("Yemen", new String[]{"Yemeni_civil_war_(2014–present)", "Houthi_movement", "Saudi-led_intervention_in_Yemen"}),
            Map.entry("Iraq", new String[]{"Iraqi_conflict_(2003–present)", "Islamic_State_of_Iraq_and_the_Levant", "Popular_Mobilization_Forces"}),
            Map.entry("Afghanistan", new String[]{"Taliban", "War_in_Afghanistan_(2001–2021)", "Islamic_State_–_Khorasan_Province"}),
            Map.entry("DRC", new String[]{"Kivu_conflict", "M23_(armed_group)", "Allied_Democratic_Forces"}),
            Map.entry("Sahel", new String[]{"Islamist_insurgency_in_the_Sahel", "Jama'at_Nasr_al-Islam_wal_Muslimin", "Wagner_Group_in_Africa"}),
            Map.entry("Somalia", new String[]{"Al-Shabaab_(militant_group)", "Somali_Civil_War_(2009–present)"}),
            Map.entry("Myanmar", new String[]{"Myanmar_civil_war_(2021–present)", "Tatmadaw", "National_Unity_Government_(Myanmar)"}),
            Map.entry("Ethiopia", new String[]{"Tigray_War", "Tigray_People's_Liberation_Front"}),
            Map.entry("Haiti", new String[]{"Crisis_in_Haiti", "Gangs_in_Haiti"}),
            Map.entry("Mexico", new String[]{"Mexican_drug_war", "Sinaloa_Cartel", "Jalisco_New_Generation_Cartel"}),
            Map.entry("Pakistan", new String[]{"Tehrik-i-Taliban_Pakistan", "Insurgency_in_Balochistan"}),
            Map.entry("Colombia", new String[]{"Colombian_conflict", "National_Liberation_Army_(Colombia)"}),
            Map.entry("Mozambique", new String[]{"Insurgency_in_Cabo_Delgado"}),
            Map.entry("Philippines", new String[]{"South_China_Sea_dispute", "Territorial_disputes_in_the_South_China_Sea"}),
            Map.entry("Lebanon", new String[]{"Hezbollah", "2024_Israeli_invasion_of_Lebanon"}),
            Map.entry("Houthi", new String[]{"Houthi_movement", "Red_Sea_crisis"}),
            Map.entry("Red Sea", new String[]{"Red_Sea_crisis", "Houthi_attacks_on_shipping"}),
            Map.entry("Iran", new String[]{"Iran–Israel_proxy_conflict", "Quds_Force"})
    );

    public void fetchForAllConflicts() {
        List<Conflict> conflicts = conflictRepository.findByStatus(ConflictStatus.ACTIVE);
        for (Conflict conflict : conflicts) {
            try {
                fetchForConflict(conflict);
                Thread.sleep(200); // Rate limit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Wikipedia summary error for '{}': {}", conflict.getName(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchForConflict(Conflict conflict) {
        String[] articles = findArticlesForConflict(conflict);
        if (articles == null) return;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "ConflictView/1.0 (https://github.com/stranger0407/conflict-view; conflict-view-app)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        int saved = 0;
        for (String articleTitle : articles) {
            if (saved >= 4) break; // Max 4 articles per conflict

            String url = BASE_URL + articleTitle.replace(" ", "_");
            String wikiUrl = "https://en.wikipedia.org/wiki/" + articleTitle.replace(" ", "_");

            if (osintResourceRepository.existsByUrl(wikiUrl)) continue;

            try {
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                String body = responseEntity.getBody();
                if (body == null) continue;

                Map<String, Object> data = objectMapper.readValue(body, Map.class);

                String title = (String) data.get("title");
                String extract = (String) data.get("extract");
                String description = (String) data.get("description");

                // Get thumbnail
                String thumbnailUrl = null;
                Map<String, Object> thumbnail = (Map<String, Object>) data.get("thumbnail");
                if (thumbnail != null) {
                    thumbnailUrl = (String) thumbnail.get("source");
                }

                // Get original image
                String originalImageUrl = null;
                Map<String, Object> originalimage = (Map<String, Object>) data.get("originalimage");
                if (originalimage != null) {
                    originalImageUrl = (String) originalimage.get("source");
                }

                if (extract == null || extract.length() < 50) continue; // Skip stubs

                osintResourceRepository.save(OsintResource.builder()
                        .conflict(conflict)
                        .resourceType(ResourceType.REPORT)
                        .title("[Intel Brief] " + title)
                        .url(wikiUrl)
                        .thumbnailUrl(thumbnailUrl)
                        .description(truncate(extract, 1000))
                        .sourcePlatform("Wikipedia")
                        .author("Wikipedia Contributors")
                        .publishedAt(LocalDateTime.now())
                        .build());
                saved++;

                // Also save the original image if available (as IMAGE type)
                if (originalImageUrl != null && !osintResourceRepository.existsByUrl(originalImageUrl)) {
                    osintResourceRepository.save(OsintResource.builder()
                            .conflict(conflict)
                            .resourceType(ResourceType.IMAGE)
                            .title(title + " — Reference Image")
                            .url(originalImageUrl)
                            .thumbnailUrl(thumbnailUrl != null ? thumbnailUrl : originalImageUrl)
                            .description(description != null ? description : "Wikipedia reference image")
                            .sourcePlatform("Wikipedia")
                            .author("Wikipedia Contributors")
                            .publishedAt(LocalDateTime.now())
                            .build());
                }

                Thread.sleep(100); // Rate limit between articles
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.debug("Wikipedia fetch failed for article '{}': {}", articleTitle, e.getMessage());
            }
        }
        if (saved > 0) {
            log.debug("Wikipedia: saved {} reports for '{}'", saved, conflict.getName());
        }
    }

    private String[] findArticlesForConflict(Conflict conflict) {
        String name = conflict.getName();
        for (Map.Entry<String, String[]> entry : CONFLICT_ARTICLES.entrySet()) {
            if (name.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        // Fallback: try conflict name directly
        return new String[]{name.replace(" ", "_")};
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
