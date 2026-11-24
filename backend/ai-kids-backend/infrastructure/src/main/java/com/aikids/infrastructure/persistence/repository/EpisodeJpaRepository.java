package com.aikids.infrastructure.persistence.repository;

import com.aikids.infrastructure.persistence.entity.EpisodeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeJpaRepository extends JpaRepository<EpisodeEntity, String> {
    List<EpisodeEntity> findBySeriesIdOrderByCreatedAtDesc(String seriesId);
}
