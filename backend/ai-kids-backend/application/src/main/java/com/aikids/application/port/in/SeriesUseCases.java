package com.aikids.application.port.in;

import com.aikids.domain.series.Series;
import com.aikids.domain.series.SeriesId;
import java.util.List;

public final class SeriesUseCases {

    private SeriesUseCases() {
    }

    public interface CreateSeries {
        Series create(String name, String description);
    }

    public interface UpdateSeries {
        Series update(SeriesId id, String name, String description);
    }

    public interface GetSeries {
        List<Series> all();

        Series get(SeriesId id);
    }
}
