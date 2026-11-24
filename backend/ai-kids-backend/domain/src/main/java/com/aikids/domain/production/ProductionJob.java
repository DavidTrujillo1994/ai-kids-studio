package com.aikids.domain.production;

import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.shared.InvalidStateException;
import java.time.Instant;

public final class ProductionJob {

    private final JobId id;
    private final EpisodeId episodeId;
    private final Instant createdAt;
    private JobStatus status;
    private Stage stage;
    private int progress;
    private String message;
    private String errorDetail;
    private Instant startedAt;
    private Instant finishedAt;

    public ProductionJob(JobId id, EpisodeId episodeId, JobStatus status, Stage stage, int progress, String message,
                         String errorDetail, Instant createdAt, Instant startedAt, Instant finishedAt) {
        this.id = id;
        this.episodeId = episodeId;
        this.status = status;
        this.stage = stage;
        this.progress = progress;
        this.message = message;
        this.errorDetail = errorDetail;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public static ProductionJob queue(EpisodeId episodeId, Instant now) {
        return new ProductionJob(JobId.create(), episodeId, JobStatus.QUEUED, Stage.WAITING, 0, "En espera", null, now, null, null);
    }

    public void start(Instant now) {
        require(JobStatus.QUEUED);
        status = JobStatus.RUNNING;
        startedAt = now;
        message = "Preparando";
    }

    public void report(Stage newStage, int newProgress, String newMessage) {
        require(JobStatus.RUNNING);
        stage = newStage;
        progress = Math.clamp(newProgress, progress, 99);
        message = newMessage;
    }

    public void succeed(Instant now) {
        require(JobStatus.RUNNING);
        finish(JobStatus.SUCCEEDED, "Video listo", now);
        progress = 100;
    }

    public void fail(String userMessage, String detail, Instant now) {
        requireActive();
        errorDetail = detail;
        finish(JobStatus.FAILED, userMessage, now);
    }

    public void cancel(Instant now) {
        requireActive();
        finish(JobStatus.CANCELLED, "Cancelado", now);
    }

    private void finish(JobStatus end, String endMessage, Instant now) {
        status = end;
        message = endMessage;
        finishedAt = now;
    }

    private void require(JobStatus expected) {
        if (status != expected) {
            throw new InvalidStateException("El trabajo está en estado " + status + ".");
        }
    }

    private void requireActive() {
        if (!status.active()) {
            throw new InvalidStateException("El trabajo ya terminó.");
        }
    }

    public JobId id() {
        return id;
    }

    public EpisodeId episodeId() {
        return episodeId;
    }

    public JobStatus status() {
        return status;
    }

    public Stage stage() {
        return stage;
    }

    public int progress() {
        return progress;
    }

    public String message() {
        return message;
    }

    public String errorDetail() {
        return errorDetail;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }
}
