package com.aikids.domain.direction;

import java.util.List;
import java.util.regex.Pattern;

/** Si la frase contiene alguna de las palabras, sugiere cómo actuarla. gait puede ser null. */
public record DirectionRule(List<String> words, List<String> animations, String expression, String gait) {

    public DirectionRule {
        words = List.copyOf(words);
        animations = List.copyOf(animations);
    }

    /** plainText va en minúsculas y sin tildes; cada palabra vale como comienzo de palabra. */
    boolean matches(String plainText) {
        return words.stream().anyMatch(w -> Pattern.compile("(?<![a-z])" + Pattern.quote(w)).matcher(plainText).find());
    }
}
