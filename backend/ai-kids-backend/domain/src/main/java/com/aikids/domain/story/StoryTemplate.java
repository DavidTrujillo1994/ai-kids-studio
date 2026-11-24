package com.aikids.domain.story;

import com.aikids.domain.script.CastMember;
import com.aikids.domain.script.Script;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Historia de partida. Sus actores son papeles (rol-1, rol-2…) que se reparten al crear el episodio. */
public record StoryTemplate(AssetKey id, String name, String icon, String description, int roles, AssetKey environment, Script script) {

    private static final String ROLE = "rol-";

    public record Actor(AssetKey key, String name) {
    }

    public Script castWith(List<Actor> actors) {
        if (actors.size() < roles) {
            throw new InvalidDataException("«" + name + "» necesita " + roles + " personajes.");
        }
        if (actors.stream().map(Actor::key).distinct().count() != actors.size()) {
            throw new InvalidDataException("Elige personajes diferentes.");
        }
        Map<AssetKey, AssetKey> keys = new HashMap<>();
        for (int i = 0; i < actors.size(); i++) {
            keys.put(new AssetKey(ROLE + (i + 1)), actors.get(i).key());
        }
        UnaryOperator<AssetKey> actor = role -> keys.getOrDefault(role, role);
        UnaryOperator<String> text = t -> {
            String result = t;
            for (int i = 0; i < actors.size(); i++) {
                result = result.replace("{" + (i + 1) + "}", actors.get(i).name());
            }
            return result;
        };
        return new Script(script.cast().stream().map(c -> new CastMember(actor.apply(c.character()), c.startX())).toList(), script.ambience(),
                script.steps().stream().map(s -> s.recast(actor, text)).toList());
    }
}
