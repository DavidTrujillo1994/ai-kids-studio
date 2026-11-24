package com.aikids.infrastructure.storage;

import com.aikids.application.port.out.AssetImagePort;
import com.aikids.domain.shared.AssetKey;
import com.aikids.infrastructure.EngineSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class FileAssetImageAdapter implements AssetImagePort {

    private final EngineSettings settings;

    FileAssetImageAdapter(EngineSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<Path> portrait(AssetKey character) {
        return image("characters", character, "retrato.png");
    }

    @Override
    public Optional<Path> view(AssetKey environment) {
        return image("environments", environment, "miniatura.png");
    }

    private Optional<Path> image(String kind, AssetKey key, String file) {
        return Optional.of(settings.media().resolve(kind).resolve(key.value()).resolve(file)).filter(Files::isRegularFile);
    }
}
