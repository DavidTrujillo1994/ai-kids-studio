package com.aikids.application.service;

import com.aikids.application.port.in.SystemUseCases.CheckTools;
import com.aikids.application.port.in.SystemUseCases.GetAssetImages;
import com.aikids.application.port.in.SystemUseCases.GetCatalog;
import com.aikids.application.port.in.SystemUseCases.PrepareFirstUse;
import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.application.port.out.AssetImagePort;
import com.aikids.application.port.out.CharacterRepositoryPort;
import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.application.port.out.ExistingEpisodePort;
import com.aikids.application.port.out.SeriesRepositoryPort;
import com.aikids.application.port.out.ToolHealthPort;
import com.aikids.application.port.out.TransactionPort;
import com.aikids.application.shared.NotFoundException;
import com.aikids.application.shared.ToolStatus;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.character.CharacterRole;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.episode.EpisodeStatus;
import com.aikids.domain.series.Series;
import com.aikids.domain.shared.AssetKey;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SystemService implements GetCatalog, GetAssetImages, CheckTools, PrepareFirstUse {

    private final AssetCatalogPort assets;
    private final AssetImagePort images;
    private final ToolHealthPort tools;
    private final SeriesRepositoryPort series;
    private final CharacterRepositoryPort characters;
    private final EpisodeRepositoryPort episodes;
    private final ExistingEpisodePort existing;
    private final TransactionPort transaction;
    private final Clock clock;

    public SystemService(AssetCatalogPort assets, AssetImagePort images, ToolHealthPort tools, SeriesRepositoryPort series, CharacterRepositoryPort characters,
                         EpisodeRepositoryPort episodes, ExistingEpisodePort existing, TransactionPort transaction, Clock clock) {
        this.assets = assets;
        this.images = images;
        this.tools = tools;
        this.series = series;
        this.characters = characters;
        this.episodes = episodes;
        this.existing = existing;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Override
    public AssetCatalog catalog() {
        return assets.load();
    }

    @Override
    public Path portrait(AssetKey character) {
        return images.portrait(character).orElseThrow(() -> new NotFoundException("el retrato"));
    }

    @Override
    public Path view(AssetKey environment) {
        return images.view(environment).orElseThrow(() -> new NotFoundException("la imagen del lugar"));
    }

    @Override
    public List<ToolStatus> check() {
        return tools.check();
    }

    @Override
    public void prepare() {
        AssetCatalog catalog = assets.load();
        List<ExistingEpisodePort.Found> found = existing.findAll();
        transaction.run(() -> {
            List<Series> existingSeries = series.all();
            Series first = existingSeries.isEmpty() ? createFirst(found) : existingSeries.getFirst();
            addNewCharacters(first, catalog);
        });
    }

    private Series createFirst(List<ExistingEpisodePort.Found> found) {
        Instant now = clock.instant();
        Series first = Series.create("Las Aventuras del Bosque Mágico", "Historias de amistad y curiosidad en el bosque.", now);
        series.save(first);
        found.forEach(f -> episodes.save(new Episode(EpisodeId.create(), first.id(), f.folder(), f.title(), "", f.environment(), f.script(),
                f.hasVideo() ? EpisodeStatus.PENDING_APPROVAL : EpisodeStatus.SCRIPT_READY, List.of(), now, now)));
        return first;
    }

    private void addNewCharacters(Series target, AssetCatalog catalog) {
        Set<AssetKey> known = characters.ofSeries(target.id()).stream().map(StoryCharacter::assetKey).collect(Collectors.toSet());
        catalog.characters().stream().filter(a -> !known.contains(a.key())).forEach(a -> characters.save(StoryCharacter.register(target.id(), a.key(),
                a.protagonist() ? CharacterRole.PROTAGONISTA : CharacterRole.SECUNDARIO, a.name(), a.personality(), a.designVersion())));
    }
}
