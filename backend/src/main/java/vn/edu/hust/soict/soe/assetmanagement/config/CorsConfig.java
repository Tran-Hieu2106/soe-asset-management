package vn.edu.hust.soict.soe.assetmanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration.
 * Allows the Vite frontend (localhost:5173) to call the Spring API (localhost:8080).
 * Merged logic from WebConfig.java to prevent Bean conflicts.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**") // Apply to ALL endpoints globally
                .allowedOrigins(
                        "http://localhost:5173",   // M4's Vite dev server
                        "http://localhost:3000"    // Fallback if team uses Create React App
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Accept")
                .allowCredentials(true) // Crucial for passing the JWT token via Authorization header
                .maxAge(3600); // Cache the OPTIONS preflight response for 1 hour to speed up requests
    }
}