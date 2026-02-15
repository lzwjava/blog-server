import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lzwjava.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class NoteControllerTest {

    @BeforeEach
    void setUpTestEnvironment() {
        // Use the hardcoded script path which may not exist in test environment
        // The controller will attempt to execute
        // /Users/lzwjava/projects/blog-source/scripts/create/create_note_from_clipboard.py
        createdFilePath = null;
    }

    @AfterEach
    void tearDownTestEnvironment() {
        // Clean up any created test files
        if (createdFilePath != null) {
            try {
                // The file paths are relative to blog-source directory
                Path filePath = Paths.get(blogSourcePath, createdFilePath);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
            createdFilePath = null;
        }
    }

    @LocalServerPort
    private int port;

    @Value("${blog.source.path}")
    private String blogSourcePath;

    private String createdFilePath;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCreateNoteWithValidRequest() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put(
                "content",
                "This is a test note content that is longer than 100 characters to satisfy the minimum length requirement for note creation. The script requires at least 100 characters, so we use this longer test content.");

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        // Note creation should succeed with valid parameters
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Note created successfully"));

        // Capture file path for cleanup
        if (response.getBody().contains("Note created successfully")) {
            createdFilePath = response.getBody().replace("Note created successfully: ", "");
        }
    }

    @Test
    void testCreateNoteWithMissingContent() {
        Map<String, String> requestBody = new HashMap<>();
        // No content provided

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Note content is required"));
    }

    @Test
    void testCreateNoteWithEmptyContent() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("content", "");

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Note content is required"));
    }

    @Test
    void testCreateNoteWithWhitespaceContent() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("content", "   ");

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Note content is required"));
    }

    @Test
    void testCreateNoteWithDefaultModel() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put(
                "content",
                "This is a test note content that is longer than 100 characters to satisfy the minimum length requirement for note creation. The script requires at least 100 characters, so we use this longer test content.");

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        // Note creation should succeed
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Note created successfully"));

        // Capture file path for cleanup
        if (response.getBody().contains("Note created successfully")) {
            createdFilePath = response.getBody().replace("Note created successfully: ", "");
        }
    }
}
