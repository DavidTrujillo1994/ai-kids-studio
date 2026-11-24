package com.aikids.domain.production;

public enum JobStatus {
    QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED;

    public boolean active() {
        return this == QUEUED || this == RUNNING;
    }
}
