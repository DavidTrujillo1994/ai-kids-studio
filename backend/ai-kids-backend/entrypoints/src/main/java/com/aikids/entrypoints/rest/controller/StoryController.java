package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.StoryUseCases.GetStoryTemplates;
import com.aikids.application.port.in.StoryUseCases.SuggestMovements;
import com.aikids.domain.shared.AssetKey;
import com.aikids.entrypoints.rest.dto.Dtos.ScriptDto;
import com.aikids.entrypoints.rest.dto.Dtos.SuggestRequest;
import com.aikids.entrypoints.rest.dto.Dtos.TemplateDto;
import com.aikids.entrypoints.rest.mapper.ScriptMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class StoryController {

    private final GetStoryTemplates templates;
    private final SuggestMovements suggest;

    StoryController(GetStoryTemplates templates, SuggestMovements suggest) {
        this.templates = templates;
        this.suggest = suggest;
    }

    @GetMapping("/story-templates")
    List<TemplateDto> templates() {
        return templates.templates().stream()
                .map(t -> new TemplateDto(t.id().value(), t.name(), t.icon(), t.description(), t.roles(), t.environment().value())).toList();
    }

    @PostMapping("/movement-suggestions")
    ScriptDto suggest(@Valid @RequestBody SuggestRequest r) {
        return ScriptMapper.toDto(suggest.suggest(ScriptMapper.toDomain(r.script()), new AssetKey(r.environment())));
    }
}
