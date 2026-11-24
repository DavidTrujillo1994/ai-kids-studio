package com.aikids.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aikids")
@AutoConfigurationPackage(basePackages = "com.aikids")
public class AiKidsApplication {

    public static void main(String[] args) throws IOException {
        String root = System.getenv().getOrDefault("AIKIDS_ROOT", ".");
        Files.createDirectories(Path.of(root, "data"));
        SpringApplication.run(AiKidsApplication.class, args);
    }
}
