package com.aikids.application.service;

import com.aikids.application.port.in.SeriesUseCases.CreateSeries;
import com.aikids.application.port.in.SeriesUseCases.GetSeries;
import com.aikids.application.port.in.SeriesUseCases.UpdateSeries;
import com.aikids.application.port.out.SeriesRepositoryPort;
import com.aikids.application.shared.NotFoundException;
import com.aikids.domain.series.Series;
import com.aikids.domain.series.SeriesId;
import java.time.Clock;
import java.util.List;

public final class SeriesService implements CreateSeries, UpdateSeries, GetSeries {

    private final SeriesRepositoryPort repository;
    private final Clock clock;

    public SeriesService(SeriesRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Series create(String name, String description) {
        Series series = Series.create(name, description, clock.instant());
        repository.save(series);
        return series;
    }

    @Override
    public Series update(SeriesId id, String name, String description) {
        Series series = get(id);
        series.update(name, description, clock.instant());
        repository.save(series);
        return series;
    }

    @Override
    public List<Series> all() {
        return repository.all();
    }

    @Override
    public Series get(SeriesId id) {
        return repository.find(id).orElseThrow(() -> new NotFoundException("la serie"));
    }
}
