package com.aikids.application.port.in;

import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.ProductionJob;
import java.util.List;
import java.util.Optional;

public final class ProductionUseCases {

    private ProductionUseCases() {
    }

    public interface StartProduction {
        /** Si el episodio ya tiene un trabajo activo devuelve ese mismo: un doble clic no lo duplica. */
        ProductionJob start(EpisodeId id);
    }

    public interface CancelProduction {
        ProductionJob cancel(JobId id);
    }

    public interface GetProduction {
        ProductionJob get(JobId id);

        Optional<ProductionJob> latestOf(EpisodeId id);

        List<ProductionJob> active();
    }

    public interface RecoverProduction {
        /** Al arrancar, cierra los trabajos que quedaron a medias. */
        void recover();
    }
}
