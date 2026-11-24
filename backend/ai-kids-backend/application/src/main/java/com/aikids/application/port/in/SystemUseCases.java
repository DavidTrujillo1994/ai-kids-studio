package com.aikids.application.port.in;

import com.aikids.application.shared.ToolStatus;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.shared.AssetKey;
import java.nio.file.Path;
import java.util.List;

public final class SystemUseCases {

    private SystemUseCases() {
    }

    public interface GetCatalog {
        AssetCatalog catalog();
    }

    public interface GetAssetImages {
        Path portrait(AssetKey character);

        Path view(AssetKey environment);
    }

    public interface CheckTools {
        List<ToolStatus> check();
    }

    public interface PrepareFirstUse {
        /** Crea la serie inicial la primera vez y suma a ella los personajes dibujados después. */
        void prepare();
    }
}
