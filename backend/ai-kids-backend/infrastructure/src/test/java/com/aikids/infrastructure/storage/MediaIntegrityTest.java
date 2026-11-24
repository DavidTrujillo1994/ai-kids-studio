package com.aikids.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.catalog.CharacterAsset;
import com.aikids.domain.direction.DirectionGuide;
import com.aikids.domain.story.StoryTemplate;
import com.aikids.infrastructure.EngineSettings;
import com.aikids.infrastructure.script.ScriptJson;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Revisa el contenido real de media/: ninguna historia ni regla puede pedir algo que no existe. */
class MediaIntegrityTest {

    private final EngineSettings settings = new EngineSettings(Path.of("../../.."), Path.of("x"));
    private final ScriptJson json = new ScriptJson();
    private final AssetCatalog catalog = new FileAssetCatalogAdapter(settings, json).load();
    private final FileStoryAdapter stories = new FileStoryAdapter(settings, json);

    @Test
    void everyTemplateWorksWithEveryPairOfCharacters() {
        assertThat(stories.all()).isNotEmpty();
        for (StoryTemplate template : stories.all()) {
            for (CharacterAsset a : catalog.characters()) {
                for (CharacterAsset b : catalog.characters()) {
                    if (!a.equals(b)) {
                        var actors = List.of(new StoryTemplate.Actor(a.key(), a.name()), new StoryTemplate.Actor(b.key(), b.name())).subList(0, template.roles());
                        var script = template.castWith(actors);
                        assertThat(script.problems(catalog)).as("%s con %s y %s", template.name(), a.name(), b.name()).isEmpty();
                        assertThat(script.steps().toString()).doesNotContain("rol-", "{1}", "{2}");
                        assertThat(json.read(json.write(script))).as("ida y vuelta").isEqualTo(script);
                    }
                }
            }
            assertThat(catalog.environments()).contains(template.environment());
        }
    }

    @Test
    void theDirectionRulesOnlyUseWhatExists() {
        DirectionGuide guide = stories.load();
        assertThat(catalog.animations()).containsAll(guide.neutral()).containsAll(guide.question()).containsAll(guide.exclamation());
        assertThat(catalog.gaits()).containsAll(guide.environmentGaits().values());
        guide.rules().forEach(rule -> {
            assertThat(catalog.animations()).as("%s", rule.words()).containsAll(rule.animations());
            assertThat(catalog.characters()).as("%s", rule.words()).allMatch(c -> c.expressions().contains(rule.expression()));
            if (rule.gait() != null) {
                assertThat(catalog.gaits()).contains(rule.gait());
            }
        });
    }
}
