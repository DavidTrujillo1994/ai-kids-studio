package com.aikids.application.port.out;

import com.aikids.domain.shared.AssetKey;
import java.nio.file.Path;
import java.util.Optional;

/** Imágenes para elegir: retratos de personajes y vistas de lugares. */
public interface AssetImagePort {

    Optional<Path> portrait(AssetKey character);

    Optional<Path> view(AssetKey environment);
}
