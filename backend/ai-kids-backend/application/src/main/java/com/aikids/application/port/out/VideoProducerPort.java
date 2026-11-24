package com.aikids.application.port.out;

import com.aikids.domain.episode.Episode;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.Stage;

/** Convierte un episodio en video. Bloquea hasta terminar. */
public interface VideoProducerPort {

    void produce(JobId jobId, Episode episode, ProgressListener listener) throws ProductionFailedException, ProductionCancelledException;

    /** Pide detener el trabajo; no hace nada si ya no está corriendo. */
    void cancel(JobId jobId);

    @FunctionalInterface
    interface ProgressListener {
        void onProgress(Stage stage, int percent, String message);
    }

    final class ProductionFailedException extends Exception {
        private final String detail;

        public ProductionFailedException(String message, String detail) {
            super(message);
            this.detail = detail;
        }

        public String detail() {
            return detail;
        }
    }

    final class ProductionCancelledException extends Exception {
    }
}
