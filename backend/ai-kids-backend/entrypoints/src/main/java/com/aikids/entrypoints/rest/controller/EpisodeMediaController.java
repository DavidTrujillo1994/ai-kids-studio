package com.aikids.entrypoints.rest.controller;

import com.aikids.application.port.in.EpisodeUseCases.GetEpisodeMedia;
import com.aikids.application.port.in.EpisodeUseCases.GetEpisodes;
import com.aikids.domain.episode.EpisodeId;
import com.aikids.domain.shared.Text;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/episodes/{id}")
class EpisodeMediaController {

    private final GetEpisodeMedia media;
    private final GetEpisodes episodes;

    EpisodeMediaController(GetEpisodeMedia media, GetEpisodes episodes) {
        this.media = media;
        this.episodes = episodes;
    }

    /** Spring atiende las peticiones por rangos, así el reproductor puede adelantar y atrasar. */
    @GetMapping("/preview")
    ResponseEntity<Resource> video(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean download) {
        Path file = media.video(new EpisodeId(id));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(MediaType.valueOf("video/mp4"));
        if (download) {
            String name = Text.slug(episodes.get(new EpisodeId(id)).title());
            response.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename((name.isEmpty() ? "episodio" : name) + ".mp4").build().toString());
        }
        return response.body(new FileSystemResource(file));
    }

    @GetMapping("/thumbnail")
    ResponseEntity<Resource> thumbnail(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(media.thumbnail(new EpisodeId(id))));
    }
}
