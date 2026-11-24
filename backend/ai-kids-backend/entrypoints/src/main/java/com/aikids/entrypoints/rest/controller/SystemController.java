package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.SystemUseCases.CheckTools;
import com.aikids.application.port.in.SystemUseCases.GetAssetImages;
import com.aikids.application.port.in.SystemUseCases.GetCatalog;
import com.aikids.domain.catalog.AssetCatalog;
import com.aikids.domain.shared.AssetKey;
import com.aikids.entrypoints.rest.dto.Dtos.CatalogResponse;
import com.aikids.entrypoints.rest.dto.Dtos.CharacterAssetDto;
import com.aikids.entrypoints.rest.dto.Dtos.ToolResponse;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class SystemController {

    private final GetCatalog catalog;
    private final GetAssetImages images;
    private final CheckTools tools;

    SystemController(GetCatalog catalog, GetAssetImages images, CheckTools tools) {
        this.catalog = catalog;
        this.images = images;
        this.tools = tools;
    }

    @GetMapping("/catalog")
    CatalogResponse catalog() {
        AssetCatalog c = catalog.catalog();
        return new CatalogResponse(
                c.characters().stream().map(a -> new CharacterAssetDto(a.key().value(), a.name(), a.expressions())).toList(),
                c.environments().stream().map(AssetKey::value).toList(), c.ambiences(), c.props().stream().map(AssetKey::value).toList(), c.animations(), c.gaits());
    }

    @GetMapping("/catalog/characters/{key}/portrait")
    ResponseEntity<Resource> portrait(@PathVariable String key) {
        return png(images.portrait(new AssetKey(key)));
    }

    @GetMapping("/catalog/environments/{key}/view")
    ResponseEntity<Resource> view(@PathVariable String key) {
        return png(images.view(new AssetKey(key)));
    }

    private static ResponseEntity<Resource> png(Path file) {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(MediaType.IMAGE_PNG).body(new FileSystemResource(file));
    }

    @GetMapping("/tools")
    List<ToolResponse> tools() {
        return tools.check().stream().map(t -> new ToolResponse(t.name(), t.purpose(), t.ready(), t.detail())).toList();
    }
}
