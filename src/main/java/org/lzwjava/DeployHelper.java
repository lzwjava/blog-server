package org.lzwjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployHelper {

    private static final Logger logger = LoggerFactory.getLogger(DeployHelper.class);
    private static final String SERVER_IP = System.getenv("SERVER_IP");
    private static final String SSH_USERNAME = "root";
    private static final String JAR_PATH = "target/blog-server-1.0.jar";
    private static final String REMOTE_PATH = "/root/app.jar";

    public static void main(String[] args) {
        try {
            deployJarToHetzner();
        } catch (IOException | InterruptedException e) {
            logger.error("Error during deployment", e);
        }
    }

    public static void deployJarToHetzner() throws IOException, InterruptedException {
        // Command to transfer the JAR file using SCP
        String[] scpCommand = {"scp", JAR_PATH, SSH_USERNAME + "@" + SERVER_IP + ":" + REMOTE_PATH};
        executeCommand(scpCommand, "JAR file uploaded successfully.");

        // Command to check and kill existing Java process
        String[] pgrepCommand = {"ssh", SSH_USERNAME + "@" + SERVER_IP, "pgrep -f 'java -jar'"};
        Process pgrepProcess = new ProcessBuilder(pgrepCommand).start();
        int pgrepExitCode = pgrepProcess.waitFor();

        if (pgrepExitCode == 0) {
            String[] pkillCommand = {"ssh", SSH_USERNAME + "@" + SERVER_IP, "pkill -f 'java -jar'"};
            executeCommand(pkillCommand, "Existing Java process killed.");
        } else {
            logger.info("No existing Java process found.");
        }

        // Command to start the JAR file on the server
        String[] startCommand = {
            "ssh", SSH_USERNAME + "@" + SERVER_IP, "nohup java -jar " + REMOTE_PATH + " > app.log 2>&1 &"
        };
        executeCommand(startCommand, "JAR file started on the server.");
    }

    private static void executeCommand(String[] command, String successMessage)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            logger.info(line);
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            logger.info(successMessage);
        } else {
            logger.error("Command failed with exit code: " + exitCode);
        }
    }
}
