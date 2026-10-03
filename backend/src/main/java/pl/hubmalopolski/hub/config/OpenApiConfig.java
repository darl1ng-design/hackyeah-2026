package pl.hubmalopolski.hub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI: /swagger-ui.html, JSON: /v3/api-docs (generowane w runtime). */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI hubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hub Innowacji Spolecznych API")
                        .version("v1")
                        .description("""
                                                    REST API (sciezki /api/v1/**) dla frontendu.
                                                    Auth: login formularzowy Spring Security (POST /login z username/password/_csrf)
                                                    ustawia ciastko JSESSIONID — endpointy /api/v1 wysylaja je automatycznie
                                                    (w fetch: credentials: 'include').
                                                    UWAGA demo: /api/v1/** nie wymaga logowania i nie wymaga CSRF —
                                                    do wdrozenia zostac to zmienione (SecurityConfig)."""))
                .components(new Components().addSecuritySchemes("cookieAuth",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID")));
    }
}