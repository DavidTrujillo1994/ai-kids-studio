package com.aikids.infrastructure.persistence.adapter;

import com.aikids.application.port.out.CharacterRepositoryPort;
import com.aikids.domain.character.CharacterId;
import com.aikids.domain.character.CharacterRole;
import com.aikids.domain.character.DesignStatus;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.infrastructure.persistence.entity.CharacterEntity;
import com.aikids.infrastructure.persistence.repository.CharacterJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaCharacterRepositoryAdapter implements CharacterRepositoryPort {

    private final CharacterJpaRepository repository;

    JpaCharacterRepositoryAdapter(CharacterJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(StoryCharacter c) {
        CharacterEntity e = new CharacterEntity();
        e.id = c.id().value().toString();
        e.seriesId = c.seriesId().value().toString();
        e.assetKey = c.assetKey().value();
        e.role = c.role().name();
        e.name = c.name();
        e.personality = c.personality();
        e.designVersion = c.designVersion();
        e.designStatus = c.designStatus().name();
        repository.save(e);
    }

    @Override
    public Optional<StoryCharacter> find(CharacterId id) {
        return repository.findById(id.value().toString()).map(JpaCharacterRepositoryAdapter::toDomain);
    }

    @Override
    public List<StoryCharacter> ofSeries(SeriesId id) {
        return repository.findBySeriesIdOrderByName(id.value().toString()).stream().map(JpaCharacterRepositoryAdapter::toDomain).toList();
    }

    private static StoryCharacter toDomain(CharacterEntity e) {
        return new StoryCharacter(new CharacterId(UUID.fromString(e.id)), new SeriesId(UUID.fromString(e.seriesId)),
                new AssetKey(e.assetKey), CharacterRole.valueOf(e.role), e.name, e.personality, e.designVersion,
                DesignStatus.valueOf(e.designStatus));
    }
}
