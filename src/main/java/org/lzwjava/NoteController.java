package org.lzwjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoteController {

    private static Logger logger = LoggerFactory.getLogger(NoteController.class);

    @Value("${blog.source.path}")
    private String blogSourcePath;

    @Value("${python.executable.path}")
    private String pythonExecutablePath;

    @CrossOrigin(origins = "*")
    @PostMapping("/create-note")
    public ResponseEntity<String> createNote(@RequestBody Map<String, String> request) {
        String noteContent = request.get("content");
        String modelKey = request.getOrDefault("model", "gpt-4o");

        if (noteContent == null || noteContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Note content is required");
        }

        try {
            return executeCreateNoteScript(modelKey);
        } catch (IOException e) {
            logger.error("IO error during note creation", e);
            return ResponseEntity.status(500).body("IO error during note creation: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Note creation was interrupted", e);
            return ResponseEntity.status(500).body("Note creation interrupted: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during note creation", e);
            return ResponseEntity.status(500).body("Unexpected error: " + e.getMessage());
        }
    }

    private ResponseEntity<String> executeCreateNoteScript(String modelKey) throws IOException, InterruptedException {
        logger.info("Executing create_note_from_clipboard script with model: {}", modelKey);

        String scriptPath = "/Users/lzwjava/projects/blog-source/scripts/create/create_note_from_clipboard.py";

        ProcessBuilder scriptProcess = new ProcessBuilder(this.pythonExecutablePath, scriptPath, modelKey);
        scriptProcess.directory(new java.io.File("/Users/lzwjava/projects/blog-source"));

        StringBuilder scriptOutput = new StringBuilder();
        StringBuilder scriptErrorOutput = new StringBuilder();

        Process script = scriptProcess.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(script.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                scriptOutput.append(line).append(System.lineSeparator());
                logger.info("Script output: {}", line);
            }
        }

        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(script.getErrorStream()))) {
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                scriptErrorOutput.append(errorLine).append(System.lineSeparator());
                logger.error("Script error: {}", errorLine);
            }
        }

        int scriptExitCode = script.waitFor();

        if (scriptExitCode != 0) {
            String errorMsg = scriptErrorOutput.length() > 0 ? scriptErrorOutput.toString() : "Script execution failed";
            logger.error("create_note_from_clipboard script failed with exit code: {}", scriptExitCode);
            return ResponseEntity.status(500).body("Failed to create note: " + errorMsg);
        }

        logger.info("Note created successfully");
        return ResponseEntity.ok("Note created successfully: " + scriptOutput.toString());
    }
}
