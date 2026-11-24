package com.aikids.application.service;

import com.aikids.application.port.in.ProductionUseCases.CancelProduction;
import com.aikids.application.port.in.ProductionUseCases.GetProduction;
import com.aikids.application.port.in.ProductionUseCases.RecoverProduction;
import com.aikids.application.port.in.ProductionUseCases.StartProduction;
import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.application.port.out.JobExecutionPort;
import com.aikids.application.port.out.ProductionJobRepositoryPort;
import com.aikids.application.port.out.TransactionPort;
import com.aikids.application.port.out.VideoProducerPort;
import com.aikids.application.port.out.VideoProducerPort.ProductionCancelledException;
import com.aikids.application.port.out.VideoProducerPort.ProductionFailedException;
import com.aikids.application.shared.NotFoundException;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.JobStatus;
import com.aikids.domain.production.ProductionJob;
import com.aikids.domain.production.Stage;
import com.aikids.domain.script.InvalidScriptException;
import com.aikids.domain.script.ScriptProblem;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class ProductionService implements StartProduction, CancelProduction, GetProduction, RecoverProduction {

    private final EpisodeRepositoryPort episodes;
    private final ProductionJobRepositoryPort jobs;
    private final AssetCatalogPort assets;
    private final VideoProducerPort producer;
    private final JobExecutionPort executor;
    private final TransactionPort transaction;
    private final Clock clock;

    public ProductionService(EpisodeRepositoryPort episodes, ProductionJobRepositoryPort jobs, AssetCatalogPort assets,
                             VideoProducerPort producer, JobExecutionPort executor, TransactionPort transaction, Clock clock) {
        this.episodes = episodes;
        this.jobs = jobs;
        this.assets = assets;
        this.producer = producer;
        this.executor = executor;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Override
    public ProductionJob start(EpisodeId id) {
        Start start = transaction.run(() -> {
            Episode episode = episodes.find(id).orElseThrow(() -> new NotFoundException("el episodio"));
            Optional<ProductionJob> running = jobs.latestOf(id).filter(j -> j.status().active());
            if (running.isPresent()) {
                return new Start(running.get(), false);
            }
            List<ScriptProblem> problems = episode.script().problems(assets.load());
            if (!problems.isEmpty()) {
                throw new InvalidScriptException(problems);
            }
            episode.startProduction(clock.instant());
            ProductionJob job = ProductionJob.queue(id, clock.instant());
            episodes.save(episode);
            jobs.save(job);
            return new Start(job, true);
        });
        if (start.isNew()) {
            executor.submit(() -> execute(start.job().id()));
        }
        return start.job();
    }

    @Override
    public ProductionJob cancel(JobId id) {
        ProductionJob job = transaction.run(() -> {
            ProductionJob found = get(id);
            if (found.status() == JobStatus.QUEUED) {
                found.cancel(clock.instant());
                jobs.save(found);
                withEpisode(found, e -> e.productionCancelled(clock.instant()));
            }
            return found;
        });
        if (job.status() == JobStatus.RUNNING) {
            producer.cancel(id);
        }
        return job;
    }

    @Override
    public ProductionJob get(JobId id) {
        return jobs.find(id).orElseThrow(() -> new NotFoundException("el trabajo"));
    }

    @Override
    public Optional<ProductionJob> latestOf(EpisodeId id) {
        return jobs.latestOf(id);
    }

    @Override
    public List<ProductionJob> active() {
        return jobs.active();
    }

    @Override
    public void recover() {
        transaction.run(() -> jobs.active().forEach(job -> {
            job.fail("La aplicación se cerró durante la producción. Vuelve a intentarlo: las voces ya hechas se conservan.",
                    "Interrumpido por el cierre de la aplicación", clock.instant());
            jobs.save(job);
            withEpisode(job, e -> e.productionFailed(clock.instant()));
        }));
    }

    private void execute(JobId id) {
        Optional<Episode> ready = transaction.run(() -> {
            ProductionJob job = get(id);
            if (job.status() != JobStatus.QUEUED) {
                return Optional.empty();
            }
            job.start(clock.instant());
            jobs.save(job);
            return episodes.find(job.episodeId());
        });
        ready.ifPresent(episode -> produce(id, episode));
    }

    private void produce(JobId id, Episode episode) {
        try {
            producer.produce(id, episode, (stage, percent, message) -> report(id, stage, percent, message));
            finish(id, job -> job.succeed(clock.instant()), e -> e.productionFinished(clock.instant()));
        } catch (ProductionCancelledException e) {
            finish(id, job -> job.cancel(clock.instant()), ep -> ep.productionCancelled(clock.instant()));
        } catch (ProductionFailedException e) {
            finish(id, job -> job.fail(e.getMessage(), e.detail(), clock.instant()), ep -> ep.productionFailed(clock.instant()));
        } catch (RuntimeException e) {
            finish(id, job -> job.fail("Ocurrió un error inesperado.", e.toString(), clock.instant()),
                    ep -> ep.productionFailed(clock.instant()));
        }
    }

    private void report(JobId id, Stage stage, int percent, String message) {
        transaction.run(() -> {
            ProductionJob job = get(id);
            job.report(stage, percent, message);
            jobs.save(job);
        });
    }

    private void finish(JobId id, Consumer<ProductionJob> onJob, Consumer<Episode> onEpisode) {
        transaction.run(() -> {
            ProductionJob job = get(id);
            onJob.accept(job);
            jobs.save(job);
            withEpisode(job, onEpisode);
        });
    }

    private void withEpisode(ProductionJob job, Consumer<Episode> action) {
        episodes.find(job.episodeId()).ifPresent(episode -> {
            action.accept(episode);
            episodes.save(episode);
        });
    }

    private record Start(ProductionJob job, boolean isNew) {
    }
}
