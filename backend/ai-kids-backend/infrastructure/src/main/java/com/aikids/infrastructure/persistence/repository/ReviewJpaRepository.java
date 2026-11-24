package com.aikids.infrastructure.persistence.repository;

import com.aikids.infrastructure.persistence.entity.ReviewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByEpisodeIdOrderByPosition(String episodeId);

    List<ReviewEntity> findByEpisodeIdInOrderByPosition(List<String> episodeIds);

    int countByEpisodeId(String episodeId);

    void deleteByEpisodeId(String episodeId);
}
