package com.aikids.infrastructure.persistence.adapter;

import com.aikids.application.port.out.SeriesRepositoryPort;
import com.aikids.domain.series.Series;
import com.aikids.domain.series.SeriesId;
import com.aikids.infrastructure.persistence.entity.SeriesEntity;
import com.aikids.infrastructure.persistence.repository.SeriesJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaSeriesRepositoryAdapter implements SeriesRepositoryPort {

    private final SeriesJpaRepository repository;

    JpaSeriesRepositoryAdapter(SeriesJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Series s) {
        SeriesEntity e = new SeriesEntity();
        e.id = s.id().value().toString();
        e.name = s.name();
        e.description = s.description();
        e.createdAt = s.createdAt().toEpochMilli();
        e.updatedAt = s.updatedAt().toEpochMilli();
        repository.save(e);
    }

    @Override
    public Optional<Series> find(SeriesId id) {
        return repository.findById(id.value().toString()).map(JpaSeriesRepositoryAdapter::toDomain);
    }

    @Override
    public List<Series> all() {
        return repository.findAllByOrderByCreatedAt().stream().map(JpaSeriesRepositoryAdapter::toDomain).toList();
    }

    private static Series toDomain(SeriesEntity e) {
        return new Series(new SeriesId(UUID.fromString(e.id)), e.name, e.description, Instant.ofEpochMilli(e.createdAt),
                Instant.ofEpochMilli(e.updatedAt));
    }
}
