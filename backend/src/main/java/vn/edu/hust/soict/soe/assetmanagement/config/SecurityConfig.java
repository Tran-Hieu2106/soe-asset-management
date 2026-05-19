package vn.edu.hust.soict.soe.assetmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.hust.soict.soe.assetmanagement.auth.filter.JwtAuthFilter;

/**
 * Spring Security configuration.
 *
 * URL access rules per role:
 *  - POST /api/auth/login       → public (no auth required)
 *  - GET  /swagger-ui/**        → public (dev only)
 *  - GET  /api/users/me         → any authenticated user
 *  - /api/users/**              → SYSTEM_ADMIN only
 *  - /api/assets/**             → SYSTEM_ADMIN, ASSET_MANAGER, FINANCE_AUDIT (read), APPROVING_AUTH (read)
 *  - /api/stock/**              → SYSTEM_ADMIN, WAREHOUSE, APPROVING_AUTH (approve), FINANCE_AUDIT (read)
 *  - /api/handovers/**          → SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH
 *  - /api/liquidations/**       → SYSTEM_ADMIN, ASSET_MANAGER, APPROVING_AUTH
 *  - /api/reports/**            → SYSTEM_ADMIN, FINANCE_AUDIT, APPROVING_AUTH
 *  - /api/audit-logs/**         → SYSTEM_ADMIN, FINANCE_AUDIT
 *
 * Fine-grained per-method control is handled via @PreAuthorize in controllers.
 * EnableMethodSecurity enables @PreAuthorize annotations.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT API
            .csrf(AbstractHttpConfigurer::disable)

            // Use CORS config from CorsConfig.java
            .cors(cors -> {})

            // Stateless session — no server-side session storage
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL-level access rules
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                // Swagger UI — open in dev (restrict in production)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Current user profile — any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/users/me")
                    .authenticated()
                    
                // User management — SYSTEM_ADMIN only
                .requestMatchers("/api/users/**")
                    .hasRole("SYSTEM_ADMIN")

                // Fixed assets — ASSET_MANAGER creates/updates, others read
                .requestMatchers(HttpMethod.GET, "/api/assets/**")
                    .hasAnyRole("SYSTEM_ADMIN", "ASSET_MANAGER",
                                "FINANCE_AUDIT", "APPROVING_AUTH")
                .requestMatchers("/api/assets/**")
                    .hasAnyRole("SYSTEM_ADMIN", "ASSET_MANAGER")

                // Stock — WAREHOUSE records, APPROVING_AUTH approves, FINANCE_AUDIT reads
                .requestMatchers(HttpMethod.GET, "/api/stock/**", "/api/materials/**")
                    .hasAnyRole("SYSTEM_ADMIN", "WAREHOUSE",
                                "APPROVING_AUTH", "FINANCE_AUDIT")
                .requestMatchers("/api/stock/**", "/api/materials/**")
                    .hasAnyRole("SYSTEM_ADMIN", "WAREHOUSE")

                // Handover and liquidation
                .requestMatchers("/api/handovers/**", "/api/liquidations/**")
                    .hasAnyRole("SYSTEM_ADMIN", "ASSET_MANAGER", "APPROVING_AUTH")

                // Reports and audit log
                .requestMatchers(HttpMethod.GET, "/api/reports/**")
                    .hasAnyRole("SYSTEM_ADMIN", "FINANCE_AUDIT", "APPROVING_AUTH")
                .requestMatchers(HttpMethod.GET, "/api/audit-logs/**")
                    .hasAnyRole("SYSTEM_ADMIN", "FINANCE_AUDIT")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Register JWT filter before Spring's username/password filter
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /*
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }*/
}