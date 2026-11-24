package com.aikids.application.service;

import com.aikids.application.port.in.CharacterUseCases.ApproveCharacterDesign;
import com.aikids.application.port.in.CharacterUseCases.GetCharacters;
import com.aikids.application.port.in.CharacterUseCases.RegisterCharacter;
import com.aikids.application.port.in.CharacterUseCases.UpdateCharacter;
import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.application.port.out.CharacterRepositoryPort;
import com.aikids.application.port.out.SeriesRepositoryPort;
import com.aikids.application.shared.NotFoundException;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.catalog.CharacterAsset;
import com.aikids.domain.character.CharacterId;
import com.aikids.domain.character.CharacterRole;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import java.util.List;

public final class CharacterService implements RegisterCharacter, UpdateCharacter, ApproveCharacterDesign, GetCharacters {

    private final CharacterRepositoryPort repository;
    private final SeriesRepositoryPort seriesRepository;
    private final AssetCatalogPort assets;

    public CharacterService(CharacterRepositoryPort repository, SeriesRepositoryPort seriesRepository, AssetCatalogPort assets) {
        this.repository = repository;
        this.seriesRepository = seriesRepository;
        this.assets = assets;
    }

    @Override
    public StoryCharacter register(SeriesId seriesId, AssetKey assetKey, CharacterRole role) {
        seriesRepository.find(seriesId).orElseThrow(() -> new NotFoundException("la serie"));
        CharacterAsset asset = assets.load().character(assetKey).orElseThrow(() -> new NotFoundException("el dibujo del personaje"));
        if (repository.ofSeries(seriesId).stream().anyMatch(c -> c.assetKey().equals(assetKey))) {
            throw new InvalidDataException(asset.name() + " ya está en la serie.");
        }
        StoryCharacter character = StoryCharacter.register(seriesId, assetKey, role, asset.name(), asset.personality(), asset.designVersion());
        repository.save(character);
        return character;
    }

    @Override
    public StoryCharacter update(CharacterId id, String name, String personality) {
        StoryCharacter character = get(id);
        character.updateProfile(name, personality);
        repository.save(character);
        return character;
    }

    @Override
    public StoryCharacter approve(CharacterId id) {
        StoryCharacter character = get(id);
        syncDesign(character, assets.load());
        character.approveDesign();
        repository.save(character);
        return character;
    }

    @Override
    public List<StoryCharacter> ofSeries(SeriesId id) {
        AssetCatalog catalog = assets.load();
        List<StoryCharacter> characters = repository.ofSeries(id);
        characters.forEach(c -> syncDesign(c, catalog));
        return characters;
    }

    private void syncDesign(StoryCharacter character, AssetCatalog catalog) {
        catalog.character(character.assetKey()).ifPresent(asset -> {
            if (character.detectDesign(asset.designVersion())) {
                repository.save(character);
            }
        });
    }

    private StoryCharacter get(CharacterId id) {
        return repository.find(id).orElseThrow(() -> new NotFoundException("el personaje"));
    }
}
