package rocks.euu.mcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkPackage {
    
    private Integer id;
    private String subject;
    private Description description;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Description {
        private String raw;
        private String html;
    }
    private String startDate;
    private String dueDate;
    private String estimatedTime;
    private Boolean scheduleManually;
    
    @JsonProperty("_links")
    private Links links;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private Link self;
        private Link project;
        private Link type;
        private Link status;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String href;
        private String title;
    }
}
