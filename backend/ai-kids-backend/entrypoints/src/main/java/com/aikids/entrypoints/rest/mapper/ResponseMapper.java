package com.aikids.entrypoints.rest.mapper;

import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.production.ProductionJob;
import com.aikids.domain.script.ScriptProblem;
import com.aikids.domain.series.Series;
import com.aikids.entrypoints.rest.dto.Dtos.CharacterResponse;
import com.aikids.entrypoints.rest.dto.Dtos.EpisodeResponse;
import com.aikids.entrypoints.rest.dto.Dtos.EpisodeSummary;
import com.aikids.entrypoints.rest.dto.Dtos.JobResponse;
import com.aikids.entrypoints.rest.dto.Dtos.ProblemDto;
import com.aikids.entrypoints.rest.dto.Dtos.ReviewDto;
import com.aikids.entrypoints.rest.dto.Dtos.SeriesResponse;
import java.util.List;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static SeriesResponse series(Series s) {
        return new SeriesResponse(s.id().value().toString(), s.name(), s.description(), s.createdAt());
    }

    public static CharacterResponse character(StoryCharacter c) {
        return new CharacterResponse(c.id().value().toString(), c.assetKey().value(), c.role().name(), c.name(), c.personality(),
                c.designVersion(), c.designStatus().name());
    }

    public static EpisodeSummary summary(Episode e, boolean hasVideo) {
        return new EpisodeSummary(e.id().value().toString(), e.title(), e.idea(), e.status().name(), hasVideo, e.createdAt(), e.updatedAt());
    }

    public static EpisodeResponse episode(Episode e, boolean hasVideo, List<ScriptProblem> problems, ProductionJob job) {
        return new EpisodeResponse(e.id().value().toString(), e.seriesId().value().toString(), e.title(), e.idea(), e.environment().value(),
                e.status().name(), e.status().editable(), hasVideo, ScriptMapper.toDto(e.script()), problems(problems),
                e.reviews().stream().map(r -> new ReviewDto(r.decision().name(), r.comments(), r.at())).toList(),
                job == null ? null : job(job), e.createdAt(), e.updatedAt());
    }

    public static List<ProblemDto> problems(List<ScriptProblem> problems) {
        return problems.stream().map(p -> new ProblemDto(p.step(), p.message())).toList();
    }

    public static JobResponse job(ProductionJob j) {
        return new JobResponse(j.id().value().toString(), j.episodeId().value().toString(), j.status().name(), j.stage().name(), j.progress(),
                j.message(), j.errorDetail(), j.startedAt(), j.finishedAt());
    }
}
