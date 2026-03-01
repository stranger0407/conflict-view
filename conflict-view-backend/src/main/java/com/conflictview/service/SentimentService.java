package com.conflictview.service;

import com.conflictview.model.enums.SentimentType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SentimentService {

    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "killed", "dead", "death", "deaths", "casualties", "wounded", "injured",
            "attack", "attacks", "bombing", "bomb", "missile", "airstrike", "strike",
            "war", "battle", "fighting", "clashes", "violence", "massacre", "genocide",
            "destruction", "destroyed", "refugee", "famine", "crisis", "disaster",
            "terror", "terrorist", "explosion", "shelling", "occupation", "invasion",
            "siege", "blockade", "hostage", "kidnap", "abduct", "torture", "execution"
    );

    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "ceasefire", "peace", "agreement", "deal", "negotiate", "talks",
            "humanitarian", "aid", "relief", "reconstruction", "rebuild",
            "hope", "progress", "diplomatic", "compromise", "reconciliation",
            "truce", "withdrawal", "elections", "democracy", "freedom"
    );

    public SentimentType analyze(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        int negScore = 0;
        int posScore = 0;

        for (String kw : NEGATIVE_KEYWORDS) {
            if (combined.contains(kw)) negScore++;
        }
        for (String kw : POSITIVE_KEYWORDS) {
            if (combined.contains(kw)) posScore++;
        }

        if (negScore > posScore + 1) return SentimentType.NEGATIVE;
        if (posScore > negScore + 1) return SentimentType.POSITIVE;
        return SentimentType.NEUTRAL;
    }
}
