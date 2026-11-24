package com.aikids.infrastructure.persistence.repository;

import com.aikids.infrastructure.persistence.entity.SeriesEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesJpaRepository extends JpaRepository<SeriesEntity, String> {
    List<SeriesEntity> findAllByOrderByCreatedAt();
}
