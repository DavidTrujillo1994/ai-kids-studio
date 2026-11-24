package com.aikids.application.port.out;

import com.aikids.domain.series.Series;
import com.aikids.domain.series.SeriesId;
import java.util.List;
import java.util.Optional;

public interface SeriesRepositoryPort {

    void save(Series series);

    Optional<Series> find(SeriesId id);

    List<Series> all();
}
