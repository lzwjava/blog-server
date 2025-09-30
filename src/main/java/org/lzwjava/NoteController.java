package org.lzwjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class NoteController {

    private static Logger logger = LoggerFactory.getLogger(NoteController.class);

    @CrossOrigin(origins = "*")
    @GetMapping("/bandwidth")
    public ResponseEntity<String> getBandwidth(@RequestParam(value = "i", required = false) String networkInterface) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();

            Process process;
            if (osName.contains("mac")) {
                process = new ProcessBuilder("ifconfig", "en0").start();
            } else {
                process = new ProcessBuilder("vnstat", "-i", networkInterface, "-5", "--json").start();
            }

            logger.info("getBandwidth: Executing command for OS: {}", osName);

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            // Capture standard output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            // Capture error output
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Prioritize stdout for JSON errors, fallback to stderr
                String errorResponse = output.length() > 0 ? output.toString() : errorOutput.toString();
                return ResponseEntity.status(500).body(errorResponse);
            }

            return ResponseEntity.ok(output.toString());

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error executing command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Command execution interrupted: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create-note")
    public ResponseEntity<String> createNote(@RequestBody Map<String, String> request) {
        String noteContent = request.get("content");
        String modelKey = request.getOrDefault("model", "gpt-4o"); // Default model

        if (noteContent == null || noteContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Note content is required");
        }

        try {
            // Step 1: Write note content to clipboard
            ProcessBuilder clipboardProcess;
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("mac")) {
                // macOS: Use pbcopy
                clipboardProcess = new ProcessBuilder("echo", "-n", noteContent).start();
                Process pbcopyProcess = new ProcessBuilder("pbcopy").start();
                clipboardProcess.getInputStream().transferTo(pbcopyProcess.getOutputStream());
                pbcopyProcess.waitFor();
                logger.info("Wrote note content to clipboard using pbcopy");
            } else if (osName.contains("linux")) {
                // Linux: Use xclip or xsel if available
                try {
                    clipboardProcess = new ProcessBuilder("echo", "-n", noteContent).start();
                    Process xclipProcess = new ProcessBuilder("xclip", "-selection", "clipboard").start();
                    clipboardProcess.getInputStream().transferTo(xclipProcess.getOutputStream());
                    xclipProcess.waitFor();
                    logger.info("Wrote note content to clipboard using xclip");
                } catch (Exception e) {
                    // Fallback to xsel
                    clipboardProcess = new ProcessBuilder("echo", "-n", noteContent).start();
                    Process xselProcess = new ProcessBuilder("xsel", "--clipboard", "--input").start();
                    clipboardProcess.getInputStream().transferTo(xselProcess.getOutputStream());
                    xselProcess.waitFor();
                    logger.info("Wrote note content to clipboard using xsel");
                }
            } else {
                return ResponseEntity.status(500).body("Unsupported OS for clipboard operations");
            }

            // Step 2: Execute create_note Python script
            logger.info("Executing create_note script with model: {}", modelKey);

            // Assuming the create_note.py script is in the blog-source repo
            // TODO: This path should be configurable via environment variable
            String scriptPath = System.getProperty("BLOG_SOURCE_PATH", "/path/to/blog-source");

            ProcessBuilder scriptProcess = new ProcessBuilder(
                "python3",
                scriptPath + "/create_note_from_clipboard.py",
                modelKey,
                "--only-create"  // Skip gpa() call as we'll handle Git operations separately
            );

            // Set working directory to script location
            scriptProcess.directory(new java.io.File(scriptPath));

            StringBuilder scriptOutput = new StringBuilder();
            StringBuilder scriptErrorOutput = new StringBuilder();

            Process script = scriptProcess.start();

            // Capture script output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(script.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    scriptOutput.append(line).append(System.lineSeparator());
                    logger.info("Script output: {}", line);
                }
            }

            // Capture script error output
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
}
