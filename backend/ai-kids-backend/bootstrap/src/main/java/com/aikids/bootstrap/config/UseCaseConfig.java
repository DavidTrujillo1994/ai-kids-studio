package com.aikids.bootstrap.config;

import com.aikids.application.port.out.AssetCatalogPort;
import com.aikids.application.port.out.AssetImagePort;
import com.aikids.application.port.out.CharacterRepositoryPort;
import com.aikids.application.port.out.DirectionGuidePort;
import com.aikids.application.port.out.EpisodeMediaPort;
import com.aikids.application.port.out.EpisodeRepositoryPort;
import com.aikids.application.port.out.ExistingEpisodePort;
import com.aikids.application.port.out.JobExecutionPort;
import com.aikids.application.port.out.ProductionJobRepositoryPort;
import com.aikids.application.port.out.SeriesRepositoryPort;
import com.aikids.application.port.out.StoryTemplatePort;
import com.aikids.application.port.out.ToolHealthPort;
import com.aikids.application.port.out.TransactionPort;
import com.aikids.application.port.out.VideoProducerPort;
import com.aikids.application.service.CharacterService;
import com.aikids.application.service.EpisodeService;
import com.aikids.application.service.ProductionService;
import com.aikids.application.service.ReviewService;
import com.aikids.application.service.SeriesService;
import com.aikids.application.service.StoryService;
import com.aikids.application.service.SystemService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Único lugar donde se conectan los casos de uso con sus adaptadores. */
@Configuration
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SeriesService seriesService(SeriesRepositoryPort series, Clock clock) {
        return new SeriesService(series, clock);
    }

    @Bean
    CharacterService characterService(CharacterRepositoryPort characters, SeriesRepositoryPort series, AssetCatalogPort assets) {
        return new CharacterService(characters, series, assets);
    }

    @Bean
    EpisodeService episodeService(EpisodeRepositoryPort episodes, SeriesRepositoryPort series, CharacterRepositoryPort characters,
                                  AssetCatalogPort assets, StoryTemplatePort templates, EpisodeMediaPort media, TransactionPort transaction, Clock clock) {
        return new EpisodeService(episodes, series, characters, assets, templates, media, transaction, clock);
    }

    @Bean
    StoryService storyService(StoryTemplatePort templates, DirectionGuidePort guide, AssetCatalogPort assets) {
        return new StoryService(templates, guide, assets);
    }

    @Bean
    ReviewService reviewService(EpisodeRepositoryPort episodes, EpisodeMediaPort media, TransactionPort transaction, Clock clock) {
        return new ReviewService(episodes, media, transaction, clock);
    }

    @Bean
    ProductionService productionService(EpisodeRepositoryPort episodes, ProductionJobRepositoryPort jobs, AssetCatalogPort assets,
                                        VideoProducerPort producer, JobExecutionPort executor, TransactionPort transaction, Clock clock) {
        return new ProductionService(episodes, jobs, assets, producer, executor, transaction, clock);
    }

    @Bean
    SystemService systemService(AssetCatalogPort assets, AssetImagePort images, ToolHealthPort tools, SeriesRepositoryPort series, CharacterRepositoryPort characters,
                                EpisodeRepositoryPort episodes, ExistingEpisodePort existing, TransactionPort transaction, Clock clock) {
        return new SystemService(assets, images, tools, series, characters, episodes, existing, transaction, clock);
    }
}
