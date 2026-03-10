package com.example.opensample.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("OpenSample API")
                .description("Sample API for data examples backed by MariaDB, Liquibase, Cassandra, S3 and Kafka")
                .version("v1")
                .contact(new Contact().name("OpenSample Team"))
                .license(new License().name("Apache-2.0"))
        );
    }
}
