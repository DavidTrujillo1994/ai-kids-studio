package com.aikids.entrypoints.rest.web;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** Sirve la aplicación Angular compilada: una sola dirección para todo. */
@Configuration
class FrontendConfig implements WebMvcConfigurer {

    private final String location;

    FrontendConfig(@Value("${aikids.frontend}") String frontend) {
        this.location = Path.of(frontend).toAbsolutePath().normalize().toUri().toString().replaceFirst("/?$", "/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations(location).resourceChain(false).addResolver(new PathResourceResolver() {
            @Override
            protected Resource getResource(String path, Resource base) throws IOException {
                Resource found = super.getResource(path, base);
                boolean appRoute = !path.startsWith("api/") && !path.contains(".");
                return found != null || !appRoute ? found : super.getResource("index.html", base);
            }
        });
    }
}
