package com.aikids.application.service;

import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.application.port.out.ProductionJobRepositoryPort;
import com.aikids.application.port.out.TransactionPort;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.catalog.CharacterAsset;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.ProductionJob;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

final class Fakes {

    static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final AssetKey BRUNO = new AssetKey("bruno");
    static final AssetKey BOSQUE = new AssetKey("bosque-central");
    static final AssetCatalog CATALOG = new AssetCatalog(List.of(new CharacterAsset(BRUNO, "Bruno", "", true, 1, List.of("normal", "happy"))),
            List.of(BOSQUE), List.of("dia"), List.of(), List.of("idle", "walk", "wave", "jump"), List.of("walk"));
    static final TransactionPort TRANSACTION = new TransactionPort() {
        @Override
        public <T> T run(Supplier<T> work) {
            return work.get();
        }
    };

    private Fakes() {
    }

    static final class Episodes implements EpisodeRepositoryPort {
        private final Map<EpisodeId, Episode> data = new LinkedHashMap<>();

        @Override
        public void save(Episode episode) {
            data.put(episode.id(), episode);
        }

        @Override
        public Optional<Episode> find(EpisodeId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public List<Episode> ofSeries(SeriesId id) {
            return data.values().stream().filter(e -> e.seriesId().equals(id)).toList();
        }

        @Override
        public void delete(EpisodeId id) {
            data.remove(id);
        }
    }

    static final class Jobs implements ProductionJobRepositoryPort {
        private final Map<JobId, ProductionJob> data = new LinkedHashMap<>();

        @Override
        public void save(ProductionJob job) {
            data.put(job.id(), job);
        }

        @Override
        public Optional<ProductionJob> find(JobId id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Optional<ProductionJob> latestOf(EpisodeId id) {
            return data.values().stream().filter(j -> j.episodeId().equals(id)).reduce((a, b) -> b);
        }

        @Override
        public List<ProductionJob> active() {
            return data.values().stream().filter(j -> j.status().active()).toList();
        }
    }
}
