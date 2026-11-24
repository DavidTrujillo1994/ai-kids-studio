package com.aikids.domain.series;

import com.aikids.domain.shared.Text;
import java.time.Instant;

public final class Series {

    private final SeriesId id;
    private final Instant createdAt;
    private String name;
    private String description;
    private Instant updatedAt;

    public Series(SeriesId id, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        assign(name, description);
    }

    public static Series create(String name, String description, Instant now) {
        return new Series(SeriesId.create(), name, description, now, now);
    }

    public void update(String newName, String newDescription, Instant now) {
        assign(newName, newDescription);
        updatedAt = now;
    }

    private void assign(String newName, String newDescription) {
        name = Text.required(newName, "el nombre de la serie", 80);
        description = Text.optional(newDescription, "la descripción", 500);
    }

    public SeriesId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
