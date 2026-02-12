package rocks.euu.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import rocks.euu.mcp.client.OpenProjectClient;
import rocks.euu.mcp.model.Project;
import rocks.euu.mcp.model.Relation;
import rocks.euu.mcp.model.WorkPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenProjectTools {
    
    private final OpenProjectClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Tool(description = "List all accessible projects in OpenProject. " +
          "Returns project names, identifiers, and IDs.")
    public String listProjects() {
        log.info("Listing all projects");
        List<Project> projects = client.listProjects();
        return toJson(projects);
    }
    
    @Tool(description = "Create a new project in OpenProject. " +
          "Returns the created project with its ID and identifier.")
    public String createProject(
            @ToolParam(description = "Display name of the project") 
            String name,
            @ToolParam(description = "URL-friendly identifier (slug), e.g. 'my-project'. Lowercase, hyphens allowed, no spaces.") 
            String identifier,
            @ToolParam(description = "Optional description of the project") 
            String description,
            @ToolParam(description = "Optional parent project identifier or numeric ID for sub-projects") 
            String parentId) {
        
        log.info("Creating project '{}' ({})", name, identifier);
        Project project = client.createProject(name, identifier, description, parentId);
        return toJson(project);
    }
    
    @Tool(description = "List all work packages (tasks) in an OpenProject project. " +
          "Returns IDs, subjects, dates, and status for Gantt chart planning.")
    public String listWorkPackages(
            @ToolParam(description = "Project identifier (slug) or numeric ID") 
            String projectId) {
        log.info("Listing work packages for project: {}", projectId);
        List<WorkPackage> workPackages = client.listWorkPackages(projectId);
        return toJson(workPackages);
    }
    
    @Tool(description = "Create a new work package (task) in an OpenProject project. " +
          "Use this to add tasks to a Gantt chart. Returns the created work package with its ID.")
    public String createWorkPackage(
            @ToolParam(description = "Project identifier (slug) or numeric ID") 
            String projectId,
            @ToolParam(description = "Title/subject of the work package") 
            String subject,
            @ToolParam(description = "Start date in YYYY-MM-DD format, e.g. 2025-02-15") 
            String startDate,
            @ToolParam(description = "Due date in YYYY-MM-DD format, e.g. 2025-02-20") 
            String dueDate,
            @ToolParam(description = "Optional description of the task") 
            String description) {
        
        log.info("Creating work package '{}' in project {}", subject, projectId);
        WorkPackage wp = client.createWorkPackage(projectId, subject, startDate, dueDate, description, null);
        return toJson(wp);
    }
    
    @Tool(description = "Create a dependency (relation) between two work packages for Gantt scheduling. " +
          "Use 'follows' type to indicate that one task must wait for another to complete. " +
          "For example: 'Testing follows Development' means Testing starts after Development ends.")
    public String createDependency(
            @ToolParam(description = "ID of the successor work package (the one that waits)") 
            int successorId,
            @ToolParam(description = "ID of the predecessor work package (the one that must complete first)") 
            int predecessorId) {
        
        log.info("Creating dependency: {} follows {}", successorId, predecessorId);
        Relation relation = client.createRelation(successorId, predecessorId, "follows");
        return toJson(relation);
    }
    
    @Tool(description = "Create a complete project plan with multiple tasks and dependencies in one call. " +
          "Provide a JSON array of tasks with their dependencies. Each task needs: " +
          "subject, startDate, dueDate, and optionally dependsOn (array of task indices).")
    public String createProjectPlan(
            @ToolParam(description = "Project identifier (slug) or numeric ID") 
            String projectId,
            @ToolParam(description = "JSON array of tasks, e.g.: " +
                "[{\"subject\":\"Design\",\"startDate\":\"2025-02-01\",\"dueDate\":\"2025-02-05\"}," +
                "{\"subject\":\"Development\",\"startDate\":\"2025-02-06\",\"dueDate\":\"2025-02-15\",\"dependsOn\":[0]}," +
                "{\"subject\":\"Testing\",\"startDate\":\"2025-02-16\",\"dueDate\":\"2025-02-20\",\"dependsOn\":[1]}]") 
            String tasksJson) {
        
        log.info("Creating project plan in project {}", projectId);
        
        try {
            TaskDefinition[] tasks = objectMapper.readValue(tasksJson, TaskDefinition[].class);
            int[] createdIds = new int[tasks.length];
            
            // First pass: create all work packages
            for (int i = 0; i < tasks.length; i++) {
                TaskDefinition task = tasks[i];
                WorkPackage wp = client.createWorkPackage(
                        projectId, 
                        task.subject, 
                        task.startDate, 
                        task.dueDate, 
                        task.description, 
                        null);
                createdIds[i] = wp.getId();
                log.info("Created work package '{}' with ID {}", task.subject, wp.getId());
            }
            
            // Second pass: create dependencies
            int relationsCreated = 0;
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i].dependsOn != null) {
                    for (int depIndex : tasks[i].dependsOn) {
                        if (depIndex >= 0 && depIndex < createdIds.length) {
                            client.createRelation(createdIds[i], createdIds[depIndex], "follows");
                            relationsCreated++;
                            log.info("Created dependency: {} follows {}", createdIds[i], createdIds[depIndex]);
                        }
                    }
                }
            }
            
            return String.format(
                    "{\"success\":true,\"workPackagesCreated\":%d,\"relationsCreated\":%d,\"ids\":%s}",
                    tasks.length, 
                    relationsCreated, 
                    toJson(createdIds));
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse tasks JSON", e);
            return "{\"success\":false,\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}";
        }
    }
    
    @Tool(description = "Delete a work package by its ID")
    public String deleteWorkPackage(
            @ToolParam(description = "ID of the work package to delete") 
            int workPackageId) {
        
        log.info("Deleting work package {}", workPackageId);
        client.deleteWorkPackage(workPackageId);
        return "{\"success\":true,\"deleted\":" + workPackageId + "}";
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON", e);
            return "{\"error\":\"Serialization failed\"}";
        }
    }
    
    // Inner class for parsing task definitions
    private static class TaskDefinition {
        public String subject;
        public String startDate;
        public String dueDate;
        public String description;
        public int[] dependsOn;
    }
}
