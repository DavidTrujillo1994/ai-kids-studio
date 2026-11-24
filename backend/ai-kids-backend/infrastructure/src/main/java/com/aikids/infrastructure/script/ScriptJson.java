package com.aikids.infrastructure.script;

import com.aikids.domain.episode.Episode;
import com.aikids.domain.script.CastMember;
import com.aikids.domain.script.PathPoint;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.Step;
import com.aikids.domain.shared.AssetKey;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Traduce el guion al formato guion.json que entiende el motor, y de vuelta. */
@Component
public class ScriptJson {

    private final JsonMapper mapper = JsonMapper.builder().build();

    public String write(Script script) {
        return mapper.writeValueAsString(toNode(script));
    }

    public String writeForEngine(Episode episode) {
        ObjectNode root = toNode(episode.script());
        root.put("id", episode.folder().value());
        root.put("title", episode.title());
        root.put("environment", episode.environment().value());
        root.put("fps", 24);
        root.putArray("resolution").add(1280).add(720);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    public Script read(String json) {
        return read(mapper.readTree(json));
    }

    public Script read(JsonNode root) {
        List<CastMember> cast = new ArrayList<>();
        for (JsonNode c : root.path("characters")) {
            cast.add(new CastMember(new AssetKey(c.path("id").asString("")), c.path("x").asInt(0)));
        }
        List<Step> steps = new ArrayList<>();
        for (JsonNode s : root.path("steps")) {
            steps.add(readStep(s));
        }
        return new Script(cast, root.path("ambience").asString(Script.DEFAULT_AMBIENCE), steps);
    }

    public JsonNode tree(String json) {
        return mapper.readTree(json);
    }

    private ObjectNode toNode(Script script) {
        ObjectNode root = mapper.createObjectNode();
        root.put("ambience", script.ambience());
        ArrayNode cast = root.putArray("characters");
        for (CastMember c : script.cast()) {
            cast.addObject().put("id", c.character().value()).put("x", c.startX());
        }
        ArrayNode steps = root.putArray("steps");
        for (Step step : script.steps()) {
            writeStep(steps.addObject(), step);
        }
        return root;
    }

    private static void writeStep(ObjectNode n, Step step) {
        switch (step) {
            case Step.Walk s -> n.put("do", "walk").put("character", s.character().value()).put("to", s.to()).put("gait", s.gait())
                    .put("expression", s.expression());
            case Step.Say s -> n.put("do", "say").put("character", s.character().value()).put("text", s.text())
                    .put("animation", s.animation()).put("expression", s.expression());
            case Step.Play s -> n.put("do", "play").put("character", s.character().value()).put("animation", s.animation())
                    .put("expression", s.expression());
            case Step.Pause s -> n.put("do", "wait").put("character", s.character().value()).put("seconds", s.seconds())
                    .put("animation", s.animation()).put("expression", s.expression());
            case Step.Scene s -> {
                ObjectNode positions = n.put("do", "scene").put("environment", s.environment().value()).put("ambience", s.ambience()).putObject("positions");
                s.positions().forEach(p -> positions.put(p.character().value(), p.startX()));
            }
            case Step.Appear s -> {
                ArrayNode path = n.put("do", "prop").put("prop", s.prop().value()).putArray("path");
                s.path().forEach(p -> path.addArray().add(p.second()).add(p.x()).add(p.y()));
            }
        }
    }

    private static Step readStep(JsonNode s) {
        String expression = s.path("expression").asString(Step.DEFAULT_EXPRESSION);
        return switch (s.path("do").asString("")) {
            case "walk" -> new Step.Walk(actor(s), s.path("to").asInt(0), s.path("gait").asString(Step.DEFAULT_GAIT), expression);
            case "say" -> new Step.Say(actor(s), s.path("text").asString(""), s.path("animation").asString("talk"), expression);
            case "play" -> new Step.Play(actor(s), s.path("animation").asString("idle"), expression);
            case "wait" -> new Step.Pause(actor(s), s.path("seconds").asDouble(1), s.path("animation").asString("idle"), expression);
            case "scene" -> {
                List<CastMember> positions = new ArrayList<>();
                JsonNode places = s.path("positions");
                for (String who : places.propertyNames()) {
                    positions.add(new CastMember(new AssetKey(who), places.path(who).asInt(0)));
                }
                yield new Step.Scene(new AssetKey(s.path("environment").asString("")), s.path("ambience").asString(Script.DEFAULT_AMBIENCE), positions);
            }
            case "prop" -> {
                List<PathPoint> path = new ArrayList<>();
                for (JsonNode p : s.path("path")) {
                    path.add(new PathPoint(p.path(0).asDouble(0), p.path(1).asDouble(0), p.path(2).asDouble(0)));
                }
                yield new Step.Appear(new AssetKey(s.path("prop").asString("")), path);
            }
            default -> throw new IllegalArgumentException("Paso desconocido en el guion: " + s.path("do"));
        };
    }

    private static AssetKey actor(JsonNode s) {
        return new AssetKey(s.path("character").asString(""));
    }
}
