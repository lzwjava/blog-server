package org.lzwjava;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    @Value("${blog.source.path}")
    private String blogSourcePath;

    @Value("${github.token:${GITHUB_TOKEN:}}")
    private String githubToken;

    private final OpenRouterService openRouterService;

    public NoteController(OpenRouterService openRouterService) {
        this.openRouterService = openRouterService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/create-note")
    public ResponseEntity<String> createNote(@RequestBody Map<String, String> request) {
        String noteContent = request.get("content");

        if (noteContent == null || noteContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Note content is required");
        }

        try {
            String title = generateTitle(noteContent);
            if (title == null) {
                return ResponseEntity.status(500).body("Failed to generate title");
            }

            String shortTitle = processTitleForFilename(title);
            String notePath = createFilename(shortTitle);
            String frontMatter = formatFrontMatter(title);
            String cleanedContent = cleanContent(noteContent);

            writeNote(notePath, frontMatter, cleanedContent);

            // Git operations
            try {
                gitPush(notePath);
            } catch (Exception gitException) {
                logger.error("Git push failed but note was created locally", gitException);
                return ResponseEntity.ok("Note created successfully but git push failed: " + notePath + ". Error: "
                        + gitException.getMessage());
            }

            return ResponseEntity.ok("Note created and pushed successfully: " + notePath);
        } catch (Exception e) {
            logger.error("Error creating note", e);
            return ResponseEntity.status(500).body("Error creating note: " + e.getMessage());
        }
    }

    private String generateTitle(String content) {
        String prompt =
                "Generate a concise and engaging title for the following content. Respond with ONLY the title:\n\n"
                        + content;
        String title = openRouterService.callOpenRouterApi(prompt);
        if (title != null) {
            title = title.replace("*", " ").trim();
        }
        return title;
    }

    private String processTitleForFilename(String title) {
        String processed = title.trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9-]", "")
                .toLowerCase();
        return processed;
    }

    private String createFilename(String shortTitle) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String notesDir = blogSourcePath + "/notes";
        File dir = new File(notesDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String baseFileName = String.format("%s-%s-en.md", dateStr, shortTitle);
        Path path = Paths.get(notesDir, baseFileName);

        int counter = 1;
        while (Files.exists(path)) {
            String fileNameWithCounter = String.format("%s-%s-%d-en.md", dateStr, shortTitle, counter);
            path = Paths.get(notesDir, fileNameWithCounter);
            counter++;
        }
        return path.toString();
    }

    private String formatFrontMatter(String fullTitle) {
        String title = fullTitle;
        if (title.contains(":") && !title.startsWith("\"")) {
            title = "\"" + title + "\"";
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return String.format(
                """
                ---
                audio: false
                generated: true
                image: false
                lang: en
                layout: post
                title: %s
                translated: false
                type: note
                ---""",
                title);
    }

    private String cleanContent(String content) {
        String[] lines = content.split("\\r?\\n");
        if (lines.length > 0 && lines[0].startsWith("# ")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                sb.append(lines[i]).append(System.lineSeparator());
            }
            return sb.toString().trim();
        }
        return content.trim();
    }

    private void writeNote(String filePath, String frontMatter, String content) throws IOException {
        Path path = Paths.get(filePath);
        String fullContent = frontMatter + "\n\n" + content;
        Files.writeString(path, fullContent, StandardCharsets.UTF_8);
        logger.info("Created note: {}", filePath);
    }

    private void gitPush(String notePath) throws IOException, InterruptedException {
        String fileName = new File(notePath).getName();

        executeGitCommand(new String[] {"git", "add", "notes/" + fileName}, "Staged note");
        executeGitCommand(new String[] {"git", "commit", "-m", "feat: add note " + fileName}, "Committed note");

        if (githubToken != null && !githubToken.isEmpty()) {
            String remoteUrl =
                    String.format("https://x-access-token:%s@github.com/lzwjava/blog-source.git", githubToken);
            executeGitCommand(new String[] {"git", "push", remoteUrl, "main"}, "Pushed to remote");
        } else {
            logger.warn("GitHub token not provided, skipping git push");
            throw new IOException("GitHub token is missing");
        }
    }

    private void executeGitCommand(String[] command, String successMessage) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(blogSourcePath));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Git output: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Git command failed with exit code " + exitCode);
        }
        logger.info(successMessage);
    }
}
