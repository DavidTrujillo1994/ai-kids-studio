package com.aikids.domain.series;

import java.util.Objects;
import java.util.UUID;

public record SeriesId(UUID value) {

    public SeriesId {
        Objects.requireNonNull(value);
    }

    public static SeriesId create() {
        return new SeriesId(UUID.randomUUID());
    }
}
