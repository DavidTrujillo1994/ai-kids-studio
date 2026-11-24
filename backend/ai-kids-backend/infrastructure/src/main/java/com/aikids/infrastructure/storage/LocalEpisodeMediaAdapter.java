package com.aikids.infrastructure.storage;

import com.aikids.application.port.out.EpisodeMediaPort;
import com.aikids.application.port.out.ExistingEpisodePort;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.Text;
import com.aikids.infrastructure.EngineSettings;
import com.aikids.infrastructure.script.ScriptJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
class LocalEpisodeMediaAdapter implements EpisodeMediaPort, ExistingEpisodePort {

    private static final Logger LOG = LoggerFactory.getLogger(LocalEpisodeMediaAdapter.class);
    private static final Map<String, String> EXPORTED = Map.of(
            "render/final.mp4", "video.mp4",
            "subtitles/subtitulos.srt", "subtitulos.srt",
            "thumbnail/miniatura.png", "miniatura.png",
            "script/guion.json", "guion.json");

    private final EngineSettings settings;
    private final ScriptJson json;

    LocalEpisodeMediaAdapter(EngineSettings settings, ScriptJson json) {
        this.settings = settings;
        this.json = json;
    }

    @Override
    public Optional<Path> video(AssetKey folder) {
        return existing(episode(folder).resolve("render/final.mp4"));
    }

    @Override
    public Optional<Path> thumbnail(AssetKey folder) {
        return existing(episode(folder).resolve("thumbnail/miniatura.png"));
    }

    @Override
    public String export(AssetKey folder, String name) {
        String slug = Text.slug(name);
        Path target = settings.media().resolve("exports").resolve((slug.isEmpty() ? "" : slug + "_") + folder.value());
        try {
            Files.createDirectories(target);
            for (Map.Entry<String, String> file : EXPORTED.entrySet()) {
                Path source = episode(folder).resolve(file.getKey());
                if (Files.exists(source)) {
                    Files.copy(source, target.resolve(file.getValue()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return settings.root().relativize(target).toString().replace('\\', '/');
    }

    @Override
    public List<Found> findAll() {
        Path parent = settings.media().resolve("episodes");
        if (!Files.isDirectory(parent)) {
            return List.of();
        }
        List<Found> found = new ArrayList<>();
        try (Stream<Path> folders = Files.list(parent)) {
            folders.sorted().filter(p -> Files.exists(p.resolve("script/guion.json"))).forEach(p -> read(p).ifPresent(found::add));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }

    private Optional<Found> read(Path folder) {
        try {
            JsonNode root = json.tree(Files.readString(folder.resolve("script/guion.json")));
            AssetKey key = new AssetKey(folder.getFileName().toString());
            return Optional.of(new Found(key, root.path("title").asString(key.value()), new AssetKey(root.path("environment").asString("")),
                    json.read(root), video(key).isPresent()));
        } catch (IOException | RuntimeException e) {
            LOG.warn("No se pudo incorporar {}: {}", folder, e.toString());
            return Optional.empty();
        }
    }

    private Path episode(AssetKey folder) {
        return settings.media().resolve("episodes").resolve(folder.value());
    }

    private static Optional<Path> existing(Path p) {
        return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }
}
