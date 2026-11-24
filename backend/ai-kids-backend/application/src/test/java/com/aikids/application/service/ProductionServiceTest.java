package com.aikids.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aikids.application.port.out.VideoProducerPort;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeStatus;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.JobStatus;
import com.aikids.domain.production.ProductionJob;
import com.aikids.domain.production.Stage;
import com.aikids.domain.script.CastMember;
import com.aikids.domain.script.InvalidScriptException;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.Step;
import com.aikids.domain.series.SeriesId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductionServiceTest {

    private final Fakes.Episodes episodes = new Fakes.Episodes();
    private final Fakes.Jobs jobs = new Fakes.Jobs();
    private final Deque<Runnable> queue = new ArrayDeque<>();
    private final FakeProducer producer = new FakeProducer();
    private final ProductionService service = new ProductionService(episodes, jobs, () -> Fakes.CATALOG, producer, queue::add,
            Fakes.TRANSACTION, Fakes.CLOCK);
    private Episode episode;

    @BeforeEach
    void setUp() {
        episode = Episode.create(SeriesId.create(), "Prueba", "", Fakes.BOSQUE,
                new Script(List.of(new CastMember(Fakes.BRUNO, 0)), List.of(new Step.Play(Fakes.BRUNO, "jump", "happy"))), Fakes.NOW);
        episodes.save(episode);
    }

    private void runQueue() {
        while (!queue.isEmpty()) {
            queue.poll().run();
        }
    }

    @Test
    void producesAndLeavesTheEpisodePendingOfHumanApproval() {
        ProductionJob job = service.start(episode.id());
        assertThat(episode.status()).isEqualTo(EpisodeStatus.IN_PRODUCTION);
        runQueue();
        assertThat(service.get(job.id()).status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(episode.status()).isEqualTo(EpisodeStatus.PENDING_APPROVAL);
    }

    @Test
    void aDoubleClickDoesNotDuplicateTheJob() {
        assertThat(service.start(episode.id()).id()).isEqualTo(service.start(episode.id()).id());
        assertThat(queue).hasSize(1);
    }

    @Test
    void anInvalidScriptIsNotProduced() {
        episode.edit("Prueba", "", Fakes.BOSQUE, new Script(List.of(), List.of(new Step.Play(Fakes.BRUNO, "volar", "happy"))), Fakes.NOW);
        assertThatThrownBy(() -> service.start(episode.id())).isInstanceOf(InvalidScriptException.class);
        assertThat(episode.status()).isEqualTo(EpisodeStatus.SCRIPT_READY);
        assertThat(queue).isEmpty();
    }

    @Test
    void aFailureIsExplainedAndCanBeRetried() {
        producer.failure = new VideoProducerPort.ProductionFailedException("Falta Godot", "detalle");
        ProductionJob job = service.start(episode.id());
        runQueue();
        assertThat(service.get(job.id()).message()).isEqualTo("Falta Godot");
        assertThat(episode.status()).isEqualTo(EpisodeStatus.FAILED);
        producer.failure = null;
        service.start(episode.id());
        runQueue();
        assertThat(episode.status()).isEqualTo(EpisodeStatus.PENDING_APPROVAL);
    }

    @Test
    void anUnexpectedErrorDoesNotLeaveTheJobHanging() {
        producer.crash = true;
        ProductionJob job = service.start(episode.id());
        runQueue();
        assertThat(service.get(job.id()).status()).isEqualTo(JobStatus.FAILED);
        assertThat(episode.status()).isEqualTo(EpisodeStatus.FAILED);
    }

    @Test
    void aQueuedJobIsCancelledWithoutEverRunning() {
        ProductionJob job = service.start(episode.id());
        service.cancel(job.id());
        runQueue();
        assertThat(service.get(job.id()).status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(episode.status()).isEqualTo(EpisodeStatus.SCRIPT_READY);
        assertThat(producer.calls).isZero();
    }

    @Test
    void aRunningJobIsCancelledThroughTheProducer() {
        producer.cancelDuring = service;
        ProductionJob job = service.start(episode.id());
        runQueue();
        assertThat(service.get(job.id()).status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(episode.status()).isEqualTo(EpisodeStatus.SCRIPT_READY);
    }

    @Test
    void afterARestartTheInterruptedJobIsClosedAndCanBeRetried() {
        ProductionJob job = service.start(episode.id());
        queue.clear();
        service.recover();
        assertThat(service.get(job.id()).status()).isEqualTo(JobStatus.FAILED);
        assertThat(episode.status()).isEqualTo(EpisodeStatus.FAILED);
        assertThat(service.active()).isEmpty();
        service.start(episode.id());
        assertThat(queue).hasSize(1);
    }

    private static final class FakeProducer implements VideoProducerPort {
        private ProductionFailedException failure;
        private boolean crash;
        private ProductionService cancelDuring;
        private boolean cancelled;
        private int calls;

        @Override
        public void produce(JobId jobId, Episode e, ProgressListener listener) throws ProductionFailedException, ProductionCancelledException {
            calls++;
            listener.onProgress(Stage.VOICES, 30, "Voz 1 de 1");
            if (cancelDuring != null) {
                cancelDuring.cancel(jobId);
            }
            if (cancelled) {
                throw new ProductionCancelledException();
            }
            if (crash) {
                throw new IllegalStateException("inesperado");
            }
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        public void cancel(JobId jobId) {
            cancelled = true;
        }
    }
}
