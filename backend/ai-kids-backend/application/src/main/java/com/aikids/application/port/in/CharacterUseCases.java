package com.aikids.application.port.in;

import com.aikids.domain.character.CharacterId;
import com.aikids.domain.character.CharacterRole;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import java.util.List;

public final class CharacterUseCases {

    private CharacterUseCases() {
    }

    public interface RegisterCharacter {
        StoryCharacter register(SeriesId seriesId, AssetKey assetKey, CharacterRole role);
    }

    public interface UpdateCharacter {
        StoryCharacter update(CharacterId id, String name, String personality);
    }

    public interface ApproveCharacterDesign {
        StoryCharacter approve(CharacterId id);
    }

    public interface GetCharacters {
        List<StoryCharacter> ofSeries(SeriesId id);
    }
}
