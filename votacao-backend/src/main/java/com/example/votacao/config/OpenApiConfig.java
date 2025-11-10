package com.example.votacao.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Votação")
                        .description("Desafio Votação - Backend (v1)")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipe").email("aureliomaica@gmail.com")))
                        //.license(new License().name("MIT")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")))
                .externalDocs(new ExternalDocumentation()
                        .description("README")
                        .url(""));
    }
}