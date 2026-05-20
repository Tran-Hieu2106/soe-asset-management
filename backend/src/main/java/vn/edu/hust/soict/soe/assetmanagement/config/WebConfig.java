/*
package vn.edu.hust.soict.soe.assetmanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOrigins("http://localhost:5173") // M4's Vite local server
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // Crucial for JWT tokens and secure cookies later
                .maxAge(3600); // Cache the preflight response for 1 hour
    }
}
*/
/*
This file is not needed as CORS is configured in CorsConfig.java
Already merged into CorsConfig.java
should not configure CORS 2 times in both files */