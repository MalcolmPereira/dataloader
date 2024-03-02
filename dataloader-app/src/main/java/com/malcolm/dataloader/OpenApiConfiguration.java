package com.malcolm.dataloader;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Open API Configuration for Swagger Documentation
 *
 * @author Malcolm
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * Custom OpenAPI configuration
     *
     * @return OpenAPI Open API configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("High Volume Data Loader")
                        .version("1.0")
                        .description("Simple REST API to facilitate high volume CSV parsing and data loading."));
    }
}
