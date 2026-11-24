package com.aikids.domain.episode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aikids.domain.script.CastMember;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.Step;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidDataException;
import com.aikids.domain.shared.InvalidStateException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EpisodeTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final AssetKey BRUNO = new AssetKey("bruno");
    private static final AssetKey BOSQUE = new AssetKey("bosque-central");
    private static final Script SCRIPT = new Script(List.of(new CastMember(BRUNO, 0)), List.of(new Step.Play(BRUNO, "jump", "happy")));

    private static Episode episode() {
        return Episode.create(SeriesId.create(), "  ¡La Ñapa de Bruno! ", "", BOSQUE, SCRIPT, NOW);
    }

    private static Episode rendered() {
        Episode e = episode();
        e.startProduction(NOW);
        e.productionFinished(NOW);
        return e;
    }

    @Test
    void createsASafeFolderFromTheTitle() {
        assertThat(episode().folder().value()).matches("la-napa-de-bruno-[0-9a-f]{8}");
        assertThat(episode().title()).isEqualTo("¡La Ñapa de Bruno!");
    }

    @Test
    void withoutScriptItStaysADraftAndCannotBeProduced() {
        Episode e = Episode.create(SeriesId.create(), "x", "", BOSQUE, Script.empty(), NOW);
        assertThat(e.status()).isEqualTo(EpisodeStatus.DRAFT);
        assertThatThrownBy(() -> e.startProduction(NOW)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void cannotBeApprovedWithoutAFinishedRender() {
        Episode e = episode();
        assertThatThrownBy(() -> e.approve("", NOW)).isInstanceOf(InvalidStateException.class);
        e.startProduction(NOW);
        assertThatThrownBy(() -> e.approve("", NOW)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void approvedEpisodeIsLockedAndExportable() {
        Episode e = rendered();
        assertThatThrownBy(e::requireExportable).isInstanceOf(InvalidStateException.class);
        e.approve("Genial", NOW);
        e.requireExportable();
        assertThatThrownBy(() -> e.edit("otro", "", BOSQUE, SCRIPT, NOW)).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> e.startProduction(NOW)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void rejectingRequiresAReasonAndKeepsTheStateWhenMissing() {
        Episode e = rendered();
        assertThatThrownBy(() -> e.reject(" ", NOW)).isInstanceOf(InvalidDataException.class);
        assertThat(e.status()).isEqualTo(EpisodeStatus.PENDING_APPROVAL);
        e.reject("Muy rápido", NOW);
        assertThat(e.reviews()).singleElement().extracting(Review::comments).isEqualTo("Muy rápido");
    }

    @Test
    void changingTheScriptInvalidatesTheVideoButRenamingDoesNot() {
        Episode e = rendered();
        e.edit("Nuevo título", "idea", BOSQUE, SCRIPT, NOW);
        assertThat(e.status()).isEqualTo(EpisodeStatus.PENDING_APPROVAL);
        e.edit("Nuevo título", "idea", BOSQUE, new Script(SCRIPT.cast(), List.of(new Step.Play(BRUNO, "wave", "happy"))), NOW);
        assertThat(e.status()).isEqualTo(EpisodeStatus.SCRIPT_READY);
    }

    @Test
    void cannotBeEditedOrRemovedWhileInProduction() {
        Episode e = episode();
        e.startProduction(NOW);
        assertThatThrownBy(() -> e.edit("x", "", BOSQUE, SCRIPT, NOW)).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(e::requireDeletable).isInstanceOf(InvalidStateException.class);
        e.productionCancelled(NOW);
        assertThat(e.status()).isEqualTo(EpisodeStatus.SCRIPT_READY);
    }
}
