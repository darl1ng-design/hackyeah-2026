package pl.hubmalopolski.hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import java.util.Locale;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/csrf", "/api/v1/innovations/**",
                                "/api/v1/areas", "/api/v1/resources", "/api/v1/regions",
                                "/api/v1/matches/*", "/api/v1/ideas", "/api/v1/ideas/*",
                                "/api/v1/transcribe/health", "/api/v1/grant-calls").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/matches",
                                "/api/v1/ideas/assistant", "/api/v1/ideas/assistant/parse", "/api/v1/register",
                                "/api/v1/transcribe").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        PathPatternRequestMatcher.pathPattern("/api/v1/**")))
                .formLogin(withDefaults())
                .logout(l -> l.logoutSuccessUrl("/").permitAll());
        return http.build();
    }

    @Bean
    UserDetailsService users(AppUserRepository users) {
        return email -> {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            var account = users.findByEmail(normalized)
                    .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
            return User.withUsername(account.getEmail()).password(account.getPasswordHash())
                    .roles(account.getRole().name()).build();
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
