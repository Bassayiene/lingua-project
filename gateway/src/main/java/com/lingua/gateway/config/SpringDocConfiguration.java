package com.lingua.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfiguration {

    private static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Gateway API").description("Comptes et authentification").version("0.0.1"))
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
