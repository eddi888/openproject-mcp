package rocks.euu.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Relation {
    
    private Integer id;
    private String name;
    private String type;
    private String reverseType;
    private String description;
    private Integer delay;
    
    @JsonProperty("_links")
    private Links links;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private Link self;
        private Link from;
        private Link to;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
        private String title;
    }
}
