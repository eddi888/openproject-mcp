package rocks.euu.mcp.client;

import rocks.euu.mcp.model.Project;
import rocks.euu.mcp.model.WorkPackage;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests against a real OpenProject instance.
 * 
 * These tests are disabled by default and only run when environment variables are set:
 *   OPENPROJECT_BASE_URL=https://your-instance.openproject.com
 *   OPENPROJECT_API_KEY=your-api-key
 *   OPENPROJECT_TEST_PROJECT=your-test-project-slug
 * 
 * To run these tests:
 *   export OPENPROJECT_BASE_URL=https://your-instance.openproject.com
 *   export OPENPROJECT_API_KEY=your-api-key
 *   export OPENPROJECT_TEST_PROJECT=demo-project
 *   mvn test -Dtest=OpenProjectClientRealApiTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("OpenProject Real API Integration Tests")
class OpenProjectClientRealApiTest {
    
    private static OpenProjectClient client;
    private static String testProjectId;
    private static Integer createdWorkPackageId;
    
    @BeforeAll
    static void setUp() {
        String baseUrl = System.getenv("OPENPROJECT_BASE_URL");
        String apiKey = System.getenv("OPENPROJECT_API_KEY");
        testProjectId = "1"; //System.getenv("OPENPROJECT_TEST_PROJECT");


        
        // Skip all tests if environment is not configured
        assumeTrue(baseUrl != null && !baseUrl.isEmpty(), 
                "OPENPROJECT_BASE_URL not set - skipping real API tests");
        assumeTrue(apiKey != null && !apiKey.isEmpty(), 
                "OPENPROJECT_API_KEY not set - skipping real API tests");
        assumeTrue(testProjectId != null && !testProjectId.isEmpty(), 
                "OPENPROJECT_TEST_PROJECT not set - skipping real API tests");
        
        String credentials = "apikey:" + apiKey;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl + "/api/v3")
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        client = new OpenProjectClient(webClient);
        
        System.out.println("Running tests against: " + baseUrl);
        System.out.println("Test project: " + testProjectId);
    }
    
    @Test
    @Order(1)
    @DisplayName("Should list all accessible projects")
    void testListProjects() {
        List<Project> projects = client.listProjects();
        
        assertThat(projects).isNotEmpty();
        System.out.println("Found " + projects.size() + " projects:");
        projects.forEach(p -> System.out.println("  - " + p.getIdentifier() + ": " + p.getName()));
    }
    
    @Test
    @Order(2)
    @DisplayName("Should get test project details")
    void testGetProject() {
        Project project = client.getProject(testProjectId);
        
        assertThat(project).isNotNull();
        assertThat(project.getIdentifier()).isNotEmpty();
        System.out.println("Test project: " + project.getName());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should list work packages in test project")
    void testListWorkPackages() {
        List<WorkPackage> workPackages = client.listWorkPackages(testProjectId);
        
        System.out.println("Found " + workPackages.size() + " work packages:");
        workPackages.forEach(wp -> 
                System.out.println("  - #" + wp.getId() + ": " + wp.getSubject()));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should create a new work package")
    void testCreateWorkPackage() {
        WorkPackage wp = client.createWorkPackage(
                testProjectId,
                "MCP Test Task - " + System.currentTimeMillis(),
                "2025-02-15",
                "2025-02-20",
                "This task was created by the MCP integration test",
                null);
        
        assertThat(wp.getId()).isNotNull();
        assertThat(wp.getSubject()).startsWith("MCP Test Task");
        
        createdWorkPackageId = wp.getId();
        System.out.println("Created work package #" + wp.getId() + ": " + wp.getSubject());
    }
    
    @Test
    @Order(5)
    @DisplayName("Should delete the created work package")
    void testDeleteWorkPackage() {
        assumeTrue(createdWorkPackageId != null, "No work package was created");
        
        client.deleteWorkPackage(createdWorkPackageId);
        System.out.println("Deleted work package #" + createdWorkPackageId);
    }
    
    @Test
    @Order(10)
    @DisplayName("Should create a complete project plan with dependencies")
    void testCreateProjectPlan() {
        // Create three tasks with dependencies
        WorkPackage design = client.createWorkPackage(
                testProjectId,
                "MCP Plan: Design - " + System.currentTimeMillis(),
                "2025-03-01",
                "2025-03-05",
                "Design phase",
                null);
        
        WorkPackage development = client.createWorkPackage(
                testProjectId,
                "MCP Plan: Development - " + System.currentTimeMillis(),
                "2025-03-06",
                "2025-03-15",
                "Development phase",
                null);
        
        WorkPackage testing = client.createWorkPackage(
                testProjectId,
                "MCP Plan: Testing - " + System.currentTimeMillis(),
                "2025-03-16",
                "2025-03-20",
                "Testing phase",
                null);
        
        // Create dependencies: Development follows Design, Testing follows Development
        client.createRelation(development.getId(), design.getId(), "follows");
        client.createRelation(testing.getId(), development.getId(), "follows");
        
        System.out.println("Created project plan:");
        System.out.println("  #" + design.getId() + ": " + design.getSubject());
        System.out.println("  #" + development.getId() + ": " + development.getSubject() + " (follows #" + design.getId() + ")");
        System.out.println("  #" + testing.getId() + ": " + testing.getSubject() + " (follows #" + development.getId() + ")");
        
        // Cleanup
        client.deleteWorkPackage(testing.getId());
        client.deleteWorkPackage(development.getId());
        client.deleteWorkPackage(design.getId());
        System.out.println("Cleaned up test work packages");
    }
}
