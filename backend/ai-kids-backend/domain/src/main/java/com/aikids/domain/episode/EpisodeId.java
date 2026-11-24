package com.aikids.domain.episode;

import java.util.Objects;
import java.util.UUID;

public record EpisodeId(UUID value) {

    public EpisodeId {
        Objects.requireNonNull(value);
    }

    public static EpisodeId create() {
        return new EpisodeId(UUID.randomUUID());
    }
}
