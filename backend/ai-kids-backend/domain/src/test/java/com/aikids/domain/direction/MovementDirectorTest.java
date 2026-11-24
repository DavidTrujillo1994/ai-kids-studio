package com.aikids.domain.direction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.catalog.CharacterAsset;
import com.aikids.domain.script.CastMember;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.Step;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import com.aikids.domain.story.StoryTemplate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MovementDirectorTest {

    private static final AssetKey BRUNO = new AssetKey("bruno");
    private static final AssetKey LUNA = new AssetKey("luna");
    private static final AssetKey BOSQUE = new AssetKey("bosque-central");
    private static final AssetKey CIELO = new AssetKey("cielo");
    private static final AssetCatalog CATALOG = new AssetCatalog(
            List.of(new CharacterAsset(BRUNO, "Bruno", "", true, 1, List.of("normal", "happy", "scared")), new CharacterAsset(LUNA, "Luna", "", true, 1, List.of("normal", "happy"))),
            List.of(BOSQUE, CIELO), List.of("dia", "noche"), List.of(), List.of("idle", "walk", "talk", "fly", "celebrate", "scared", "think", "shrug"), List.of("walk", "fly"));
    private static final DirectionGuide GUIDE = new DirectionGuide(List.of(
            new DirectionRule(List.of("volar"), List.of("celebrate"), "happy", "fly"),
            new DirectionRule(List.of("miedo"), List.of("scared"), "scared", null),
            new DirectionRule(List.of("nadar"), List.of("bucear"), "happy", "swim")),
            List.of("think", "shrug"), List.of("talk"), List.of("talk"), Map.of(CIELO, "fly"));

    private static List<Step> direct(Step... steps) {
        return new MovementDirector(GUIDE, CATALOG).direct(new Script(List.of(new CastMember(BRUNO, 0)), List.of(steps)), BOSQUE).steps();
    }

    private static Step.Say say(String text) {
        return new Step.Say(BRUNO, text, "idle", "normal");
    }

    private static Step.Walk walk() {
        return new Step.Walk(BRUNO, 500, "walk", "normal");
    }

    @Test
    void whoSaysHeWillFlyActuallyFliesNext() {
        assertThat(direct(say("¡Hoy voy a VOLAR!"), walk(), walk())).containsExactly(
                new Step.Say(BRUNO, "¡Hoy voy a VOLAR!", "celebrate", "happy"), new Step.Walk(BRUNO, 500, "fly", "normal"), walk());
    }

    @Test
    void matchesWholeBeginningsOnly() {
        assertThat(((Step.Say) direct(say("Me gusta revolar")).getFirst()).animation()).isEqualTo("talk");
    }

    @Test
    void doesNotRepeatTheSameGestureInConsecutiveQuestions() {
        assertThat(direct(say("¿Qué hacemos?"), say("¿A dónde vamos?")).stream().map(s -> ((Step.Say) s).animation())).containsExactly("think", "shrug");
    }

    @Test
    void neverSuggestsWhatDoesNotExist() {
        Step.Say suggested = (Step.Say) direct(say("Quiero nadar")).getFirst();
        assertThat(suggested.animation()).isEqualTo("idle");
        assertThat(direct(say("Quiero nadar"), walk()).getLast()).isEqualTo(walk());
        Step.Say luna = new Step.Say(LUNA, "Tengo miedo", "idle", "normal");
        assertThat(((Step.Say) direct(luna).getFirst()).expression()).isEqualTo("normal");
    }

    @Test
    void thePlaceDecidesHowToMoveWhenNothingWasSaid() {
        assertThat(direct(new Step.Scene(CIELO, "dia", List.of()), walk()).getLast()).isEqualTo(new Step.Walk(BRUNO, 500, "fly", "normal"));
    }

    @Test
    void aSceneChangeIsValidated() {
        Script script = new Script(List.of(new CastMember(BRUNO, 0)), List.of(
                new Step.Scene(CIELO, "noche", List.of(new CastMember(BRUNO, 400))),
                new Step.Scene(new AssetKey("marte"), "eclipse", List.of(new CastMember(LUNA, 9000)))));
        assertThat(script.problems(CATALOG)).hasSize(4).allMatch(p -> p.step() == 1);
    }

    @Test
    void aTemplateIsCastWithTheChosenCharacters() {
        AssetKey one = new AssetKey("rol-1");
        AssetKey two = new AssetKey("rol-2");
        StoryTemplate template = new StoryTemplate(new AssetKey("x"), "X", "", "", 2, BOSQUE, new Script(List.of(new CastMember(one, 10), new CastMember(two, 20)), "noche",
                List.of(new Step.Say(two, "Hola {1}, soy {2}", "talk", "normal"), new Step.Scene(CIELO, "dia", List.of(new CastMember(one, 5))))));
        Script script = template.castWith(List.of(new StoryTemplate.Actor(LUNA, "Luna"), new StoryTemplate.Actor(BRUNO, "Bruno")));
        assertThat(script.ambience()).isEqualTo("noche");
        assertThat(script.steps()).containsExactly(new Step.Say(BRUNO, "Hola Luna, soy Bruno", "talk", "normal"), new Step.Scene(CIELO, "dia", List.of(new CastMember(LUNA, 5))));
        assertThatThrownBy(() -> template.castWith(List.of(new StoryTemplate.Actor(LUNA, "Luna")))).isInstanceOf(InvalidDataException.class);
    }
}
