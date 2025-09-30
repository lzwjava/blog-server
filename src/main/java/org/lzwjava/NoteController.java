package org.lzwjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Value("${blog.default.model}")
    private String defaultModel;

    @CrossOrigin(origins = "*")
    @GetMapping("/models")
    public ResponseEntity<List<String>> getModels() {
        try {
            String scriptPath = this.blogSourcePath + "/scripts/create/get_models.py";

            ProcessBuilder scriptProcess = new ProcessBuilder(this.pythonExecutablePath, scriptPath);
            scriptProcess.directory(new java.io.File(this.blogSourcePath));

            Process script = scriptProcess.start();
            StringBuilder scriptOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(script.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    scriptOutput.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = script.waitFor();
            if (exitCode != 0) {
                logger.error("get_models script failed with exit code: {}", exitCode);
                return ResponseEntity.status(500).body(Arrays.asList());
            }

            String[] modelKeys = scriptOutput.toString().trim().split(",");
            for (int i = 0; i < modelKeys.length; i++) {
                modelKeys[i] = modelKeys[i].trim();
            }

            return ResponseEntity.ok(Arrays.asList(modelKeys));
        } catch (IOException e) {
            logger.error("IO error getting models", e);
            return ResponseEntity.status(500).body(Arrays.asList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Getting models was interrupted", e);
            return ResponseEntity.status(500).body(Arrays.asList());
        } catch (Exception e) {
            logger.error("Unexpected error getting models", e);
            return ResponseEntity.status(500).body(Arrays.asList());
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create-note")
    public ResponseEntity<String> createNote(@RequestBody Map<String, String> request) {
        String noteContent = request.get("content");
        String modelKey = request.getOrDefault("model", defaultModel);

        if (noteContent == null || noteContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Note content is required");
        }

        try {
            return executeCreateNoteScript(modelKey, noteContent);
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

    private ResponseEntity<String> executeCreateNoteScript(String modelKey, String noteContent)
            throws IOException, InterruptedException {
        logger.info("Executing create_note_from_clipboard script with model: {}", modelKey);

        String scriptPath = this.blogSourcePath + "/scripts/create/create_note_from_clipboard.py";

        ProcessBuilder scriptProcess = new ProcessBuilder(
                this.pythonExecutablePath, scriptPath, "--content", noteContent, "--note-model", modelKey);
        scriptProcess.directory(new java.io.File(this.blogSourcePath));

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

            // Return 400 for argument/validation errors, 500 for system errors
            int statusCode =
                    errorMsg.contains("error: the following arguments are required") || errorMsg.contains("usage:")
                            ? 400
                            : 500;

            // Include detailed error output including traceback for debugging
            return ResponseEntity.status(statusCode)
                    .body("Failed to create note (exit code " + scriptExitCode + "):\n" + errorMsg
                            + (scriptOutput.length() > 0
                                    ? "\nScript output before error:\n" + scriptOutput.toString()
                                    : ""));
        }

        logger.info("Note created successfully");
        return ResponseEntity.ok("Note created successfully: " + scriptOutput.toString());
    }
}
