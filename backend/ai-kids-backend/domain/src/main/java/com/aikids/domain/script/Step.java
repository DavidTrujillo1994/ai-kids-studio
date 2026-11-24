package com.aikids.domain.script;

import com.aikids.domain.shared.AssetKey;
import java.util.List;
import java.util.function.UnaryOperator;

/** Un paso del guion. Cada tipo conoce sus propias reglas: agregar uno no obliga a tocar los demás. */
public sealed interface Step {

    int MIN_X = -450;
    int MAX_X = 1700;
    String DEFAULT_EXPRESSION = "normal";
    String DEFAULT_GAIT = "walk";

    void check(ScriptContext context);

    /** El mismo paso con otros actores y otro texto; lo usan las plantillas de historia. */
    Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> text);

    private static boolean inside(int x) {
        return x >= MIN_X && x <= MAX_X;
    }

    record Walk(AssetKey character, int to, String gait, String expression) implements Step {
        @Override
        public void check(ScriptContext context) {
            context.requireActor(character, gait, expression);
            context.requireGait(gait);
            context.require(inside(to), "El destino queda fuera del escenario.");
        }

        @Override
        public Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> text) {
            return new Walk(actor.apply(character), to, gait, expression);
        }
    }

    record Say(AssetKey character, String text, String animation, String expression) implements Step {
        public static final int MAX_TEXT = 160;

        @Override
        public void check(ScriptContext context) {
            context.requireActor(character, animation, expression);
            context.require(text != null && !text.isBlank(), "Escribe lo que dice el personaje.");
            context.require(text == null || text.length() <= MAX_TEXT,
                    "La frase es muy larga: divídela (máximo " + MAX_TEXT + " caracteres).");
            context.require(text == null || text.isBlank() || text.codePoints().anyMatch(Character::isLetter),
                    "La frase debe tener palabras.");
        }

        @Override
        public Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> words) {
            return new Say(actor.apply(character), words.apply(text), animation, expression);
        }
    }

    record Play(AssetKey character, String animation, String expression) implements Step {
        @Override
        public void check(ScriptContext context) {
            context.requireActor(character, animation, expression);
        }

        @Override
        public Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> text) {
            return new Play(actor.apply(character), animation, expression);
        }
    }

    record Pause(AssetKey character, double seconds, String animation, String expression) implements Step {
        @Override
        public void check(ScriptContext context) {
            context.requireActor(character, animation, expression);
            context.require(seconds >= 0.1 && seconds <= 10, "La pausa va de 0,1 a 10 segundos.");
        }

        @Override
        public Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> text) {
            return new Pause(actor.apply(character), seconds, animation, expression);
        }
    }

    record Appear(AssetKey prop, List<PathPoint> path) implements Step {
        public Appear {
            path = List.copyOf(path);
        }

        @Override
        public void check(ScriptContext context) {
            context.requireProp(prop);
            context.require(path.size() >= 2, "El objeto necesita un recorrido.");
            boolean ordered = true;
            for (int i = 1; i < path.size(); i++) {
                ordered &= path.get(i).second() > path.get(i - 1).second();
            }
            context.require(ordered, "Los tiempos del recorrido deben ir en aumento.");
        }

        @Override
        public Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> text) {
            return this;
        }
    }

    /** Cambia de lugar. Quien no tenga posición queda fuera de cuadro. */
    record Scene(AssetKey environment, String ambience, List<CastMember> positions) implements Step {
        public Scene {
            positions = List.copyOf(positions);
        }

        @Override
        public void check(ScriptContext context) {
            context.requirePlace(environment, ambience);
            for (CastMember p : positions) {
                context.requireInCast(p.character());
                context.require(inside(p.startX()), "Una posición queda fuera del escenario.");
            }
        }

        @Override
        public Step recast(UnaryOperator<AssetKey> actor, UnaryOperator<String> text) {
            return new Scene(environment, ambience, positions.stream().map(p -> new CastMember(actor.apply(p.character()), p.startX())).toList());
        }
    }
}
