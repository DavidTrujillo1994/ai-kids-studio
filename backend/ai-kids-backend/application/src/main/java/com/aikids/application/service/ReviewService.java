package com.aikids.application.service;

import com.aikids.application.port.in.EpisodeUseCases.ExportEpisode;
import com.aikids.application.port.in.EpisodeUseCases.ReviewEpisode;
import com.aikids.application.port.out.EpisodeMediaPort;
import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.application.port.out.TransactionPort;
import com.aikids.application.shared.NotFoundException;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.shared.InvalidStateException;
import java.time.Clock;
import java.util.function.Consumer;

public final class ReviewService implements ReviewEpisode, ExportEpisode {

    private final EpisodeRepositoryPort repository;
    private final EpisodeMediaPort media;
    private final TransactionPort transaction;
    private final Clock clock;

    public ReviewService(EpisodeRepositoryPort repository, EpisodeMediaPort media, TransactionPort transaction, Clock clock) {
        this.repository = repository;
        this.media = media;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Override
    public Episode approve(EpisodeId id, String comments) {
        return change(id, e -> {
            if (media.video(e.folder()).isEmpty()) {
                throw new InvalidStateException("No hay un video que aprobar.");
            }
            e.approve(comments, clock.instant());
        });
    }

    @Override
    public Episode reject(EpisodeId id, String comments) {
        return change(id, e -> e.reject(comments, clock.instant()));
    }

    @Override
    public String export(EpisodeId id) {
        Episode episode = get(id);
        episode.requireExportable();
        return media.export(episode.folder(), episode.title());
    }

    private Episode change(EpisodeId id, Consumer<Episode> action) {
        return transaction.run(() -> {
            Episode episode = get(id);
            action.accept(episode);
            repository.save(episode);
            return episode;
        });
    }

    private Episode get(EpisodeId id) {
        return repository.find(id).orElseThrow(() -> new NotFoundException("el episodio"));
    }
}
