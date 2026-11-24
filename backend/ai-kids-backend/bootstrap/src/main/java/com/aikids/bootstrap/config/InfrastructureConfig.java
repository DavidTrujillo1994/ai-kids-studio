package com.aikids.bootstrap.config;

import com.aikids.application.port.in.ProductionUseCases.RecoverProduction;
import com.aikids.application.port.in.SystemUseCases.PrepareFirstUse;
import com.aikids.infrastructure.EngineSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class InfrastructureConfig {

    @Bean
    EngineSettings engineSettings(@Value("${aikids.root}") String root, @Value("${aikids.python}") String python) {
        // Como texto: Spring convertiría "." a un recurso del classpath y no a la carpeta actual.
        Path absolute = Path.of(root).toAbsolutePath().normalize();
        if (!Files.isDirectory(absolute.resolve("media/characters"))) {
            throw new IllegalStateException("No encuentro media/characters en " + absolute + ". Abre la aplicación con Iniciar.cmd.");
        }
        return new EngineSettings(absolute, absolute.resolve(python));
    }

    @Bean
    ApplicationRunner startUp(RecoverProduction recover, PrepareFirstUse prepare) {
        return args -> {
            recover.recover();
            prepare.prepare();
        };
    }
}
