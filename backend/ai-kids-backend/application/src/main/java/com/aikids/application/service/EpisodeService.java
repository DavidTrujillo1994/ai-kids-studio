package com.aikids.application.service;

import com.aikids.application.port.in.EpisodeUseCases.CreateEpisode;
import com.aikids.application.port.in.EpisodeUseCases.DeleteEpisode;
import com.aikids.application.port.in.EpisodeUseCases.EditEpisode;
import com.aikids.application.port.in.EpisodeUseCases.GetEpisodeMedia;
import com.aikids.application.port.in.EpisodeUseCases.GetEpisodes;
import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.application.port.out.CharacterRepositoryPort;
import com.aikids.application.port.out.EpisodeMediaPort;
import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.application.port.out.SeriesRepositoryPort;
import com.aikids.application.port.out.StoryTemplatePort;
import com.aikids.application.port.out.TransactionPort;
import com.aikids.application.shared.NotFoundException;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.ScriptProblem;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import com.aikids.domain.story.StoryTemplate;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

public final class EpisodeService implements CreateEpisode, EditEpisode, GetEpisodes, GetEpisodeMedia, DeleteEpisode {

    private final EpisodeRepositoryPort repository;
    private final SeriesRepositoryPort seriesRepository;
    private final CharacterRepositoryPort characters;
    private final AssetCatalogPort assets;
    private final StoryTemplatePort templates;
    private final EpisodeMediaPort media;
    private final TransactionPort transaction;
    private final Clock clock;

    public EpisodeService(EpisodeRepositoryPort repository, SeriesRepositoryPort seriesRepository, CharacterRepositoryPort characters,
                          AssetCatalogPort assets, StoryTemplatePort templates, EpisodeMediaPort media, TransactionPort transaction, Clock clock) {
        this.repository = repository;
        this.seriesRepository = seriesRepository;
        this.characters = characters;
        this.assets = assets;
        this.templates = templates;
        this.media = media;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Override
    public Episode create(NewEpisode request) {
        AssetCatalog catalog = assets.load();
        Optional<StoryTemplate> template = request.template().map(this::template);
        AssetKey environment = request.environment().or(() -> template.map(StoryTemplate::environment))
                .orElseThrow(() -> new InvalidDataException("Elige dónde ocurre."));
        requireEnvironment(environment, catalog);
        return transaction.run(() -> {
            seriesRepository.find(request.seriesId()).orElseThrow(() -> new NotFoundException("la serie"));
            Script script = template.map(t -> t.castWith(actors(request))).orElse(Script.empty());
            Episode episode = Episode.create(request.seriesId(), request.title(), request.idea(), environment, script, clock.instant());
            repository.save(episode);
            return episode;
        });
    }

    private StoryTemplate template(AssetKey id) {
        return templates.all().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow(() -> new NotFoundException("la historia"));
    }

    private List<StoryTemplate.Actor> actors(NewEpisode request) {
        List<StoryCharacter> available = characters.ofSeries(request.seriesId());
        return request.cast().stream().map(key -> available.stream().filter(c -> c.assetKey().equals(key)).findFirst()
                .map(c -> new StoryTemplate.Actor(key, c.name())).orElseThrow(() -> new NotFoundException("el personaje"))).toList();
    }

    @Override
    public Episode edit(EpisodeId id, String title, String idea, AssetKey environment, Script script) {
        requireEnvironment(environment, assets.load());
        return transaction.run(() -> {
            Episode episode = get(id);
            episode.edit(title, idea, environment, script, clock.instant());
            repository.save(episode);
            return episode;
        });
    }

    @Override
    public List<Episode> ofSeries(SeriesId id) {
        return repository.ofSeries(id);
    }

    @Override
    public Episode get(EpisodeId id) {
        return repository.find(id).orElseThrow(() -> new NotFoundException("el episodio"));
    }

    @Override
    public List<ScriptProblem> problems(Episode episode) {
        return episode.script().problems(assets.load());
    }

    @Override
    public boolean hasVideo(Episode episode) {
        return media.video(episode.folder()).isPresent();
    }

    @Override
    public Path video(EpisodeId id) {
        return media.video(get(id).folder()).orElseThrow(() -> new NotFoundException("el video"));
    }

    @Override
    public Path thumbnail(EpisodeId id) {
        return media.thumbnail(get(id).folder()).orElseThrow(() -> new NotFoundException("la miniatura"));
    }

    @Override
    public void delete(EpisodeId id) {
        transaction.run(() -> {
            get(id).requireDeletable();
            repository.delete(id);
        });
    }

    private static void requireEnvironment(AssetKey environment, AssetCatalog catalog) {
        if (!catalog.environments().contains(environment)) {
            throw new InvalidDataException("El escenario «" + environment + "» no existe.");
        }
    }
}
