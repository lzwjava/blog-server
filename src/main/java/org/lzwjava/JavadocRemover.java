package org.lzwjava;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class JavadocRemover {

    public static void main(String[] args) {
        String directoryPath = ".";
        removeJavadocFromDirectory(directoryPath);
    }

    public static void removeJavadocFromDirectory(String directoryPath) {
        Path startPath = Paths.get(directoryPath);
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        removeJavadocFromFile(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void removeJavadocFromFile(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath));
            Pattern pattern = Pattern.compile("/\\*\\*.*?\\*/", Pattern.DOTALL);
            content = pattern.matcher(content).replaceAll("");
            Files.write(filePath, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
