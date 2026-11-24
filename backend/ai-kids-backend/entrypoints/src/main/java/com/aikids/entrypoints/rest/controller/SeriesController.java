package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.SeriesUseCases.CreateSeries;
import com.aikids.application.port.in.SeriesUseCases.GetSeries;
import com.aikids.application.port.in.SeriesUseCases.UpdateSeries;
import com.aikids.domain.series.SeriesId;
import com.aikids.entrypoints.rest.dto.Dtos.SeriesRequest;
import com.aikids.entrypoints.rest.dto.Dtos.SeriesResponse;
import com.aikids.entrypoints.rest.mapper.ResponseMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/series")
class SeriesController {

    private final CreateSeries create;
    private final UpdateSeries update;
    private final GetSeries get;

    SeriesController(CreateSeries create, UpdateSeries update, GetSeries get) {
        this.create = create;
        this.update = update;
        this.get = get;
    }

    @GetMapping
    List<SeriesResponse> all() {
        return get.all().stream().map(ResponseMapper::series).toList();
    }

    @GetMapping("/{id}")
    SeriesResponse one(@PathVariable UUID id) {
        return ResponseMapper.series(get.get(new SeriesId(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SeriesResponse create(@Valid @RequestBody SeriesRequest r) {
        return ResponseMapper.series(create.create(r.name(), r.description()));
    }

    @PutMapping("/{id}")
    SeriesResponse update(@PathVariable UUID id, @Valid @RequestBody SeriesRequest r) {
        return ResponseMapper.series(update.update(new SeriesId(id), r.name(), r.description()));
    }
}
