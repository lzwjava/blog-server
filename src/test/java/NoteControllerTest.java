import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
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

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Note created successfully"));
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

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Note created successfully"));
    }
}
