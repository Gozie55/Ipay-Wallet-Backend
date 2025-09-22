package com.ipayz.ipayz_backend.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

 
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 🔑 Add your allowed dev + prod origins here
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:8080"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization") // allow frontend to read JWT headers
                .allowCredentials(true); // allow cookies/auth headers if needed
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Allow serving uploaded files publicly
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
