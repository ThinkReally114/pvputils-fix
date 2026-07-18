package com.pvp_utils_skija_patch;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.tukaani.xz.XZInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

final class NeteaseMusicPlatformFix {
    private static final Logger LOGGER = LoggerFactory.getLogger("pvputils-skija-patch");
    private static final Object LOCK = new Object();
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3000;
    private static final String RESOURCE_ROOT = "/assets/pvp_utils/music-service/";
    private static final String API_ARCHIVE = RESOURCE_ROOT + "netease-cloud-music-api.zip";
    private static final String SERVICE_VERSION = "netease-4.32.0-node-22.13.1-v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static Process process;

    private NeteaseMusicPlatformFix() {
    }

    static void start() {
        if (!isLinuxOrMac()) {
            return;
        }
        synchronized (LOCK) {
            if ((process != null && process.isAlive()) || isServiceAvailable()) {
                return;
            }
            try {
                Installation installation = prepareInstallation();
                ProcessBuilder builder = new ProcessBuilder(
                        installation.nodeExecutable().toString(), installation.appScript().toString());
                builder.directory(installation.workingDirectory().toFile());
                builder.environment().put("HOST", HOST);
                builder.environment().put("PORT", Integer.toString(PORT));
                process = builder.start();
                drain(process.getInputStream(), "stdout");
                drain(process.getErrorStream(), "stderr");
                watch(process);
                LOGGER.info("网易云音乐本地服务已启动 / Netease Music local service started for {}",
                        platformName());
            } catch (IOException exception) {
                process = null;
                LOGGER.error("网易云音乐 Linux/macOS 服务启动失败 / Failed to start Netease Music service", exception);
            }
        }
    }

    static void stop() {
        Process current;
        synchronized (LOCK) {
            current = process;
            process = null;
        }
        if (current == null || !current.isAlive()) {
            return;
        }
        current.destroy();
        try {
            if (!current.waitFor(5, TimeUnit.SECONDS)) {
                current.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            current.destroyForcibly();
        }
    }

    private static boolean isLinuxOrMac() {
        String os = osName();
        return os.contains("linux") || os.contains("mac") || os.contains("darwin");
    }

    private static String osName() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    }

    private static String platformName() {
        return osName().contains("mac") || osName().contains("darwin") ? "macOS" : "Linux";
    }

    private static boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + HOST + ":" + PORT + "/"))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "PVPUtils-fix/1.0")
                    .GET()
                    .build();
            int status = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status >= 200 && status < 500;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Installation prepareInstallation() throws IOException {
        Path installDirectory = FabricLoader.getInstance().getGameDir()
                .resolve("PVPUtils").resolve("music-service").resolve(SERVICE_VERSION);
        Path marker = installDirectory.resolve(".version");
        Path apiDirectory = installDirectory.resolve("api/node_modules/NeteaseCloudMusicApi");
        Path appScript = apiDirectory.resolve("app.js");
        Path nodeDirectory = installDirectory.resolve("node");
        Path nodeExecutable = nodeDirectory.resolve("bin/node");

        if (Files.isRegularFile(marker)
                && SERVICE_VERSION.equals(Files.readString(marker, StandardCharsets.UTF_8))
                && Files.isRegularFile(appScript)
                && Files.isRegularFile(nodeExecutable)) {
            return new Installation(nodeExecutable, appScript, apiDirectory);
        }

        deleteRecursively(installDirectory);
        Files.createDirectories(installDirectory);
        extractZip(RESOURCE_ROOT + "netease-cloud-music-api.zip", installDirectory, false);
        extractTarXz(RESOURCE_ROOT + nodeArchiveName(), nodeDirectory);
        Files.writeString(marker, SERVICE_VERSION, StandardCharsets.UTF_8);
        nodeExecutable.toFile().setExecutable(true, false);
        return new Installation(nodeExecutable, appScript, apiDirectory);
    }

    private static String nodeArchiveName() throws IOException {
        String os = osName();
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm64 = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("mac") || os.contains("darwin")) {
            return arm64 ? "node-macos-aarch64.tar.xz" : "node-macos-x64.tar.xz";
        }
        if (os.contains("linux") && !arm64) {
            return "node-linux-x64.tar.xz";
        }
        throw new IOException("Unsupported Netease Music platform: " + os + " " + arch);
    }

    private static void extractZip(String resource, Path target, boolean stripFirstDirectory) throws IOException {
        try (ZipInputStream input = new ZipInputStream(resourceStream(resource))) {
            var entry = input.getNextEntry();
            while (entry != null) {
                Path relative = relativePath(entry.getName(), stripFirstDirectory);
                if (relative != null) {
                    Path output = safeOutputPath(target, relative);
                    if (entry.isDirectory()) {
                        Files.createDirectories(output);
                    } else {
                        Files.createDirectories(output.getParent());
                        Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                input.closeEntry();
                entry = input.getNextEntry();
            }
        }
    }

    private static void extractTarXz(String resource, Path target) throws IOException {
        try (XZInputStream xz = new XZInputStream(resourceStream(resource));
             TarArchiveInputStream input = new TarArchiveInputStream(xz)) {
            TarArchiveEntry entry;
            while ((entry = input.getNextTarEntry()) != null) {
                Path relative = relativePath(entry.getName(), true);
                if (relative == null) {
                    continue;
                }
                Path output = safeOutputPath(target, relative);
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path relativePath(String name, boolean stripFirstDirectory) {
        Path path = Path.of(name.replace('\\', '/')).normalize();
        if (path.isAbsolute() || path.startsWith("..")) {
            return null;
        }
        return stripFirstDirectory && path.getNameCount() > 1
                ? path.subpath(1, path.getNameCount()) : path;
    }

    private static Path safeOutputPath(Path target, Path relative) throws IOException {
        Path output = target.resolve(relative).normalize();
        if (!output.toAbsolutePath().normalize().startsWith(target.toAbsolutePath().normalize())) {
            throw new IOException("Archive entry escapes target directory: " + relative);
        }
        return output;
    }

    private static InputStream resourceStream(String resource) throws IOException {
        InputStream stream = NeteaseMusicPlatformFix.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("Missing bundled resource: " + resource);
        }
        return stream;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static void drain(InputStream stream, String name) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // Keep the child process pipes drained without flooding the game log.
                }
            } catch (IOException ignored) {
            }
        }, "PVPUtils-NeteaseMusic-" + name);
        thread.setDaemon(true);
        thread.start();
    }

    private static void watch(Process startedProcess) {
        Thread thread = new Thread(() -> {
            try {
                int exitCode = startedProcess.waitFor();
                synchronized (LOCK) {
                    if (process == startedProcess) {
                        process = null;
                    }
                }
                if (exitCode != 0) {
                    LOGGER.warn("Netease Music service exited with code {}", exitCode);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, "PVPUtils-NeteaseMusic-Watch");
        thread.setDaemon(true);
        thread.start();
    }

    private record Installation(Path nodeExecutable, Path appScript, Path workingDirectory) {
    }
}
