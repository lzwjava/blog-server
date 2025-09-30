package org.lzwjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoteController {

    private static Logger logger = LoggerFactory.getLogger(NoteController.class);

    @CrossOrigin(origins = "*")
    @PostMapping("/create-note")
    public ResponseEntity<String> createNote(@RequestBody Map<String, String> request) {
        String noteContent = request.get("content");
        String modelKey = request.getOrDefault("model", "gpt-4o");

        if (noteContent == null || noteContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Note content is required");
        }

        try {
            // Step 1: Write note content to clipboard
            ResponseEntity<String> clipboardResult = writeToClipboard(noteContent);
            if (clipboardResult != null) {
                return clipboardResult;
            }

            // Step 2: Execute create_note Python script
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

    private ResponseEntity<String> writeToClipboard(String content) throws IOException, InterruptedException {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac")) {
            return writeToMacClipboard(content);
        } else if (osName.contains("linux")) {
            return writeToLinuxClipboard(content);
        } else {
            return ResponseEntity.status(500).body("Unsupported OS for clipboard operations");
        }
    }

    private ResponseEntity<String> writeToMacClipboard(String content) throws IOException, InterruptedException {
        Process echoProcess = new ProcessBuilder("echo", "-n", content).start();
        Process pbcopyProcess = new ProcessBuilder("pbcopy").start();
        echoProcess.getInputStream().transferTo(pbcopyProcess.getOutputStream());
        pbcopyProcess.waitFor();
        logger.info("Wrote note content to clipboard using pbcopy");
        return null; // Success
    }

    private ResponseEntity<String> writeToLinuxClipboard(String content) throws IOException, InterruptedException {
        try {
            Process echoProcess = new ProcessBuilder("echo", "-n", content).start();
            Process xclipProcess = new ProcessBuilder("xclip", "-selection", "clipboard").start();
            echoProcess.getInputStream().transferTo(xclipProcess.getOutputStream());
            xclipProcess.waitFor();
            logger.info("Wrote note content to clipboard using xclip");
        } catch (Exception e) {
            // Fallback to xsel
            Process echoProcess = new ProcessBuilder("echo", "-n", content).start();
            Process xselProcess = new ProcessBuilder("xsel", "--clipboard", "--input").start();
            echoProcess.getInputStream().transferTo(xselProcess.getOutputStream());
            xselProcess.waitFor();
            logger.info("Wrote note content to clipboard using xsel");
        }
        return null; // Success
    }

    private ResponseEntity<String> executeCreateNoteScript(String modelKey) throws IOException, InterruptedException {
        logger.info("Executing create_note script with model: {}", modelKey);

        // TODO(#config): Configure blog source path via environment variable
        String scriptPath = System.getProperty("BLOG_SOURCE_PATH", "/path/to/blog-source");

        ProcessBuilder scriptProcess =
                new ProcessBuilder("python3", scriptPath + "/create_note_from_clipboard.py", modelKey, "--only-create");
        scriptProcess.directory(new java.io.File(scriptPath));

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
            logger.error("create_note script failed with exit code: {}", scriptExitCode);
            return ResponseEntity.status(500).body("Failed to create note: " + errorMsg);
        }

        logger.info("Note created successfully");
        return ResponseEntity.ok("Note created successfully: " + scriptOutput.toString());
    }
}
