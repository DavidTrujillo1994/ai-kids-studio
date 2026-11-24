package com.aikids.domain.episode;

import java.time.Instant;

public record Review(Decision decision, String comments, Instant at) {

    public enum Decision {
        APPROVE, REJECT
    }
}
