package com.aikids.application.port.in;

import com.aikids.domain.script.Script;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.story.StoryTemplate;
import java.util.List;

public final class StoryUseCases {

    private StoryUseCases() {
    }

    public interface GetStoryTemplates {
        List<StoryTemplate> templates();
    }

    public interface SuggestMovements {
        /** Devuelve una propuesta; no guarda nada. */
        Script suggest(Script script, AssetKey environment);
    }
}
