package com.aikids.application.port.out;

import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.ProductionJob;
import java.util.List;
import java.util.Optional;

public interface ProductionJobRepositoryPort {

    void save(ProductionJob job);

    Optional<ProductionJob> find(JobId id);

    Optional<ProductionJob> latestOf(EpisodeId id);

    List<ProductionJob> active();
}
