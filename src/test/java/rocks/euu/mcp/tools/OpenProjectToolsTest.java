package rocks.euu.mcp.tools;

import rocks.euu.mcp.client.OpenProjectClient;
import rocks.euu.mcp.model.Project;
import rocks.euu.mcp.model.Relation;
import rocks.euu.mcp.model.WorkPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenProjectTools Unit Tests")
class OpenProjectToolsTest {
    
    @Mock
    private OpenProjectClient mockClient;
    
    private OpenProjectTools tools;
    
    @BeforeEach
    void setUp() {
        tools = new OpenProjectTools(mockClient);
    }
    
    @Test
    @DisplayName("listProjects should return JSON array of projects")
    void testListProjects() {
        // Given
        Project project = new Project();
        project.setId(1);
        project.setIdentifier("test-project");
        project.setName("Test Project");
        when(mockClient.listProjects()).thenReturn(List.of(project));
        
        // When
        String result = tools.listProjects();
        
        // Then
        assertThat(result).contains("test-project");
        assertThat(result).contains("Test Project");
        verify(mockClient).listProjects();
    }
    
    @Test
    @DisplayName("listWorkPackages should return JSON array of work packages")
    void testListWorkPackages() {
        // Given
        WorkPackage wp = new WorkPackage();
        wp.setId(101);
        wp.setSubject("Test Task");
        wp.setStartDate("2025-02-01");
        wp.setDueDate("2025-02-05");
        when(mockClient.listWorkPackages("my-project")).thenReturn(List.of(wp));
        
        // When
        String result = tools.listWorkPackages("my-project");
        
        // Then
        assertThat(result).contains("Test Task");
        assertThat(result).contains("2025-02-01");
        verify(mockClient).listWorkPackages("my-project");
    }
    
    @Test
    @DisplayName("createWorkPackage should create and return work package")
    void testCreateWorkPackage() {
        // Given
        WorkPackage wp = new WorkPackage();
        wp.setId(201);
        wp.setSubject("New Task");
        when(mockClient.createWorkPackage(
                eq("my-project"), 
                eq("New Task"), 
                eq("2025-02-15"), 
                eq("2025-02-20"),
                eq("Description"),
                isNull()))
                .thenReturn(wp);
        
        // When
        String result = tools.createWorkPackage(
                "my-project", 
                "New Task", 
                "2025-02-15", 
                "2025-02-20",
                "Description");
        
        // Then
        assertThat(result).contains("201");
        assertThat(result).contains("New Task");
        verify(mockClient).createWorkPackage(
                "my-project", "New Task", "2025-02-15", "2025-02-20", "Description", null);
    }
    
    @Test
    @DisplayName("createDependency should create relation between work packages")
    void testCreateDependency() {
        // Given
        Relation relation = new Relation();
        relation.setId(301);
        relation.setType("follows");
        when(mockClient.createRelation(102, 101, "follows")).thenReturn(relation);
        
        // When
        String result = tools.createDependency(102, 101);
        
        // Then
        assertThat(result).contains("follows");
        verify(mockClient).createRelation(102, 101, "follows");
    }
    
    @Test
    @DisplayName("createProjectPlan should create multiple tasks with dependencies")
    void testCreateProjectPlan() {
        // Given
        WorkPackage wp1 = new WorkPackage();
        wp1.setId(1001);
        wp1.setSubject("Design");
        
        WorkPackage wp2 = new WorkPackage();
        wp2.setId(1002);
        wp2.setSubject("Development");
        
        WorkPackage wp3 = new WorkPackage();
        wp3.setId(1003);
        wp3.setSubject("Testing");
        
        Relation rel1 = new Relation();
        rel1.setId(2001);
        
        Relation rel2 = new Relation();
        rel2.setId(2002);
        
        when(mockClient.createWorkPackage(eq("my-project"), eq("Design"), any(), any(), any(), any()))
                .thenReturn(wp1);
        when(mockClient.createWorkPackage(eq("my-project"), eq("Development"), any(), any(), any(), any()))
                .thenReturn(wp2);
        when(mockClient.createWorkPackage(eq("my-project"), eq("Testing"), any(), any(), any(), any()))
                .thenReturn(wp3);
        when(mockClient.createRelation(anyInt(), anyInt(), eq("follows")))
                .thenReturn(rel1, rel2);
        
        String tasksJson = """
            [
                {"subject":"Design","startDate":"2025-02-01","dueDate":"2025-02-05"},
                {"subject":"Development","startDate":"2025-02-06","dueDate":"2025-02-15","dependsOn":[0]},
                {"subject":"Testing","startDate":"2025-02-16","dueDate":"2025-02-20","dependsOn":[1]}
            ]
            """;
        
        // When
        String result = tools.createProjectPlan("my-project", tasksJson);
        
        // Then
        assertThat(result).contains("\"success\":true");
        assertThat(result).contains("\"workPackagesCreated\":3");
        assertThat(result).contains("\"relationsCreated\":2");
        
        // Verify all work packages were created
        verify(mockClient).createWorkPackage(eq("my-project"), eq("Design"), any(), any(), any(), any());
        verify(mockClient).createWorkPackage(eq("my-project"), eq("Development"), any(), any(), any(), any());
        verify(mockClient).createWorkPackage(eq("my-project"), eq("Testing"), any(), any(), any(), any());
        
        // Verify relations were created
        verify(mockClient).createRelation(1002, 1001, "follows"); // Development follows Design
        verify(mockClient).createRelation(1003, 1002, "follows"); // Testing follows Development
    }
    
    @Test
    @DisplayName("createProjectPlan should handle invalid JSON gracefully")
    void testCreateProjectPlanWithInvalidJson() {
        // When
        String result = tools.createProjectPlan("my-project", "not valid json");
        
        // Then
        assertThat(result).contains("\"success\":false");
        assertThat(result).contains("error");
    }
    
    @Test
    @DisplayName("deleteWorkPackage should delete and return success")
    void testDeleteWorkPackage() {
        // When
        String result = tools.deleteWorkPackage(201);
        
        // Then
        assertThat(result).contains("\"success\":true");
        assertThat(result).contains("201");
        verify(mockClient).deleteWorkPackage(201);
    }
}
