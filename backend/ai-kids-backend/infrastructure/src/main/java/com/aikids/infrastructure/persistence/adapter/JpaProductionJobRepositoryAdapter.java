package com.aikids.infrastructure.persistence.adapter;

import com.aikids.application.port.out.ProductionJobRepositoryPort;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.JobStatus;
import com.aikids.domain.production.ProductionJob;
import com.aikids.domain.production.Stage;
import com.aikids.infrastructure.persistence.entity.ProductionJobEntity;
import com.aikids.infrastructure.persistence.repository.ProductionJobJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaProductionJobRepositoryAdapter implements ProductionJobRepositoryPort {

    private final ProductionJobJpaRepository repository;

    JpaProductionJobRepositoryAdapter(ProductionJobJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(ProductionJob j) {
        ProductionJobEntity e = new ProductionJobEntity();
        e.id = j.id().value().toString();
        e.episodeId = j.episodeId().value().toString();
        e.status = j.status().name();
        e.stage = j.stage().name();
        e.progress = j.progress();
        e.message = j.message();
        e.errorDetail = j.errorDetail();
        e.createdAt = j.createdAt().toEpochMilli();
        e.startedAt = millis(j.startedAt());
        e.finishedAt = millis(j.finishedAt());
        repository.save(e);
    }

    @Override
    public Optional<ProductionJob> find(JobId id) {
        return repository.findById(id.value().toString()).map(JpaProductionJobRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ProductionJob> latestOf(EpisodeId id) {
        return repository.findFirstByEpisodeIdOrderByCreatedAtDesc(id.value().toString()).map(JpaProductionJobRepositoryAdapter::toDomain);
    }

    @Override
    public List<ProductionJob> active() {
        return repository.findByStatusInOrderByCreatedAt(List.of(JobStatus.QUEUED.name(), JobStatus.RUNNING.name())).stream()
                .map(JpaProductionJobRepositoryAdapter::toDomain).toList();
    }

    private static ProductionJob toDomain(ProductionJobEntity e) {
        return new ProductionJob(new JobId(UUID.fromString(e.id)), new EpisodeId(UUID.fromString(e.episodeId)),
                JobStatus.valueOf(e.status), Stage.valueOf(e.stage), e.progress, e.message, e.errorDetail,
                Instant.ofEpochMilli(e.createdAt), instant(e.startedAt), instant(e.finishedAt));
    }

    private static Long millis(Instant i) {
        return i == null ? null : i.toEpochMilli();
    }

    private static Instant instant(Long ms) {
        return ms == null ? null : Instant.ofEpochMilli(ms);
    }
}
