package com.aikids.domain.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aikids.domain.character.CharacterRole;
import com.aikids.domain.character.DesignStatus;
import com.aikids.domain.character.StoryCharacter;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidStateException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductionJobTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Test
    void progressNeverGoesBackAndOnlyReaches100WhenItSucceeds() {
        ProductionJob job = ProductionJob.queue(EpisodeId.create(), NOW);
        job.start(NOW);
        job.report(Stage.VOICES, 40, "Voz 2 de 4");
        job.report(Stage.ANIMATION, 10, "Animando");
        assertThat(job.progress()).isEqualTo(40);
        job.report(Stage.VALIDATION, 500, "Validando");
        assertThat(job.progress()).isEqualTo(99);
        job.succeed(NOW);
        assertThat(job.progress()).isEqualTo(100);
    }

    @Test
    void aFinishedJobDoesNotChange() {
        ProductionJob job = ProductionJob.queue(EpisodeId.create(), NOW);
        job.cancel(NOW);
        assertThatThrownBy(() -> job.start(NOW)).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> job.fail("x", "y", NOW)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void approvedDesignIsLockedUntilThereIsANewVersion() {
        StoryCharacter bruno = StoryCharacter.register(SeriesId.create(), new AssetKey("bruno"), CharacterRole.PROTAGONISTA, "Bruno", "", 2);
        bruno.approveDesign();
        assertThatThrownBy(() -> bruno.updateProfile("Otro", "")).isInstanceOf(InvalidStateException.class);
        assertThat(bruno.detectDesign(2)).isFalse();
        assertThat(bruno.designStatus()).isEqualTo(DesignStatus.APROBADO);
        assertThat(bruno.detectDesign(3)).isTrue();
        assertThat(bruno.designStatus()).isEqualTo(DesignStatus.BORRADOR);
    }
}
