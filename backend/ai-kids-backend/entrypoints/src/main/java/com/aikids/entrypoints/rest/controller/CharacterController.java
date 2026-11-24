package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.CharacterUseCases.ApproveCharacterDesign;
import com.aikids.application.port.in.CharacterUseCases.GetCharacters;
import com.aikids.application.port.in.CharacterUseCases.RegisterCharacter;
import com.aikids.application.port.in.CharacterUseCases.UpdateCharacter;
import com.aikids.domain.character.CharacterId;
import com.aikids.domain.character.CharacterRole;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import com.aikids.entrypoints.rest.dto.Dtos.CharacterRequest;
import com.aikids.entrypoints.rest.dto.Dtos.CharacterResponse;
import com.aikids.entrypoints.rest.dto.Dtos.RegisterCharacterRequest;
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
@RequestMapping("/api/v1")
class CharacterController {

    private final RegisterCharacter register;
    private final UpdateCharacter update;
    private final ApproveCharacterDesign approve;
    private final GetCharacters get;

    CharacterController(RegisterCharacter register, UpdateCharacter update, ApproveCharacterDesign approve, GetCharacters get) {
        this.register = register;
        this.update = update;
        this.approve = approve;
        this.get = get;
    }

    @GetMapping("/series/{id}/characters")
    List<CharacterResponse> ofSeries(@PathVariable UUID id) {
        return get.ofSeries(new SeriesId(id)).stream().map(ResponseMapper::character).toList();
    }

    @PostMapping("/series/{id}/characters")
    @ResponseStatus(HttpStatus.CREATED)
    CharacterResponse register(@PathVariable UUID id, @Valid @RequestBody RegisterCharacterRequest r) {
        return ResponseMapper.character(register.register(new SeriesId(id), new AssetKey(r.assetKey()), role(r.role())));
    }

    @PutMapping("/characters/{id}")
    CharacterResponse update(@PathVariable UUID id, @Valid @RequestBody CharacterRequest r) {
        return ResponseMapper.character(update.update(new CharacterId(id), r.name(), r.personality()));
    }

    @PostMapping("/characters/{id}/design-approval")
    CharacterResponse approve(@PathVariable UUID id) {
        return ResponseMapper.character(approve.approve(new CharacterId(id)));
    }

    private static CharacterRole role(String value) {
        try {
            return CharacterRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("El rol debe ser PROTAGONISTA o SECUNDARIO.");
        }
    }
}
