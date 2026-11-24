package com.aikids.domain.catalog;

import com.aikids.domain.shared.AssetKey;
import java.util.List;
import java.util.Optional;

/**
 * Lo que el motor de animación sabe hacer hoy: el vocabulario permitido en un guion.
 * {@code gaits} son las animaciones que sirven para desplazarse.
 */
public record AssetCatalog(List<CharacterAsset> characters, List<AssetKey> environments, List<String> ambiences, List<AssetKey> props,
                           List<String> animations, List<String> gaits) {

    public AssetCatalog {
        characters = List.copyOf(characters);
        environments = List.copyOf(environments);
        ambiences = List.copyOf(ambiences);
        props = List.copyOf(props);
        animations = List.copyOf(animations);
        gaits = List.copyOf(gaits);
    }

    public Optional<CharacterAsset> character(AssetKey key) {
        return characters.stream().filter(c -> c.key().equals(key)).findFirst();
    }
}
