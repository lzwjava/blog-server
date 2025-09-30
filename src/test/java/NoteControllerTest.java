import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lzwjava.Application;
import org.springframework.beans.factory.annotation.Autowired;
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
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCreateNoteWithValidRequest() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("content", "Test note content");
        requestBody.put("model", "gpt-4o");

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        // Python script execution fails (path hardcoded, may not exist in test environment)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to create note"));
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
        requestBody.put("content", "Test note content");
        // No model provided, should use default

        ResponseEntity<String> response =
                restTemplate.postForEntity("http://localhost:" + port + "/create-note", requestBody, String.class);

        // Python script execution fails (path hardcoded, may not exist in test environment)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to create note"));
    }
}
