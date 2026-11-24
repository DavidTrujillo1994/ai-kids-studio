package com.aikids.infrastructure.storage;

import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.catalog.CharacterAsset;
import com.aikids.domain.shared.AssetKey;
import com.aikids.infrastructure.EngineSettings;
import com.aikids.infrastructure.script.ScriptJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** El catálogo se lee de media/: si agregas un personaje o una animación en disco, aparece en la aplicación. */
@Component
class FileAssetCatalogAdapter implements AssetCatalogPort {

    private final EngineSettings settings;
    private final ScriptJson json;

    FileAssetCatalogAdapter(EngineSettings settings, ScriptJson json) {
        this.settings = settings;
        this.json = json;
    }

    @Override
    public AssetCatalog load() {
        Path media = settings.media();
        List<CharacterAsset> characters = folders(media.resolve("characters"), "rig.json").stream().map(this::character).toList();
        JsonNode animations = readIfExists(media.resolve("characters/animaciones.json"));
        List<String> names = List.copyOf(animations.propertyNames());
        List<String> gaits = names.stream().filter(n -> animations.path(n).has("gait")).toList();
        List<String> ambiences = List.copyOf(readIfExists(media.resolve("environments/ambientes.json")).propertyNames());
        return new AssetCatalog(characters, keys(media.resolve("environments"), "escenario.json"), ambiences, keys(media.resolve("props"), "cuerpo.svg"), names, gaits);
    }

    private CharacterAsset character(Path folder) {
        JsonNode sheet = read(folder.resolve("ficha.json"));
        return new CharacterAsset(new AssetKey(folder.getFileName().toString()), sheet.path("name").asString(""),
                sheet.path("personality").asString(""), "protagonista".equals(sheet.path("role").asString("")), sheet.path("designVersion").asInt(1), expressions(folder));
    }

    /** Una expresión existe si tiene ojos y boca (la boca neutra es X). */
    private static List<String> expressions(Path folder) {
        List<String> found = new ArrayList<>(List.of("normal"));
        for (Path eyes : list(folder.resolve("ojos"))) {
            String name = eyes.getFileName().toString().replaceFirst("\\.svg$", "");
            if (!found.contains(name) && Files.exists(folder.resolve("bocas/" + name + ".svg"))) {
                found.add(name);
            }
        }
        return found;
    }

    private static List<AssetKey> keys(Path parent, String marker) {
        return folders(parent, marker).stream().map(p -> new AssetKey(p.getFileName().toString())).toList();
    }

    private static List<Path> folders(Path parent, String marker) {
        return list(parent).stream().filter(p -> Files.exists(p.resolve(marker))).toList();
    }

    private static List<Path> list(Path parent) {
        if (!Files.isDirectory(parent)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(parent)) {
            return files.sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode readIfExists(Path file) {
        return Files.exists(file) ? read(file) : json.tree("{}");
    }

    private JsonNode read(Path file) {
        try {
            return json.tree(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
