package com.aikids.domain.script;

import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.shared.AssetKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Acumula los problemas de un paso contra el catálogo y el reparto. */
public final class ScriptContext {

    private final AssetCatalog catalog;
    private final Set<AssetKey> cast;
    private final List<String> problems = new ArrayList<>();

    ScriptContext(AssetCatalog catalog, Set<AssetKey> cast) {
        this.catalog = catalog;
        this.cast = cast;
    }

    void require(boolean ok, String problem) {
        if (!ok) {
            problems.add(problem);
        }
    }

    void requireInCast(AssetKey character) {
        require(cast.contains(character), "El personaje no está en el reparto.");
    }

    void requireActor(AssetKey character, String animation, String expression) {
        requireInCast(character);
        require(catalog.animations().contains(animation), "La animación «" + animation + "» no existe.");
        catalog.character(character).ifPresent(c ->
                require(c.expressions().contains(expression), c.name() + " no tiene la expresión «" + expression + "»."));
    }

    void requireGait(String gait) {
        require(catalog.gaits().contains(gait), "«" + gait + "» no sirve para desplazarse.");
    }

    void requireProp(AssetKey prop) {
        require(catalog.props().contains(prop), "El objeto «" + prop + "» no existe.");
    }

    void requirePlace(AssetKey environment, String ambience) {
        require(catalog.environments().contains(environment), "El lugar «" + environment + "» no existe.");
        requireAmbience(ambience);
    }

    void requireAmbience(String ambience) {
        require(catalog.ambiences().contains(ambience), "El ambiente «" + ambience + "» no existe.");
    }

    List<String> problems() {
        return List.copyOf(problems);
    }
}
