package com.aikids.entrypoints.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Contratos de la API. Nunca se exponen entidades de persistencia ni rutas del disco. */
public final class Dtos {

    private Dtos() {
    }

    public record SeriesRequest(@NotBlank @Size(max = 80) String name, @Size(max = 500) String description) {
    }

    public record SeriesResponse(String id, String name, String description, Instant createdAt) {
    }

    public record RegisterCharacterRequest(@NotBlank String assetKey, @NotBlank String role) {
    }

    public record CharacterRequest(@NotBlank @Size(max = 40) String name, @Size(max = 300) String personality) {
    }

    public record CharacterResponse(String id, String assetKey, String role, String name, String personality, int designVersion,
                                    String designStatus) {
    }

    public record CreateEpisodeRequest(@NotBlank @Size(max = 80) String title, @Size(max = 1000) String idea, String environment, String template,
                                       @Size(max = 10) List<@NotBlank String> cast) {
    }

    public record EditEpisodeRequest(@NotBlank @Size(max = 80) String title, @Size(max = 1000) String idea, @NotBlank String environment,
                                     @NotNull @Valid ScriptDto script) {
    }

    public record ScriptDto(@NotNull @Size(max = 10) List<@Valid @NotNull CastDto> cast, String ambience, @NotNull @Size(max = 60) List<@Valid @NotNull StepDto> steps) {
    }

    public record CastDto(@NotBlank String character, int startX) {
    }

    public enum StepType {
        WALK, SAY, PLAY, PAUSE, APPEAR, SCENE
    }

    public record StepDto(@NotNull StepType type, String character, @Size(max = 400) String text, String animation, String expression,
                          Integer to, String gait, Double seconds, String prop, @Size(max = 20) List<@NotNull PointDto> path,
                          String environment, String ambience, @Size(max = 10) List<@Valid @NotNull CastDto> positions) {
    }

    public record PointDto(double second, double x, double y) {
    }

    public record ProblemDto(int step, String message) {
    }

    public record ReviewDto(String decision, String comments, Instant at) {
    }

    public record EpisodeSummary(String id, String title, String idea, String status, boolean hasVideo, Instant createdAt, Instant updatedAt) {
    }

    public record EpisodeResponse(String id, String seriesId, String title, String idea, String environment, String status, boolean editable,
                                  boolean hasVideo, ScriptDto script, List<ProblemDto> problems, List<ReviewDto> reviews, JobResponse job,
                                  Instant createdAt, Instant updatedAt) {
    }

    public record JobResponse(String id, String episodeId, String status, String stage, int progress, String message, String errorDetail,
                              Instant startedAt, Instant finishedAt) {
    }

    public enum DecisionDto {
        APPROVE, REJECT
    }

    public record ReviewRequest(@NotNull DecisionDto decision, @Size(max = 1000) String comments) {
    }

    public record ExportResponse(String folder) {
    }

    public record CharacterAssetDto(String key, String name, List<String> expressions) {
    }

    public record CatalogResponse(List<CharacterAssetDto> characters, List<String> environments, List<String> ambiences, List<String> props,
                                  List<String> animations, List<String> gaits) {
    }

    public record TemplateDto(String id, String name, String icon, String description, int roles, String environment) {
    }

    public record SuggestRequest(@NotNull @Valid ScriptDto script, @NotBlank String environment) {
    }

    public record ToolResponse(String name, String purpose, boolean ready, String detail) {
    }
}
