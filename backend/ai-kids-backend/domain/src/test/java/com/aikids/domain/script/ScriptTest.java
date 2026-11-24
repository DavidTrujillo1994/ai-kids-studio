package com.aikids.domain.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.catalog.CharacterAsset;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ScriptTest {

    private static final AssetKey BRUNO = new AssetKey("bruno");
    private static final AssetKey MARIPOSA = new AssetKey("mariposa");
    private static final AssetCatalog CATALOG = new AssetCatalog(
            List.of(new CharacterAsset(BRUNO, "Bruno", "", true, 1, List.of("normal", "happy"))),
            List.of(new AssetKey("bosque-central")), List.of("dia"), List.of(MARIPOSA), List.of("idle", "walk", "talk", "jump"), List.of("walk"));

    private static List<ScriptProblem> problems(Step... steps) {
        return new Script(List.of(new CastMember(BRUNO, 0)), List.of(steps)).problems(CATALOG);
    }

    @Test
    void acceptsAValidScript() {
        assertThat(problems(new Step.Walk(BRUNO, 500, "walk", "normal"), new Step.Say(BRUNO, "¡Hola!", "talk", "happy"),
                new Step.Play(BRUNO, "jump", "happy"), new Step.Pause(BRUNO, 1, "idle", "normal"),
                new Step.Appear(MARIPOSA, List.of(new PathPoint(0, 1400, 100), new PathPoint(2, 500, 200))))).isEmpty();
    }

    @Test
    void reportsTheExactStepOfEachProblem() {
        assertThat(problems(new Step.Say(BRUNO, "¡Hola!", "talk", "happy"), new Step.Play(BRUNO, "volar", "triste")))
                .extracting(ScriptProblem::step).containsExactly(1, 1);
    }

    @Test
    void rejectsUnknownCharactersPropsAndDestinations() {
        assertThat(problems(new Step.Play(new AssetKey("pepe"), "jump", "normal"))).isNotEmpty();
        assertThat(problems(new Step.Walk(BRUNO, 9000, "walk", "normal"))).isNotEmpty();
        assertThat(problems(new Step.Walk(BRUNO, 500, "jump", "normal"))).isNotEmpty();
        assertThat(problems(new Step.Appear(new AssetKey("dragon"), List.of(new PathPoint(0, 0, 0), new PathPoint(1, 0, 0))))).isNotEmpty();
        assertThat(problems(new Step.Appear(MARIPOSA, List.of(new PathPoint(2, 0, 0), new PathPoint(1, 0, 0))))).isNotEmpty();
    }

    @Test
    void rejectsEmptyTooLongOrWordlessDialogues() {
        assertThat(problems(new Step.Say(BRUNO, " ", "talk", "normal"))).isNotEmpty();
        assertThat(problems(new Step.Say(BRUNO, "¡¡¡...!!!", "talk", "normal"))).isNotEmpty();
        assertThat(problems(new Step.Say(BRUNO, "a".repeat(Step.Say.MAX_TEXT + 1), "talk", "normal"))).isNotEmpty();
    }

    @Test
    void anEmptyScriptIsAProblemAndTheCastHasNoDuplicates() {
        assertThat(problems()).singleElement().extracting(ScriptProblem::step).isEqualTo(-1);
        assertThatThrownBy(() -> new Script(List.of(new CastMember(BRUNO, 0), new CastMember(BRUNO, 9)), List.of()))
                .isInstanceOf(InvalidDataException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../secreto", "a/b", "C:", "Bruno", "", "a--b", "a b"})
    void assetKeysCannotBePaths(String value) {
        assertThatThrownBy(() -> new AssetKey(value)).isInstanceOf(InvalidDataException.class);
    }
}
