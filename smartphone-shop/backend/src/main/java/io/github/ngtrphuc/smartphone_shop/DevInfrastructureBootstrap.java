package io.github.ngtrphuc.smartphone_shop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DevInfrastructureBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevInfrastructureBootstrap.class);
    private static final String AUTO_START_INFRA_PROPERTY = "smartphone.shop.dev.auto-start-infra";
    private static final String AUTO_START_INFRA_ENV = "SMARTPHONE_SHOP_DEV_AUTO_START_INFRA";
    private static final int POSTGRES_PORT = 5432;

    private DevInfrastructureBootstrap() {
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
        if (!usesLocalPostgres(args)) {
            return;
        }

        Optional<Path> projectRoot = findProjectRoot();
        if (projectRoot.isEmpty()) {
            LOGGER.warn("Cannot find docker-compose.yml from current directory. Skipping auto-start infra.");
            return;
        }

        if (!isDockerReady()) {
            startDockerDesktopIfPossible();
            if (!waitForDockerReady(Duration.ofMinutes(2))) {
                LOGGER.warn("Docker is not ready after waiting. App startup may fail if PostgreSQL is unavailable.");
                return;
            }
        }

        CommandResult composeUpResult = runCommand(projectRoot.get(),
                List.of("docker", "compose", "up", "-d", "postgres", "redis"));
        if (composeUpResult.exitCode() != 0) {
            LOGGER.warn("Failed to run docker compose up. Exit code: {}. Output: {}",
                    composeUpResult.exitCode(), composeUpResult.output());
            return;
        }

        if (!waitForPort("127.0.0.1", POSTGRES_PORT, Duration.ofSeconds(90))) {
            LOGGER.warn("PostgreSQL port {} is still unavailable. App startup may fail.", POSTGRES_PORT);
            return;
        }
        LOGGER.info("Dev infrastructure is ready (Docker + PostgreSQL + Redis).");
    }

    private static boolean isEnabled() {
        String fromProperty = System.getProperty(AUTO_START_INFRA_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Boolean.parseBoolean(fromProperty);
        }
        String fromEnv = System.getenv(AUTO_START_INFRA_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Boolean.parseBoolean(fromEnv);
        }
        return true;
    }

    private static boolean isProdProfileActive(String[] args) {
        String fromArgs = extractArgValue(args, "spring.profiles.active");
        if (isProdProfileValue(fromArgs)) {
            return true;
        }
        String fromSysProp = System.getProperty("spring.profiles.active");
        if (isProdProfileValue(fromSysProp)) {
            return true;
        }
        String fromEnv = System.getenv("SPRING_PROFILES_ACTIVE");
        return isProdProfileValue(fromEnv);
    }

    private static boolean isProdProfileValue(String profiles) {
        if (profiles == null || profiles.isBlank()) {
            return false;
        }
        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);
    }

    private static boolean usesLocalPostgres(String[] args) {
        String fromArgs = extractArgValue(args, "spring.datasource.url");
        String fromSysProp = System.getProperty("spring.datasource.url");
        String fromEnv = System.getenv("DATASOURCE_URL");
        String url = firstNonBlank(fromArgs, fromSysProp, fromEnv,
                "jdbc:postgresql://localhost:5432/smartphone_shop");
        String normalized = url.toLowerCase(Locale.ROOT);
        return normalized.startsWith("jdbc:postgresql://localhost:5432/")
                || normalized.startsWith("jdbc:postgresql://127.0.0.1:5432/");
    }

    private static Optional<Path> findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (current != null) {
            Path composePath = current.resolve("docker-compose.yml");
            if (Files.exists(composePath)) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private static boolean isDockerReady() {
        CommandResult result = runCommand(null, List.of("docker", "info"));
        return result.exitCode() == 0;
    }

    private static void startDockerDesktopIfPossible() {
        List<Path> candidates = List.of(
                Paths.get("C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe"),
                Paths.get(System.getenv("LOCALAPPDATA") == null
                        ? "C:\\Users\\Default\\AppData\\Local\\Docker\\Docker\\Docker Desktop.exe"
                        : System.getenv("LOCALAPPDATA") + "\\Docker\\Docker\\Docker Desktop.exe"));

        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                new ProcessBuilder(candidate.toString()).start();
                LOGGER.info("Starting Docker Desktop: {}", candidate);
                return;
            } catch (IOException ignored) {
                // Try next known path.
            }
        }
        LOGGER.warn("Docker Desktop executable was not found. Start Docker Desktop manually if needed.");
    }

    private static boolean waitForDockerReady(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isDockerReady()) {
                return true;
            }
            sleep(Duration.ofSeconds(2));
        }
        return false;
    }

    private static boolean waitForPort(String host, int port, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 1500);
                return true;
            } catch (IOException ignored) {
                sleep(Duration.ofSeconds(1));
            }
        }
        return false;
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
        } catch (IOException e) {
            return new CommandResult(-1, e.getMessage());
        } catch (InterruptedException e) {
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

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
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
