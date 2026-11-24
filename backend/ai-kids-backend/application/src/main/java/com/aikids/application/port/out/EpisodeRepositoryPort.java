package com.aikids.application.port.out;

import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.series.SeriesId;
import java.util.List;
import java.util.Optional;

public interface EpisodeRepositoryPort {

    void save(Episode episode);

    Optional<Episode> find(EpisodeId id);

    /** Del más reciente al más antiguo. */
    List<Episode> ofSeries(SeriesId id);

    void delete(EpisodeId id);
}
