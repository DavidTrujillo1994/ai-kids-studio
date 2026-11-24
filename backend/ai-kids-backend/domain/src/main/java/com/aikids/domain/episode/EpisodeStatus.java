package com.aikids.domain.episode;

import java.util.Map;
import java.util.Set;

public enum EpisodeStatus {
    DRAFT, SCRIPT_READY, IN_PRODUCTION, PENDING_APPROVAL, APPROVED, REJECTED, FAILED;

    private static final Map<EpisodeStatus, Set<EpisodeStatus>> NEXT = Map.of(
            DRAFT, Set.of(SCRIPT_READY),
            SCRIPT_READY, Set.of(DRAFT, IN_PRODUCTION),
            IN_PRODUCTION, Set.of(PENDING_APPROVAL, FAILED, SCRIPT_READY),
            PENDING_APPROVAL, Set.of(APPROVED, REJECTED, SCRIPT_READY, IN_PRODUCTION),
            REJECTED, Set.of(SCRIPT_READY, DRAFT, IN_PRODUCTION),
            FAILED, Set.of(SCRIPT_READY, DRAFT, IN_PRODUCTION),
            APPROVED, Set.of());

    public boolean canBecome(EpisodeStatus next) {
        return NEXT.get(this).contains(next);
    }

    public boolean editable() {
        return this != APPROVED && this != IN_PRODUCTION;
    }
}
