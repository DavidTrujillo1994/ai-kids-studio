package com.aikids.application.port.in;

import com.aikids.domain.episode.Episode;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.ScriptProblem;
import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class EpisodeUseCases {

    private EpisodeUseCases() {
    }

    public interface CreateEpisode {
        /** Sin plantilla el episodio empieza sin guion; sin lugar, usa el de la plantilla. */
        Episode create(NewEpisode request);

        record NewEpisode(SeriesId seriesId, String title, String idea, Optional<AssetKey> environment, Optional<AssetKey> template, List<AssetKey> cast) {
        }
    }

    public interface EditEpisode {
        Episode edit(EpisodeId id, String title, String idea, AssetKey environment, Script script);
    }

    public interface GetEpisodes {
        List<Episode> ofSeries(SeriesId id);

        Episode get(EpisodeId id);

        List<ScriptProblem> problems(Episode episode);

        boolean hasVideo(Episode episode);
    }

    public interface GetEpisodeMedia {
        Path video(EpisodeId id);

        Path thumbnail(EpisodeId id);
    }

    public interface DeleteEpisode {
        void delete(EpisodeId id);
    }

    public interface ReviewEpisode {
        Episode approve(EpisodeId id, String comments);

        Episode reject(EpisodeId id, String comments);
    }

    public interface ExportEpisode {
        String export(EpisodeId id);
    }
}
