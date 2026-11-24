package com.aikids.infrastructure.storage;

import com.aikids.application.port.out.DirectionGuidePort;
import com.aikids.application.port.out.StoryTemplatePort;
import com.aikids.domain.direction.DirectionGuide;
import com.aikids.domain.direction.DirectionRule;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.story.StoryTemplate;
import com.aikids.infrastructure.EngineSettings;
import com.aikids.infrastructure.script.ScriptJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Historias y reglas de dirección en archivos editables: cambiarlas no requiere programar. */
@Component
class FileStoryAdapter implements StoryTemplatePort, DirectionGuidePort {

    private final EngineSettings settings;
    private final ScriptJson json;

    FileStoryAdapter(EngineSettings settings, ScriptJson json) {
        this.settings = settings;
        this.json = json;
    }

    @Override
    public List<StoryTemplate> all() {
        List<StoryTemplate> templates = new ArrayList<>();
        for (JsonNode t : read("plantillas.json", "[]")) {
            JsonNode script = t.path("script");
            templates.add(new StoryTemplate(new AssetKey(t.path("id").asString("")), t.path("name").asString(""), t.path("icon").asString(""),
                    t.path("description").asString(""), t.path("roles").asInt(1), new AssetKey(script.path("environment").asString("")), json.read(script)));
        }
        return templates;
    }

    @Override
    public DirectionGuide load() {
        JsonNode root = read("direccion.json", "{}");
        List<DirectionRule> rules = new ArrayList<>();
        for (JsonNode r : root.path("rules")) {
            rules.add(new DirectionRule(strings(r.path("words")), strings(r.path("animations")), r.path("expression").asString("normal"),
                    r.path("gait").asString(null)));
        }
        Map<AssetKey, String> gaits = new HashMap<>();
        JsonNode places = root.path("environmentGaits");
        for (String place : places.propertyNames()) {
            gaits.put(new AssetKey(place), places.path(place).asString(""));
        }
        return new DirectionGuide(rules, strings(root.path("question")), strings(root.path("exclamation")), strings(root.path("neutral")), gaits);
    }

    private JsonNode read(String file, String missing) {
        Path path = settings.media().resolve(file);
        try {
            return json.tree(Files.exists(path) ? Files.readString(path) : missing);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode n : array) {
            values.add(n.asString(""));
        }
        return values;
    }
}
