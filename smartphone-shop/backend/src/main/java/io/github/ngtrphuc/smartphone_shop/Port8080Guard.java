package io.github.ngtrphuc.smartphone_shop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Port8080Guard {

    private static final Logger LOGGER = LoggerFactory.getLogger(Port8080Guard.class);
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final String AUTO_FREE_FLAG_PROPERTY = "smartphone.shop.dev.auto-free-8080";
    private static final String AUTO_FREE_FLAG_ENV = "SMARTPHONE_SHOP_DEV_AUTO_FREE_8080";

    private Port8080Guard() {
    }

    static void releaseForDevIfNeeded(String[] args) {
        if (!isWindows()) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        if (isProdProfileActive(args)) {
            return;
        }
        if (resolveServerPort(args) != DEFAULT_HTTP_PORT) {
            return;
        }

        List<Long> pidsOnPort = findListeningPids(DEFAULT_HTTP_PORT);
        if (pidsOnPort.isEmpty()) {
            return;
        }

        long currentPid = ProcessHandle.current().pid();
        for (Long pid : pidsOnPort) {
            if (pid == null || pid <= 0 || pid == currentPid) {
                continue;
            }
            if (!isSafeToStop(pid)) {
                LOGGER.warn("Port {} is in use by PID {} (not recognized as this app). Skipping auto-stop.",
                        DEFAULT_HTTP_PORT, pid);
                continue;
            }
            if (stopProcess(pid)) {
                LOGGER.info("Freed port {} by stopping PID {}.", DEFAULT_HTTP_PORT, pid);
            } else {
                LOGGER.warn("Failed to stop PID {} on port {}. Startup may still fail.", pid, DEFAULT_HTTP_PORT);
            }
        }
    }

    static Set<Long> parseListeningPids(String netstatOutput, int port) {
        if (netstatOutput == null || netstatOutput.isBlank()) {
            return Set.of();
        }
        Set<Long> pids = new LinkedHashSet<>();
        String expectedPort = ":" + port;
        for (String line : netstatOutput.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("TCP")) {
                continue;
            }
            String[] columns = trimmed.split("\\s+");
            if (columns.length < 5) {
                continue;
            }
            String localAddress = columns[1];
            String state = columns[3];
            if (!"LISTENING".equalsIgnoreCase(state)) {
                continue;
            }
            if (!localAddress.endsWith(expectedPort)) {
                continue;
            }
            try {
                pids.add(Long.parseLong(columns[4]));
            } catch (NumberFormatException ignored) {
                // Ignore malformed rows and continue parsing the remaining output.
            }
        }
        return pids;
    }

    static boolean isProdProfileValue(String profiles) {
        if (profiles == null || profiles.isBlank()) {
            return false;
        }
        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);
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

    private static boolean isEnabled() {
        String fromProperty = System.getProperty(AUTO_FREE_FLAG_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Boolean.parseBoolean(fromProperty);
        }
        String fromEnv = System.getenv(AUTO_FREE_FLAG_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Boolean.parseBoolean(fromEnv);
        }
        return true;
    }

    private static int resolveServerPort(String[] args) {
        String fromArgs = extractArgValue(args, "server.port");
        Integer port = parsePort(fromArgs);
        if (port != null) {
            return port;
        }
        port = parsePort(System.getProperty("server.port"));
        if (port != null) {
            return port;
        }
        port = parsePort(System.getenv("SERVER_PORT"));
        return port != null ? port : DEFAULT_HTTP_PORT;
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

    private static Integer parsePort(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<Long> findListeningPids(int port) {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "netstat -ano -p TCP");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return new ArrayList<>(parseListeningPids(output, port));
        } catch (IOException e) {
            LOGGER.warn("Unable to inspect port {} usage with netstat: {}", port, e.getMessage());
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while checking port {} usage.", port);
            return List.of();
        }
    }

    private static boolean isSafeToStop(long pid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return false;
        }
        ProcessHandle.Info info = handle.get().info();
        String command = info.command().orElse("").toLowerCase(Locale.ROOT);
        String commandLine = info.commandLine().orElse("").toLowerCase(Locale.ROOT);
        return command.endsWith("java.exe")
                || command.endsWith("\\java")
                || commandLine.contains("io.github.ngtrphuc.smartphone_shop.smartphoneshopapplication")
                || commandLine.contains("smartphone-shop")
                || commandLine.contains("smartphone_shop");
    }

    private static boolean stopProcess(long pid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return false;
        }
        ProcessHandle processHandle = handle.get();
        if (!processHandle.destroy()) {
            processHandle.destroyForcibly();
        }
        try {
            processHandle.onExit().get(Duration.ofSeconds(3).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // Fall through and check final process state below.
        }
        return !processHandle.isAlive();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }
}

