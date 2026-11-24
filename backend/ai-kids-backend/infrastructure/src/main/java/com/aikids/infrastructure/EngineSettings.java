package com.aikids.infrastructure;

import java.nio.file.Path;

/** Dónde vive el motor de producción en este computador. */
public record EngineSettings(Path root, Path python) {

    public Path media() {
        return root.resolve("media");
    }

    public Path producer() {
        return root.resolve("video-engine/scripts/producir.py");
    }
}
