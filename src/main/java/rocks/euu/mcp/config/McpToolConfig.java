package rocks.euu.mcp.config;

import rocks.euu.mcp.tools.OpenProjectTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {
    
    @Bean
    public ToolCallbackProvider openProjectToolCallbackProvider(OpenProjectTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
