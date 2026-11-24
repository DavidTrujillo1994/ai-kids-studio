package com.aikids.infrastructure.process;

import com.aikids.application.port.out.JobExecutionPort;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LocalJobExecutionAdapter implements JobExecutionPort {

    private static final Logger LOG = LoggerFactory.getLogger(LocalJobExecutionAdapter.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "produccion");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void submit(Runnable job) {
        executor.execute(() -> {
            try {
                job.run();
            } catch (RuntimeException e) {
                LOG.error("Un trabajo terminó con un error inesperado", e);
            }
        });
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
