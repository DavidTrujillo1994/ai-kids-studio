package com.aikids.application.port.out;

import com.aikids.domain.script.Script;
import com.aikids.domain.shared.AssetKey;
import java.util.List;

/** Episodios hechos por consola que aún no están en la base de datos. */
public interface ExistingEpisodePort {

    List<Found> findAll();

    record Found(AssetKey folder, String title, AssetKey environment, Script script, boolean hasVideo) {
    }
}
