package com.aikids.application.port.out;

import com.aikids.domain.character.CharacterId;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.series.SeriesId;
import java.util.List;
import java.util.Optional;

public interface CharacterRepositoryPort {

    void save(StoryCharacter character);

    Optional<StoryCharacter> find(CharacterId id);

    List<StoryCharacter> ofSeries(SeriesId id);
}
