package com.lingua.media.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfiguration {

    private static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Media API").description("Upload des images de choix et des sons").version("0.0.1"))
            // Relative server: "Try it out" calls the host that serves Swagger, the gateway included
            .servers(List.of(new Server().url("/")))
            .components(
                new Components()
                    .addSecuritySchemes(
                        BEARER_JWT,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    )
            )
            .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
