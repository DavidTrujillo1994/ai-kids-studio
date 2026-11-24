package com.aikids.domain.direction;

import com.aikids.domain.shared.AssetKey;
import java.util.List;
import java.util.Map;

public record DirectionGuide(List<DirectionRule> rules, List<String> question, List<String> exclamation, List<String> neutral,
                             Map<AssetKey, String> environmentGaits) {

    public DirectionGuide {
        rules = List.copyOf(rules);
        question = List.copyOf(question);
        exclamation = List.copyOf(exclamation);
        neutral = List.copyOf(neutral);
        environmentGaits = Map.copyOf(environmentGaits);
    }
}
