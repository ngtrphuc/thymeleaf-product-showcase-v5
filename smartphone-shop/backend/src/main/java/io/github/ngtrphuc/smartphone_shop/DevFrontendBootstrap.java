package io.github.ngtrphuc.smartphone_shop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DevFrontendBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevFrontendBootstrap.class);
    private static final String AUTO_START_FRONTEND_PROPERTY = "smartphone.shop.dev.auto-start-frontend";
    private static final String AUTO_START_FRONTEND_ENV = "SMARTPHONE_SHOP_DEV_AUTO_START_FRONTEND";
    private static final String FRONTEND_URL_PROPERTY = "app.frontend.url";
    private static final String FRONTEND_URL_ENV = "APP_FRONTEND_URL";
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:3000";
    private static final int DEFAULT_BACKEND_PORT = 8080;
    private static final String API_BASE_URL_KEY = "API_BASE_URL";
    private static final String PUBLIC_API_BASE_URL_KEY = "NEXT_PUBLIC_API_BASE_URL";
    private static final String START_LOCK_FILE = ".frontend-dev-autostart.lock";
    private static final Duration START_LOCK_STALE_AFTER = Duration.ofMinutes(2);

    private DevFrontendBootstrap() {
    }

    static void ensureStartedForDevIfNeeded(String[] args) {
        if (!isWindows()) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        if (isProdProfileActive(args)) {
            return;
        }

        String frontendUrl = resolveFrontendUrl(args);
        if (!isLocalFrontendUrl(frontendUrl)) {
            return;
        }

        URI frontendUri = URI.create(frontendUrl);
        String host = frontendUri.getHost();
        int port = frontendUri.getPort() > 0 ? frontendUri.getPort() : 80;
        if (host == null || host.isBlank()) {
            LOGGER.warn("Frontend URL '{}' does not contain a valid host. Skipping frontend auto-start.", frontendUrl);
            return;
        }
        if (isPortAvailable(host, port)) {
            return;
        }

        Optional<Path> projectRoot = findProjectRoot();
        if (projectRoot.isEmpty()) {
            LOGGER.warn("Cannot find frontend-next directory from current directory. Skipping frontend auto-start.");
            return;
        }

        Path frontendDir = projectRoot.get().resolve("frontend-next");
        if (!Files.isDirectory(frontendDir)) {
            LOGGER.warn("Expected frontend directory '{}' was not found. Skipping frontend auto-start.", frontendDir);
            return;
        }

        try {
            ensureFrontendEnvLocal(frontendDir, resolveBackendBaseUrl(args));
        } catch (IOException ex) {
            LOGGER.warn("Unable to prepare frontend env file in '{}': {}", frontendDir, ex.getMessage());
        }

        if (!ensureFrontendDependencies(frontendDir)) {
            LOGGER.warn("Frontend dependencies are not ready. Skipping frontend auto-start.");
            return;
        }

        Path startLock = frontendDir.resolve(START_LOCK_FILE);
        boolean startTriggeredByThisProcess = tryAcquireStartLock(startLock);
        try {
            if (startTriggeredByThisProcess) {
                CommandResult startResult = startFrontendDevServer(frontendDir);
                if (startResult.exitCode() != 0) {
                    LOGGER.warn("Failed to start frontend dev server. Exit code: {}. Output: {}",
                            startResult.exitCode(), startResult.output());
                    return;
                }
            }

            if (!waitForPort(host, port, Duration.ofSeconds(90))) {
                LOGGER.warn("Frontend port {} did not become available in time. Check .data/logs/frontend-dev.log.",
                        port);
                return;
            }
        } finally {
            releaseStartLock(startLock, startTriggeredByThisProcess);
        }

        LOGGER.info("Frontend dev server is ready at {}", frontendUrl);
    }

    static boolean isLocalFrontendUrl(String frontendUrl) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return false;
        }
        try {
            URI frontendUri = URI.create(frontendUrl);
            String host = frontendUri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    static String buildFrontendEnvLocalContent(String backendBaseUrl) {
        return API_BASE_URL_KEY + "=" + backendBaseUrl + System.lineSeparator()
                + PUBLIC_API_BASE_URL_KEY + "=" + backendBaseUrl + System.lineSeparator();
    }

    static void ensureFrontendEnvLocal(Path frontendDir, String backendBaseUrl) throws IOException {
        Path envLocalPath = frontendDir.resolve(".env.local");
        Map<String, String> requiredEntries = new LinkedHashMap<>();
        requiredEntries.put(API_BASE_URL_KEY, backendBaseUrl);
        requiredEntries.put(PUBLIC_API_BASE_URL_KEY, backendBaseUrl);

        if (!Files.exists(envLocalPath)) {
            Files.writeString(envLocalPath, buildFrontendEnvLocalContent(backendBaseUrl), StandardCharsets.UTF_8);
            return;
        }

        String currentContent = Files.readString(envLocalPath, StandardCharsets.UTF_8);
        String updatedContent = currentContent;
        for (Map.Entry<String, String> entry : requiredEntries.entrySet()) {
            updatedContent = upsertEnvKey(updatedContent, entry.getKey(), entry.getValue());
        }
        if (!updatedContent.equals(currentContent)) {
            Files.writeString(envLocalPath, updatedContent, StandardCharsets.UTF_8);
        }
    }

    static String resolveBackendBaseUrl(String[] args) {
        int backendPort = resolveServerPort(args);
        return "http://localhost:" + backendPort;
    }

    private static String upsertEnvKey(String content, String key, String value) {
        Pattern pattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=.*$");
        Matcher matcher = pattern.matcher(content);
        String replacement = Matcher.quoteReplacement(key + "=" + value);
        if (matcher.find()) {
            return matcher.replaceAll(replacement);
        }
        StringBuilder builder = new StringBuilder(content);
        if (!content.isEmpty() && !content.endsWith("\n") && !content.endsWith("\r\n")) {
            builder.append(System.lineSeparator());
        }
        builder.append(key)
                .append("=")
                .append(value)
                .append(System.lineSeparator());
        return builder.toString();
    }

    private static boolean ensureFrontendDependencies(Path frontendDir) {
        Path nodeModulesDir = frontendDir.resolve("node_modules");
        if (Files.isDirectory(nodeModulesDir)) {
            return true;
        }
        LOGGER.info("frontend-next/node_modules is missing. Running npm.cmd install...");
        CommandResult installResult = runCommand(frontendDir, List.of("npm.cmd", "install"));
        if (installResult.exitCode() != 0) {
            LOGGER.warn("npm.cmd install failed in '{}'. Exit code: {}. Output: {}",
                    frontendDir, installResult.exitCode(), installResult.output());
            return false;
        }
        return Files.isDirectory(nodeModulesDir);
    }

    private static boolean isEnabled() {
        String fromProperty = System.getProperty(AUTO_START_FRONTEND_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Boolean.parseBoolean(fromProperty);
        }
        String fromEnv = System.getenv(AUTO_START_FRONTEND_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Boolean.parseBoolean(fromEnv);
        }
        return true;
    }

    private static String resolveFrontendUrl(String[] args) {
        return firstNonBlank(
                extractArgValue(args, FRONTEND_URL_PROPERTY),
                System.getProperty(FRONTEND_URL_PROPERTY),
                System.getenv(FRONTEND_URL_ENV),
                DEFAULT_FRONTEND_URL);
    }

    private static Optional<Path> findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("frontend-next"))) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private static CommandResult startFrontendDevServer(Path frontendDir) {
        Path projectRoot = frontendDir.getParent() != null ? frontendDir.getParent() : frontendDir;
        Path logPath;
        try {
            Path logDir = projectRoot.resolve(".data").resolve("logs");
            Files.createDirectories(logDir);
            logPath = logDir.resolve("frontend-dev.log").toAbsolutePath();
        } catch (IOException ex) {
            return new CommandResult(-1, "Cannot create .data/logs directory: " + ex.getMessage());
        }
        Path scriptPath = frontendDir.getParent() != null
                ? frontendDir.getParent().resolve("scripts").resolve("start-frontend-dev.ps1").toAbsolutePath()
                : null;
        if (scriptPath == null || !Files.isRegularFile(scriptPath)) {
            return new CommandResult(-1, "Cannot find scripts/start-frontend-dev.ps1.");
        }
        String script = toPowerShellSingleQuoted(scriptPath.toString());
        String workingDirectory = toPowerShellSingleQuoted(frontendDir.toString());
        String logFile = toPowerShellSingleQuoted(logPath.toString());
        String command = "Start-Process -FilePath 'powershell' -WindowStyle Hidden "
                + "-ArgumentList @("
                + "'-NoProfile',"
                + "'-ExecutionPolicy','Bypass',"
                + "'-File','" + script + "',"
                + "'-FrontendDir','" + workingDirectory + "',"
                + "'-LogPath','" + logFile + "'"
                + ") | Out-Null";
        return runCommand(null, List.of(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                command));
    }

    private static boolean tryAcquireStartLock(Path startLock) {
        try {
            Files.createFile(startLock);
            return true;
        } catch (FileAlreadyExistsException ex) {
            if (isStartLockStale(startLock)) {
                try {
                    Files.deleteIfExists(startLock);
                    Files.createFile(startLock);
                    return true;
                } catch (IOException retryEx) {
                    return false;
                }
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean isStartLockStale(Path startLock) {
        try {
            Instant lastModified = Files.getLastModifiedTime(startLock).toInstant();
            return lastModified.isBefore(Instant.now().minus(START_LOCK_STALE_AFTER));
        } catch (IOException ex) {
            return true;
        }
    }

    private static void releaseStartLock(Path startLock, boolean ownedByCurrentProcess) {
        if (!ownedByCurrentProcess) {
            return;
        }
        try {
            Files.deleteIfExists(startLock);
        } catch (IOException ignored) {
            // The next startup attempt will treat an old lock file as stale.
        }
    }

    private static boolean waitForPort(String host, int port, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isPortAvailable(host, port)) {
                return true;
            }
            sleep(Duration.ofSeconds(1));
        }
        return false;
    }

    private static boolean isPortAvailable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean isProdProfileActive(String[] args) {
        String fromArgs = extractArgValue(args, "spring.profiles.active");
        if (Port8080Guard.isProdProfileValue(fromArgs)) {
            return true;
        }
        String fromSysProp = System.getProperty("spring.profiles.active");
        if (Port8080Guard.isProdProfileValue(fromSysProp)) {
            return true;
        }
        String fromEnv = System.getenv("SPRING_PROFILES_ACTIVE");
        return Port8080Guard.isProdProfileValue(fromEnv);
    }

    private static int resolveServerPort(String[] args) {
        Integer port = parsePort(extractArgValue(args, "server.port"));
        if (port != null) {
            return port;
        }
        port = parsePort(System.getProperty("server.port"));
        if (port != null) {
            return port;
        }
        port = parsePort(System.getenv("SERVER_PORT"));
        return port != null ? port : DEFAULT_BACKEND_PORT;
    }

    private static Integer parsePort(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static CommandResult runCommand(Path workingDirectory, List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, output);
        } catch (IOException ex) {
            return new CommandResult(-1, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "Interrupted while executing command.");
        }
    }

    private static String extractArgValue(String[] args, String key) {
        if (args == null || args.length == 0) {
            return null;
        }
        String prefix = "--" + key + "=";
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String toPowerShellSingleQuoted(String value) {
        return value.replace("'", "''");
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private record CommandResult(int exitCode, String output) {
    }
}
