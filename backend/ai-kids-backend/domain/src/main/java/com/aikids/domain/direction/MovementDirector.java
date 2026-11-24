package com.aikids.domain.direction;

import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.Step;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Propone movimientos y caras que encajen con lo que se dice y con el lugar, sin repetir el anterior. */
public final class MovementDirector {

    private final DirectionGuide guide;
    private final AssetCatalog catalog;

    public MovementDirector(DirectionGuide guide, AssetCatalog catalog) {
        this.guide = guide;
        this.catalog = catalog;
    }

    public Script direct(Script script, AssetKey environment) {
        Map<AssetKey, String> lastAnimation = new HashMap<>();
        Map<AssetKey, String> nextGait = new HashMap<>();
        Map<String, Integer> uses = new HashMap<>();
        AssetKey place = environment;
        List<Step> directed = new ArrayList<>();
        for (Step step : script.steps()) {
            switch (step) {
                case Step.Say s -> {
                    Optional<DirectionRule> rule = ruleFor(s.text());
                    String animation = pick(options(rule, s.text()), lastAnimation.get(s.character()), uses, s.animation());
                    String expression = rule.map(DirectionRule::expression).orElse(s.text().contains("!") ? "happy" : s.expression());
                    rule.map(DirectionRule::gait).filter(catalog.gaits()::contains).ifPresent(g -> nextGait.put(s.character(), g));
                    lastAnimation.put(s.character(), animation);
                    directed.add(new Step.Say(s.character(), s.text(), animation, available(s.character(), expression, s.expression())));
                }
                case Step.Walk w -> {
                    String said = nextGait.remove(w.character());
                    String usual = guide.environmentGaits().get(place);
                    String gait = said != null ? said : usual != null && catalog.gaits().contains(usual) ? usual : w.gait();
                    directed.add(new Step.Walk(w.character(), w.to(), gait, w.expression()));
                }
                case Step.Scene s -> {
                    place = s.environment();
                    directed.add(s);
                }
                default -> directed.add(step);
            }
        }
        return script.withSteps(directed);
    }

    private Optional<DirectionRule> ruleFor(String text) {
        String plain = Text.plain(text);
        return guide.rules().stream().filter(r -> r.matches(plain)).findFirst();
    }

    private List<String> options(Optional<DirectionRule> rule, String text) {
        return rule.map(DirectionRule::animations)
                .orElse(text.contains("?") ? guide.question() : text.contains("!") ? guide.exclamation() : guide.neutral());
    }

    private String pick(List<String> options, String previous, Map<String, Integer> uses, String fallback) {
        List<String> known = options.stream().filter(catalog.animations()::contains).toList();
        String chosen = known.stream().filter(a -> !a.equals(previous)).min((a, b) -> uses.getOrDefault(a, 0) - uses.getOrDefault(b, 0))
                .orElse(known.isEmpty() ? fallback : known.getFirst());
        uses.merge(chosen, 1, Integer::sum);
        return chosen;
    }

    private String available(AssetKey character, String wanted, String fallback) {
        return catalog.character(character).filter(c -> c.expressions().contains(wanted)).map(c -> wanted).orElse(fallback);
    }
}
