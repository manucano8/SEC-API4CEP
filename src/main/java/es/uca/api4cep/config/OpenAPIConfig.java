package es.uca.api4cep.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenAPIConfig {

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     * @return A WebMvcConfigurer that sets up CORS mappings
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("*")
                        .allowedOrigins("https://localhost:443")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    // URL for the development server
    private String devUrl = "https://localhost:443";

    /**
     * Configures OpenAPI documentation settings.
     * @return An OpenAPI instance with server, info, and license details
     */
    @Bean
    public OpenAPI myOpenAPI() {
        // Create and configure the development server
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");

        // Set up contact information for API documentation
        Contact contact = new Contact();
        contact.setEmail("manuel.cano@uca.es");
        contact.setName("Manuel Cano-Crespo");

        // Set up contact information for API documentation
        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        // Create and configure API information
        Info info = new Info()
            .title("SEC-API4CEP")
            .version("1.0")
            .contact(contact)
            .description("Secure RESTful API for runtime management of a CEP engine")
            .license(mitLicense);

        // Return the configured OpenAPI instance
        return new OpenAPI().info(info).servers(List.of(devServer));
    }
}
