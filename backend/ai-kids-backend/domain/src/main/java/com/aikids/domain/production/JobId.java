package com.aikids.domain.production;

import java.util.Objects;
import java.util.UUID;

public record JobId(UUID value) {

    public JobId {
        Objects.requireNonNull(value);
    }

    public static JobId create() {
        return new JobId(UUID.randomUUID());
    }
}
