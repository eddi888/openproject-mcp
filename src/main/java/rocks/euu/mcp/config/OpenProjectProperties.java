package rocks.euu.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "openproject")
public class OpenProjectProperties {
    
    /**
     * Base URL of your OpenProject instance, e.g. https://mycompany.openproject.com
     */
    private String baseUrl;
    
    /**
     * API key for authentication. Generate this in OpenProject under
     * My Account -> Access Tokens -> API
     */
    private String apiKey;
}
