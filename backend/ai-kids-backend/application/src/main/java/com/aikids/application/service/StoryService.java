package com.aikids.application.service;

import com.aikids.application.port.in.StoryUseCases.GetStoryTemplates;
import com.aikids.application.port.in.StoryUseCases.SuggestMovements;
import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.application.port.out.DirectionGuidePort;
import com.aikids.application.port.out.StoryTemplatePort;
import com.aikids.domain.direction.MovementDirector;
import com.aikids.domain.script.Script;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.story.StoryTemplate;
import java.util.List;

public final class StoryService implements GetStoryTemplates, SuggestMovements {

    private final StoryTemplatePort templates;
    private final DirectionGuidePort guide;
    private final AssetCatalogPort assets;

    public StoryService(StoryTemplatePort templates, DirectionGuidePort guide, AssetCatalogPort assets) {
        this.templates = templates;
        this.guide = guide;
        this.assets = assets;
    }

    @Override
    public List<StoryTemplate> templates() {
        return templates.all();
    }

    @Override
    public Script suggest(Script script, AssetKey environment) {
        return new MovementDirector(guide.load(), assets.load()).direct(script, environment);
    }
}
