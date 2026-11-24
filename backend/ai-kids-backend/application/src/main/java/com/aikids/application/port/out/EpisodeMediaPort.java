package com.aikids.application.port.out;

import com.aikids.domain.shared.AssetKey;
import java.nio.file.Path;
import java.util.Optional;

/** Lectura de lo que se produjo para un episodio. */
public interface EpisodeMediaPort {

    Optional<Path> video(AssetKey folder);

    Optional<Path> thumbnail(AssetKey folder);

    /** Copia el episodio a la carpeta de exportación y devuelve su ruta relativa. */
    String export(AssetKey folder, String name);
}
