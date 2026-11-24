package com.aikids.infrastructure.persistence.repository;

import com.aikids.infrastructure.persistence.entity.ProductionJobEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionJobJpaRepository extends JpaRepository<ProductionJobEntity, String> {
    Optional<ProductionJobEntity> findFirstByEpisodeIdOrderByCreatedAtDesc(String episodeId);

    List<ProductionJobEntity> findByStatusInOrderByCreatedAt(List<String> statuses);

    void deleteByEpisodeId(String episodeId);
}
