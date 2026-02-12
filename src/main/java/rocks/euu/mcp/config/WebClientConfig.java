package rocks.euu.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient openProjectWebClient(OpenProjectProperties properties) {
        String credentials = "apikey:" + properties.getApiKey();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl() + "/api/v3")
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
