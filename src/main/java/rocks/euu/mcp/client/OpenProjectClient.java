package rocks.euu.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import rocks.euu.mcp.model.Project;
import rocks.euu.mcp.model.Relation;
import rocks.euu.mcp.model.WorkPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenProjectClient {
    
    private final WebClient openProjectWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * List all projects accessible to the API user
     */
    public List<Project> listProjects() {
        try {
            String response = openProjectWebClient.get()
                    .uri("/projects")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode elements = root.path("_embedded").path("elements");
            
            List<Project> projects = new ArrayList<>();
            for (JsonNode element : elements) {
                projects.add(objectMapper.treeToValue(element, Project.class));
            }
            return projects;
            
        } catch (WebClientResponseException e) {
            log.error("Failed to list projects: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to list projects: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse projects response", e);
            throw new RuntimeException("Failed to parse projects: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a specific project by identifier
     */
    public Project getProject(String projectId) {
        try {
            return openProjectWebClient.get()
                    .uri("/projects/{projectId}", projectId)
                    .retrieve()
                    .bodyToMono(Project.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("Failed to get project {}: {} - {}", projectId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get project: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a new project
     */
    public Project createProject(String name, String identifier, String description, String parentId) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("{");
            body.append("\"name\":\"").append(escapeJson(name)).append("\"");
            body.append(",\"identifier\":\"").append(escapeJson(identifier)).append("\"");
            if (description != null) {
                body.append(",\"description\":{\"raw\":\"").append(escapeJson(description)).append("\"}");
            }
            if (parentId != null) {
                body.append(",\"_links\":{\"parent\":{\"href\":\"/api/v3/projects/").append(escapeJson(parentId)).append("\"}}");
            }
            body.append("}");
            
            log.debug("Creating project: {}", body);
            
            return openProjectWebClient.post()
                    .uri("/projects")
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(Project.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("Failed to create project '{}': {} - {}", name, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
        }
    }
    
    /**
     * List all work packages in a project
     */
    public List<WorkPackage> listWorkPackages(String projectId) {
        try {
            String response = openProjectWebClient.get()
                    .uri("/projects/{projectId}/work_packages", projectId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode elements = root.path("_embedded").path("elements");
            
            List<WorkPackage> workPackages = new ArrayList<>();
            for (JsonNode element : elements) {
                workPackages.add(objectMapper.treeToValue(element, WorkPackage.class));
            }
            return workPackages;
            
        } catch (WebClientResponseException e) {
            log.error("Failed to list work packages for project {}: {} - {}", 
                    projectId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to list work packages: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse work packages response", e);
            throw new RuntimeException("Failed to parse work packages: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a new work package in a project
     */
    public WorkPackage createWorkPackage(String projectId, String subject, 
                                          String startDate, String dueDate,
                                          String description, Integer typeId) {
        try {
            String typeHref = typeId != null ? "/api/v3/types/" + typeId : "/api/v3/types/1";
            
            String body = """
                {
                    "subject": "%s",
                    "description": { "raw": "%s" },
                    "startDate": %s,
                    "dueDate": %s,
                    "scheduleManually": true,
                    "_links": {
                        "type": { "href": "%s" }
                    }
                }
                """.formatted(
                    escapeJson(subject),
                    escapeJson(description != null ? description : ""),
                    startDate != null ? "\"" + startDate + "\"" : "null",
                    dueDate != null ? "\"" + dueDate + "\"" : "null",
                    typeHref
                );
            
            log.debug("Creating work package in project {}: {}", projectId, body);
            
            return openProjectWebClient.post()
                    .uri("/projects/{projectId}/work_packages", projectId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(WorkPackage.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("Failed to create work package in project {}: {} - {}", 
                    projectId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create work package: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a relation (dependency) between two work packages
     * 
     * @param fromId The work package that depends on another (successor)
     * @param toId The work package that must be completed first (predecessor)
     * @param type Relation type: "follows", "precedes", "blocks", "blocked", "relates", etc.
     */
    public Relation createRelation(int fromId, int toId, String type) {
        try {
            String body = """
                {
                    "type": "%s",
                    "_links": {
                        "from": { "href": "/api/v3/work_packages/%d" },
                        "to": { "href": "/api/v3/work_packages/%d" }
                    }
                }
                """.formatted(type, fromId, toId);
            
            log.debug("Creating relation: {}", body);
            
            return openProjectWebClient.post()
                    .uri("/work_packages/{fromId}/relations", fromId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Relation.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("Failed to create relation from {} to {}: {} - {}", 
                    fromId, toId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create relation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a work package by ID
     */
    public void deleteWorkPackage(int workPackageId) {
        try {
            openProjectWebClient.delete()
                    .uri("/work_packages/{id}", workPackageId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("Failed to delete work package {}: {} - {}", 
                    workPackageId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to delete work package: " + e.getMessage(), e);
        }
    }
    
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
