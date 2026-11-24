package com.aikids.infrastructure.persistence.repository;

import com.aikids.infrastructure.persistence.entity.CharacterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterJpaRepository extends JpaRepository<CharacterEntity, String> {
    List<CharacterEntity> findBySeriesIdOrderByName(String seriesId);
}
