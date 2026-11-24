package com.aikids.domain.shared;

import java.util.regex.Pattern;

/** Nombre de carpeta de un recurso. El formato impide rutas arbitrarias ("..", "/", "C:"). */
public record AssetKey(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public AssetKey {
        if (value == null || value.length() > 80 || !FORMAT.matcher(value).matches()) {
            throw new InvalidDataException("Identificador de recurso no válido.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
