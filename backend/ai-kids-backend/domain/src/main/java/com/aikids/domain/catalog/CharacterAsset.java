package com.aikids.domain.catalog;

import com.aikids.domain.shared.AssetKey;
import java.util.List;

public record CharacterAsset(AssetKey key, String name, String personality, boolean protagonist, int designVersion, List<String> expressions) {

    public CharacterAsset {
        expressions = List.copyOf(expressions);
    }
}
