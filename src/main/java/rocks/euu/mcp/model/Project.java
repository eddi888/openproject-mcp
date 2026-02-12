package rocks.euu.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    
    private Integer id;
    private String identifier;
    private String name;
    private Description description;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Description {
        private String raw;
        private String html;
    }
    private Boolean active;
    private Boolean public_;
    
    @JsonProperty("_links")
    private Links links;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private Link self;
        private Link parent;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
        private String title;
    }
}
