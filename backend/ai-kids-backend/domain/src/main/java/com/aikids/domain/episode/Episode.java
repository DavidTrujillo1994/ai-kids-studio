package com.aikids.domain.episode;

import com.aikids.domain.script.Script;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidStateException;
import com.aikids.domain.shared.Text;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Agregado principal: guarda el guion y controla el ciclo de vida editorial. */
public final class Episode {

    private final EpisodeId id;
    private final SeriesId seriesId;
    private final AssetKey folder;
    private final Instant createdAt;
    private final List<Review> reviews;
    private String title;
    private String idea;
    private AssetKey environment;
    private Script script;
    private EpisodeStatus status;
    private Instant updatedAt;

    public Episode(EpisodeId id, SeriesId seriesId, AssetKey folder, String title, String idea, AssetKey environment,
                   Script script, EpisodeStatus status, List<Review> reviews, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.seriesId = seriesId;
        this.folder = folder;
        this.environment = environment;
        this.script = script;
        this.status = status;
        this.reviews = new ArrayList<>(reviews);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        describe(title, idea);
    }

    public static Episode create(SeriesId seriesId, String title, String idea, AssetKey environment, Script script, Instant now) {
        EpisodeId id = EpisodeId.create();
        String cleanTitle = Text.required(title, "el título", 80);
        String slug = Text.slug(cleanTitle);
        AssetKey folder = new AssetKey((slug.isEmpty() ? "episodio" : slug) + "-" + id.value().toString().substring(0, 8));
        EpisodeStatus status = script.isEmpty() ? EpisodeStatus.DRAFT : EpisodeStatus.SCRIPT_READY;
        return new Episode(id, seriesId, folder, cleanTitle, idea, environment, script, status, List.of(), now, now);
    }

    public void edit(String newTitle, String newIdea, AssetKey newEnvironment, Script newScript, Instant now) {
        if (!status.editable()) {
            throw new InvalidStateException(status == EpisodeStatus.APPROVED
                    ? "El episodio está aprobado y ya no se puede modificar."
                    : "Espera a que termine la producción para editar.");
        }
        describe(newTitle, newIdea);
        boolean videoOutdated = !newEnvironment.equals(environment) || !newScript.equals(script);
        environment = newEnvironment;
        script = newScript;
        if (videoOutdated) {
            EpisodeStatus next = newScript.isEmpty() ? EpisodeStatus.DRAFT : EpisodeStatus.SCRIPT_READY;
            if (status != next) {
                status = next;
            }
        }
        updatedAt = now;
    }

    public void startProduction(Instant now) {
        moveTo(EpisodeStatus.IN_PRODUCTION, now);
    }

    public void productionFinished(Instant now) {
        moveTo(EpisodeStatus.PENDING_APPROVAL, now);
    }

    public void productionFailed(Instant now) {
        moveTo(EpisodeStatus.FAILED, now);
    }

    public void productionCancelled(Instant now) {
        moveTo(EpisodeStatus.SCRIPT_READY, now);
    }

    public void approve(String comments, Instant now) {
        moveTo(EpisodeStatus.APPROVED, now);
        reviews.add(new Review(Review.Decision.APPROVE, Text.optional(comments, "los comentarios", 1000), now));
    }

    public void reject(String comments, Instant now) {
        String reason = Text.required(comments, "qué se debe mejorar", 1000);
        moveTo(EpisodeStatus.REJECTED, now);
        reviews.add(new Review(Review.Decision.REJECT, reason, now));
    }

    public void requireExportable() {
        if (status != EpisodeStatus.APPROVED) {
            throw new InvalidStateException("Solo se exportan episodios aprobados.");
        }
    }

    public void requireDeletable() {
        if (status == EpisodeStatus.IN_PRODUCTION) {
            throw new InvalidStateException("Cancela la producción antes de quitar el episodio.");
        }
    }

    private void moveTo(EpisodeStatus next, Instant now) {
        if (!status.canBecome(next)) {
            throw new InvalidStateException("Un episodio en estado " + status + " no puede pasar a " + next + ".");
        }
        status = next;
        updatedAt = now;
    }

    private void describe(String newTitle, String newIdea) {
        title = Text.required(newTitle, "el título", 80);
        idea = Text.optional(newIdea, "la idea", 1000);
    }

    public EpisodeId id() {
        return id;
    }

    public SeriesId seriesId() {
        return seriesId;
    }

    public AssetKey folder() {
        return folder;
    }

    public String title() {
        return title;
    }

    public String idea() {
        return idea;
    }

    public AssetKey environment() {
        return environment;
    }

    public Script script() {
        return script;
    }

    public EpisodeStatus status() {
        return status;
    }

    public List<Review> reviews() {
        return List.copyOf(reviews);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
