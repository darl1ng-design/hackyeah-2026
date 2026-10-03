package pl.hubmalopolski.hub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html, JSON: /v3/api-docs (generowane w runtime).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI hubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hub Innowacji Spolecznych API")
                        .version("v1")
                        .description("""
                                REST API dla mieszkańców, pracowników i administratora.
                                Publiczne: katalog, regiony, zatwierdzone pomysły, utworzenie pomysłu,
                                dopasowanie i asystent. Konto mieszkańca jest opcjonalne i daje dostęp
                                do /api/v1/ideas?mine=true. /api/v1/me oraz raporty pracownicze wymagają
                                sesji; /api/v1/admin/** wymaga roli ADMIN.
                                Rejestracja: POST /api/v1/register. Logowanie: POST /login jako formularz
                                username (adres e-mail), password, _csrf; odpowiedź ustawia JSESSIONID.
                                Przed POST/PATCH pobierz GET /api/v1/csrf i wyślij token w nagłówku
                                X-CSRF-TOKEN. Po logowaniu pobierz token ponownie. W fetch użyj
                                credentials: 'include'."""))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID"))
                        .addSecuritySchemes("csrfToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")));
    }
}
