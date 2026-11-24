package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.EpisodeUseCases.ExportEpisode;
import com.aikids.application.port.in.EpisodeUseCases.ReviewEpisode;
import com.aikids.application.port.in.ProductionUseCases.CancelProduction;
import com.aikids.application.port.in.ProductionUseCases.GetProduction;
import com.aikids.application.port.in.ProductionUseCases.StartProduction;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.production.JobId;
import com.aikids.entrypoints.rest.dto.Dtos.DecisionDto;
import com.aikids.entrypoints.rest.dto.Dtos.ExportResponse;
import com.aikids.entrypoints.rest.dto.Dtos.JobResponse;
import com.aikids.entrypoints.rest.dto.Dtos.ReviewRequest;
import com.aikids.entrypoints.rest.mapper.ResponseMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ProductionController {

    private final StartProduction start;
    private final CancelProduction cancel;
    private final GetProduction get;
    private final ReviewEpisode review;
    private final ExportEpisode export;

    ProductionController(StartProduction start, CancelProduction cancel, GetProduction get, ReviewEpisode review, ExportEpisode export) {
        this.start = start;
        this.cancel = cancel;
        this.get = get;
        this.review = review;
        this.export = export;
    }

    @PostMapping("/episodes/{id}/production")
    @ResponseStatus(HttpStatus.ACCEPTED)
    JobResponse start(@PathVariable UUID id) {
        return ResponseMapper.job(start.start(new EpisodeId(id)));
    }

    @GetMapping("/jobs")
    List<JobResponse> active() {
        return get.active().stream().map(ResponseMapper::job).toList();
    }

    @GetMapping("/jobs/{id}")
    JobResponse job(@PathVariable UUID id) {
        return ResponseMapper.job(get.get(new JobId(id)));
    }

    @PostMapping("/jobs/{id}/cancel")
    JobResponse cancel(@PathVariable UUID id) {
        return ResponseMapper.job(cancel.cancel(new JobId(id)));
    }

    @PostMapping("/episodes/{id}/approval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest r) {
        if (r.decision() == DecisionDto.APPROVE) {
            review.approve(new EpisodeId(id), r.comments());
        } else {
            review.reject(new EpisodeId(id), r.comments());
        }
    }

    @PostMapping("/episodes/{id}/export")
    ExportResponse export(@PathVariable UUID id) {
        return new ExportResponse(export.export(new EpisodeId(id)));
    }
}
