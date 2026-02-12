package rocks.euu.mcp.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import rocks.euu.mcp.model.Project;
import rocks.euu.mcp.model.Relation;
import rocks.euu.mcp.model.WorkPackage;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OpenProjectClient using WireMock to simulate the OpenProject API.
 * 
 * These tests verify that the client correctly communicates with the API.
 * To test against a real OpenProject instance, see OpenProjectClientRealApiTest.
 */
class OpenProjectClientTest {
    
    private static WireMockServer wireMockServer;
    private OpenProjectClient client;
    
    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }
    
    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }
    
    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        
        String credentials = "apikey:test-api-key";
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8089/api/v3")
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        client = new OpenProjectClient(webClient);
    }
    
    @Test
    @DisplayName("listProjects should return all accessible projects")
    void testListProjects() {
        // Given
        stubFor(get(urlEqualTo("/api/v3/projects"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "_embedded": {
                                    "elements": [
                                        {
                                            "id": 1,
                                            "identifier": "my-project",
                                            "name": "My Project",
                                            "active": true
                                        },
                                        {
                                            "id": 2,
                                            "identifier": "another-project",
                                            "name": "Another Project",
                                            "active": true
                                        }
                                    ]
                                }
                            }
                            """)));
        
        // When
        List<Project> projects = client.listProjects();
        
        // Then
        assertThat(projects).hasSize(2);
        assertThat(projects.get(0).getIdentifier()).isEqualTo("my-project");
        assertThat(projects.get(1).getName()).isEqualTo("Another Project");
    }
    
    @Test
    @DisplayName("listWorkPackages should return all work packages for a project")
    void testListWorkPackages() {
        // Given
        stubFor(get(urlEqualTo("/api/v3/projects/my-project/work_packages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "_embedded": {
                                    "elements": [
                                        {
                                            "id": 101,
                                            "subject": "Task 1",
                                            "startDate": "2025-02-01",
                                            "dueDate": "2025-02-05"
                                        },
                                        {
                                            "id": 102,
                                            "subject": "Task 2",
                                            "startDate": "2025-02-06",
                                            "dueDate": "2025-02-10"
                                        }
                                    ]
                                }
                            }
                            """)));
        
        // When
        List<WorkPackage> workPackages = client.listWorkPackages("my-project");
        
        // Then
        assertThat(workPackages).hasSize(2);
        assertThat(workPackages.get(0).getSubject()).isEqualTo("Task 1");
        assertThat(workPackages.get(0).getStartDate()).isEqualTo("2025-02-01");
        assertThat(workPackages.get(1).getId()).isEqualTo(102);
    }
    
    @Test
    @DisplayName("createWorkPackage should create a new work package and return it")
    void testCreateWorkPackage() {
        // Given
        stubFor(post(urlEqualTo("/api/v3/projects/my-project/work_packages"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 201,
                                "subject": "New Task",
                                "startDate": "2025-02-15",
                                "dueDate": "2025-02-20",
                                "_links": {
                                    "self": { "href": "/api/v3/work_packages/201" }
                                }
                            }
                            """)));
        
        // When
        WorkPackage wp = client.createWorkPackage(
                "my-project", 
                "New Task", 
                "2025-02-15", 
                "2025-02-20",
                "A new task description",
                null);
        
        // Then
        assertThat(wp.getId()).isEqualTo(201);
        assertThat(wp.getSubject()).isEqualTo("New Task");
        assertThat(wp.getStartDate()).isEqualTo("2025-02-15");
        
        // Verify the request was made with correct body
        verify(postRequestedFor(urlEqualTo("/api/v3/projects/my-project/work_packages"))
                .withRequestBody(containing("\"subject\": \"New Task\""))
                .withRequestBody(containing("\"startDate\": \"2025-02-15\"")));
    }
    
    @Test
    @DisplayName("createRelation should create a dependency between work packages")
    void testCreateRelation() {
        // Given
        stubFor(post(urlEqualTo("/api/v3/work_packages/102/relations"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 301,
                                "type": "follows",
                                "_links": {
                                    "from": { "href": "/api/v3/work_packages/102" },
                                    "to": { "href": "/api/v3/work_packages/101" }
                                }
                            }
                            """)));
        
        // When
        Relation relation = client.createRelation(102, 101, "follows");
        
        // Then
        assertThat(relation.getId()).isEqualTo(301);
        assertThat(relation.getType()).isEqualTo("follows");
        
        // Verify the request
        verify(postRequestedFor(urlEqualTo("/api/v3/work_packages/102/relations"))
                .withRequestBody(containing("\"type\": \"follows\""))
                .withRequestBody(containing("/api/v3/work_packages/102"))
                .withRequestBody(containing("/api/v3/work_packages/101")));
    }
    
    @Test
    @DisplayName("deleteWorkPackage should delete a work package")
    void testDeleteWorkPackage() {
        // Given
        stubFor(delete(urlEqualTo("/api/v3/work_packages/201"))
                .willReturn(aResponse()
                        .withStatus(204)));
        
        // When
        client.deleteWorkPackage(201);
        
        // Then
        verify(deleteRequestedFor(urlEqualTo("/api/v3/work_packages/201")));
    }
}
