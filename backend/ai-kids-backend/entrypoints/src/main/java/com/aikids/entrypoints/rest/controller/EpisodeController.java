package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.EpisodeUseCases.CreateEpisode;
import com.aikids.application.port.in.EpisodeUseCases.CreateEpisode.NewEpisode;
import com.aikids.application.port.in.EpisodeUseCases.DeleteEpisode;
import com.aikids.application.port.in.EpisodeUseCases.EditEpisode;
import com.aikids.application.port.in.EpisodeUseCases.GetEpisodes;
import com.aikids.application.port.in.ProductionUseCases.GetProduction;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.entrypoints.rest.dto.Dtos.CreateEpisodeRequest;
import com.aikids.entrypoints.rest.dto.Dtos.EditEpisodeRequest;
import com.aikids.entrypoints.rest.dto.Dtos.EpisodeResponse;
import com.aikids.entrypoints.rest.dto.Dtos.EpisodeSummary;
import com.aikids.entrypoints.rest.mapper.ResponseMapper;
import com.aikids.entrypoints.rest.mapper.ScriptMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class EpisodeController {

    private final CreateEpisode create;
    private final EditEpisode edit;
    private final DeleteEpisode delete;
    private final GetEpisodes get;
    private final GetProduction production;

    EpisodeController(CreateEpisode create, EditEpisode edit, DeleteEpisode delete, GetEpisodes get, GetProduction production) {
        this.create = create;
        this.edit = edit;
        this.delete = delete;
        this.get = get;
        this.production = production;
    }

    @GetMapping("/series/{id}/episodes")
    List<EpisodeSummary> ofSeries(@PathVariable UUID id) {
        return get.ofSeries(new SeriesId(id)).stream().map(e -> ResponseMapper.summary(e, get.hasVideo(e))).toList();
    }

    @PostMapping("/series/{id}/episodes")
    @ResponseStatus(HttpStatus.CREATED)
    EpisodeResponse create(@PathVariable UUID id, @Valid @RequestBody CreateEpisodeRequest r) {
        return full(create.create(new NewEpisode(new SeriesId(id), r.title(), r.idea(), Optional.ofNullable(r.environment()).map(AssetKey::new),
                Optional.ofNullable(r.template()).map(AssetKey::new), r.cast() == null ? List.of() : r.cast().stream().map(AssetKey::new).toList())));
    }

    @GetMapping("/episodes/{id}")
    EpisodeResponse one(@PathVariable UUID id) {
        return full(get.get(new EpisodeId(id)));
    }

    @PutMapping("/episodes/{id}")
    EpisodeResponse edit(@PathVariable UUID id, @Valid @RequestBody EditEpisodeRequest r) {
        return full(edit.edit(new EpisodeId(id), r.title(), r.idea(), new AssetKey(r.environment()), ScriptMapper.toDomain(r.script())));
    }

    @DeleteMapping("/episodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        delete.delete(new EpisodeId(id));
    }

    private EpisodeResponse full(Episode e) {
        return ResponseMapper.episode(e, get.hasVideo(e), get.problems(e), production.latestOf(e.id()).orElse(null));
    }
}
