package com.aikids.domain.shared;

import java.text.Normalizer;
import java.util.Locale;

public final class Text {

    private Text() {
    }

    public static String required(String value, String field, int max) {
        String clean = value == null ? "" : value.strip();
        if (clean.isEmpty()) {
            throw new InvalidDataException("Falta " + field + ".");
        }
        return limited(clean, field, max);
    }

    public static String optional(String value, String field, int max) {
        return limited(value == null ? "" : value.strip(), field, max);
    }

    private static String limited(String clean, String field, int max) {
        if (clean.length() > max) {
            throw new InvalidDataException("El texto de " + field + " admite hasta " + max + " caracteres.");
        }
        return clean;
    }

    /** Minúsculas y sin tildes. */
    public static String plain(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** "Bruno y la Ñapa" -> "bruno-y-la-napa". */
    public static String slug(String value) {
        String slug = plain(value).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.length() > 40 ? slug.substring(0, 40).replaceAll("-+$", "") : slug;
    }
}
