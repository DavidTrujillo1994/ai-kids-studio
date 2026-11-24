package com.aikids.infrastructure.persistence.adapter;

import static java.util.stream.Collectors.groupingBy;

import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.episode.EpisodeStatus;
import com.aikids.domain.episode.Review;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.infrastructure.persistence.entity.EpisodeEntity;
import com.aikids.infrastructure.persistence.entity.ReviewEntity;
import com.aikids.infrastructure.persistence.repository.EpisodeJpaRepository;
import com.aikids.infrastructure.persistence.repository.ProductionJobJpaRepository;
import com.aikids.infrastructure.persistence.repository.ReviewJpaRepository;
import com.aikids.infrastructure.script.ScriptJson;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaEpisodeRepositoryAdapter implements EpisodeRepositoryPort {

    private final EpisodeJpaRepository repository;
    private final ReviewJpaRepository reviews;
    private final ProductionJobJpaRepository jobs;
    private final ScriptJson scriptJson;

    JpaEpisodeRepositoryAdapter(EpisodeJpaRepository repository, ReviewJpaRepository reviews, ProductionJobJpaRepository jobs,
                                ScriptJson scriptJson) {
        this.repository = repository;
        this.reviews = reviews;
        this.jobs = jobs;
        this.scriptJson = scriptJson;
    }

    @Override
    @Transactional
    public void save(Episode ep) {
        EpisodeEntity e = new EpisodeEntity();
        e.id = ep.id().value().toString();
        e.seriesId = ep.seriesId().value().toString();
        e.folder = ep.folder().value();
        e.title = ep.title();
        e.idea = ep.idea();
        e.environment = ep.environment().value();
        e.script = scriptJson.write(ep.script());
        e.status = ep.status().name();
        e.createdAt = ep.createdAt().toEpochMilli();
        e.updatedAt = ep.updatedAt().toEpochMilli();
        repository.save(e);
        // Las revisiones son un historial: solo se agregan.
        List<Review> all = ep.reviews();
        for (int i = reviews.countByEpisodeId(e.id); i < all.size(); i++) {
            ReviewEntity r = new ReviewEntity();
            r.episodeId = e.id;
            r.position = i;
            r.decision = all.get(i).decision().name();
            r.comments = all.get(i).comments();
            r.at = all.get(i).at().toEpochMilli();
            reviews.save(r);
        }
    }

    @Override
    public Optional<Episode> find(EpisodeId id) {
        String key = id.value().toString();
        return repository.findById(key).map(e -> toDomain(e, reviews.findByEpisodeIdOrderByPosition(key)));
    }

    @Override
    public List<Episode> ofSeries(SeriesId id) {
        List<EpisodeEntity> found = repository.findBySeriesIdOrderByCreatedAtDesc(id.value().toString());
        Map<String, List<ReviewEntity>> byEpisode = reviews.findByEpisodeIdInOrderByPosition(found.stream().map(e -> e.id).toList())
                .stream().collect(groupingBy(r -> r.episodeId));
        return found.stream().map(e -> toDomain(e, byEpisode.getOrDefault(e.id, List.of()))).toList();
    }

    @Override
    @Transactional
    public void delete(EpisodeId id) {
        String key = id.value().toString();
        reviews.deleteByEpisodeId(key);
        jobs.deleteByEpisodeId(key);
        repository.deleteById(key);
    }

    private Episode toDomain(EpisodeEntity e, List<ReviewEntity> rs) {
        return new Episode(new EpisodeId(UUID.fromString(e.id)), new SeriesId(UUID.fromString(e.seriesId)), new AssetKey(e.folder),
                e.title, e.idea, new AssetKey(e.environment), scriptJson.read(e.script), EpisodeStatus.valueOf(e.status),
                rs.stream().map(r -> new Review(Review.Decision.valueOf(r.decision), r.comments, Instant.ofEpochMilli(r.at))).toList(),
                Instant.ofEpochMilli(e.createdAt), Instant.ofEpochMilli(e.updatedAt));
    }
}
