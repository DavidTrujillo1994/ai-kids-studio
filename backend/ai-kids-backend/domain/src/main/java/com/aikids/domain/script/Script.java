package com.aikids.domain.script;

import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** ambience es la hora o el clima del primer lugar. */
public record Script(List<CastMember> cast, String ambience, List<Step> steps) {

    public static final int MAX_STEPS = 60;
    public static final String DEFAULT_AMBIENCE = "dia";

    public Script {
        cast = List.copyOf(cast);
        steps = List.copyOf(steps);
        if (steps.size() > MAX_STEPS) {
            throw new InvalidDataException("El guion admite hasta " + MAX_STEPS + " pasos.");
        }
        if (cast.stream().map(CastMember::character).distinct().count() != cast.size()) {
            throw new InvalidDataException("Un personaje está repetido en el reparto.");
        }
    }

    public Script(List<CastMember> cast, List<Step> steps) {
        this(cast, DEFAULT_AMBIENCE, steps);
    }

    public static Script empty() {
        return new Script(List.of(), List.of());
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    public Script withSteps(List<Step> newSteps) {
        return new Script(cast, ambience, newSteps);
    }

    public List<ScriptProblem> problems(AssetCatalog catalog) {
        List<ScriptProblem> found = new ArrayList<>();
        Set<AssetKey> keys = cast.stream().map(CastMember::character).collect(Collectors.toSet());
        for (AssetKey key : keys) {
            if (catalog.character(key).isEmpty()) {
                found.add(new ScriptProblem(-1, "El personaje «" + key + "» no tiene dibujos."));
            }
        }
        if (steps.isEmpty()) {
            found.add(new ScriptProblem(-1, "Agrega al menos un paso al guion."));
        }
        collect(found, -1, catalog, keys, context -> context.requireAmbience(ambience));
        for (int i = 0; i < steps.size(); i++) {
            collect(found, i, catalog, keys, steps.get(i)::check);
        }
        return found;
    }

    private static void collect(List<ScriptProblem> found, int step, AssetCatalog catalog, Set<AssetKey> keys,
                                java.util.function.Consumer<ScriptContext> check) {
        ScriptContext context = new ScriptContext(catalog, keys);
        check.accept(context);
        context.problems().forEach(message -> found.add(new ScriptProblem(step, message)));
    }
}
