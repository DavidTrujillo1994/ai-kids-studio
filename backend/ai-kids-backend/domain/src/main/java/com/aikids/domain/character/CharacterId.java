package com.aikids.domain.character;

import java.util.Objects;
import java.util.UUID;

public record CharacterId(UUID value) {

    public CharacterId {
        Objects.requireNonNull(value);
    }

    public static CharacterId create() {
        return new CharacterId(UUID.randomUUID());
    }
}
