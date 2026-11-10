package com.jarvis.jarvis_backend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class FileService {

    @Value("${jarvis.workspace.path:./jarvis-vault}")
    private String configuredWorkspacePath;

    private Path rootWorkspace;
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @PostConstruct
    public void init() {
        try {
            this.rootWorkspace = Paths.get(configuredWorkspacePath).toAbsolutePath().normalize();
            if (!Files.exists(rootWorkspace)) {
                Files.createDirectories(rootWorkspace);
                System.out.println("JARVIS Storage Vault initialized at: " + rootWorkspace);
                
                // Create an introductory welcome file
                Path welcomeFile = rootWorkspace.resolve("JARVIS_SYSTEM_LOG.md");
                if (!Files.exists(welcomeFile)) {
                    String intro = "# STARK INDUSTRIES - J.A.R.V.I.S. PROTOCOL INITIALIZED\n\n"
                            + "**Status:** Online & Ready\n"
                            + "**Storage Vault:** " + rootWorkspace + "\n\n"
                            + "### Supported File Handling Directives:\n"
                            + "- **Create File**: \"Jarvis, create file notes.txt with content Meeting with Pepper at 3 PM\"\n"
                            + "- **Read File**: \"Jarvis, read file notes.txt\"\n"
                            + "- **List Files**: \"Jarvis, list files in workspace\"\n"
                            + "- **Append File**: \"Jarvis, append 'Prepare Mark 85 armor' to notes.txt\"\n"
                            + "- **Search Files**: \"Jarvis, search for *.txt\"\n"
                            + "- **File Info**: \"Jarvis, file details for notes.txt\"\n"
                            + "- **Organize**: \"Jarvis, organize workspace\"\n"
                            + "- **Delete**: \"Jarvis, purge file temp.txt\"\n\n"
                            + "*Awaiting your command, sir.*";
                    Files.writeString(welcomeFile, intro, StandardCharsets.UTF_8);
                }
            } else {
                System.out.println("JARVIS Storage Vault active at: " + rootWorkspace);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize JARVIS workspace: " + e.getMessage());
            this.rootWorkspace = Paths.get(".").toAbsolutePath().normalize();
        }
    }

    public Path getRootWorkspace() {
        return rootWorkspace;
    }

    private Path resolveSafePath(String inputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            return rootWorkspace;
        }

        String cleaned = inputPath.trim();
        // Remove quotes if present
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        Path path = Paths.get(cleaned);
        if (path.isAbsolute()) {
            return path.normalize();
        }

        return rootWorkspace.resolve(cleaned).normalize();
    }

    public FileInfo createFile(String fileName, String content) throws IOException {
        Path target = resolveSafePath(fileName);

        if (target.getParent() != null && !Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }

        byte[] bytes = (content != null) ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return buildFileInfo(target);
    }

    public String readFile(String fileName) throws IOException {
        Path target = resolveSafePath(fileName);

        if (!Files.exists(target)) {
            throw new IOException("File not found: " + target.getFileName());
        }

        if (Files.isDirectory(target)) {
            throw new IOException("Target is a directory, not a file: " + target.getFileName());
        }

        long size = Files.size(target);
        if (size > 5 * 1024 * 1024) { // 5MB limit
            throw new IOException("File exceeds 5MB preview threshold. Total size: " + formatFileSize(size));
        }

        return Files.readString(target, StandardCharsets.UTF_8);
    }

    public FileInfo appendToFile(String fileName, String content) throws IOException {
        Path target = resolveSafePath(fileName);

        if (target.getParent() != null && !Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }

        StringBuilder textToAppend = new StringBuilder();
        if (Files.exists(target) && Files.size(target) > 0) {
            byte[] existingBytes = Files.readAllBytes(target);
            if (existingBytes.length > 0 && existingBytes[existingBytes.length - 1] != '\n') {
                textToAppend.append(System.lineSeparator());
            }
        }
        textToAppend.append(content != null ? content : "").append(System.lineSeparator());

        Files.writeString(target, textToAppend.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        return buildFileInfo(target);
    }

    public boolean deleteFile(String fileName) throws IOException {
        Path target = resolveSafePath(fileName);

        if (!Files.exists(target)) {
            throw new IOException("File or directory not found: " + target.getFileName());
        }

        if (Files.isDirectory(target)) {
            try (Stream<Path> walk = Files.walk(target)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            return !Files.exists(target);
        } else {
            return Files.deleteIfExists(target);
        }
    }

    public FileInfo createDirectory(String dirName) throws IOException {
        Path target = resolveSafePath(dirName);
        Files.createDirectories(target);
        return buildFileInfo(target);
    }

    public List<FileInfo> listFiles(String subDir) throws IOException {
        Path targetDir = (subDir == null || subDir.trim().isEmpty()) 
                ? rootWorkspace 
                : resolveSafePath(subDir);

        if (!Files.exists(targetDir)) {
            throw new IOException("Directory not found: " + targetDir.getFileName());
        }

        List<FileInfo> list = new ArrayList<>();
        try (Stream<Path> stream = Files.list(targetDir)) {
            list = stream
                    .map(this::buildFileInfoQuietly)
                    .filter(f -> f != null)
                    .sorted((a, b) -> {
                        if (a.isDirectory() != b.isDirectory()) {
                            return a.isDirectory() ? -1 : 1;
                        }
                        return a.getName().compareToIgnoreCase(b.getName());
                    })
                    .collect(Collectors.toList());
        }
        return list;
    }

    public List<FileInfo> searchFiles(String query) throws IOException {
        if (query == null || query.trim().isEmpty()) {
            return listFiles(null);
        }

        final String cleanQuery = query.toLowerCase().replace("*", "").trim();
        List<FileInfo> matches = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(rootWorkspace, 5)) {
            matches = walk
                    .filter(p -> !p.equals(rootWorkspace))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(cleanQuery))
                    .map(this::buildFileInfoQuietly)
                    .filter(f -> f != null)
                    .limit(50)
                    .collect(Collectors.toList());
        }

        return matches;
    }

    public FileInfo getFileInfo(String fileName) throws IOException {
        Path target = resolveSafePath(fileName);
        if (!Files.exists(target)) {
            throw new IOException("File not found: " + fileName);
        }
        return buildFileInfo(target);
    }

    public FileInfo renameOrMove(String src, String dest) throws IOException {
        Path sourcePath = resolveSafePath(src);
        Path destPath = resolveSafePath(dest);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Source path not found: " + src);
        }

        if (destPath.getParent() != null && !Files.exists(destPath.getParent())) {
            Files.createDirectories(destPath.getParent());
        }

        Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        return buildFileInfo(destPath);
    }

    public FileInfo copyFile(String src, String dest) throws IOException {
        Path sourcePath = resolveSafePath(src);
        Path destPath = resolveSafePath(dest);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Source file not found: " + src);
        }

        if (destPath.getParent() != null && !Files.exists(destPath.getParent())) {
            Files.createDirectories(destPath.getParent());
        }

        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        return buildFileInfo(destPath);
    }

    public Map<String, Integer> organizeWorkspace() throws IOException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("Documents", 0);
        stats.put("Code", 0);
        stats.put("Images", 0);
        stats.put("Data", 0);
        stats.put("Archives", 0);
        stats.put("Misc", 0);

        try (Stream<Path> stream = Files.list(rootWorkspace)) {
            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());

            for (Path file : files) {
                String name = file.getFileName().toString();
                String ext = getFileExtension(name).toLowerCase();
                String targetFolder = "Misc";

                if (List.of("txt", "md", "pdf", "doc", "docx", "rtf", "log").contains(ext)) {
                    targetFolder = "Documents";
                } else if (List.of("java", "py", "js", "ts", "html", "css", "json", "xml", "c", "cpp", "sh", "bat").contains(ext)) {
                    targetFolder = "Code";
                } else if (List.of("png", "jpg", "jpeg", "gif", "svg", "bmp", "webp").contains(ext)) {
                    targetFolder = "Images";
                } else if (List.of("csv", "sql", "xlsx", "xls").contains(ext)) {
                    targetFolder = "Data";
                } else if (List.of("zip", "rar", "7z", "tar", "gz").contains(ext)) {
                    targetFolder = "Archives";
                }

                Path folderPath = rootWorkspace.resolve(targetFolder);
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath);
                }

                Path targetFile = folderPath.resolve(name);
                Files.move(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                stats.put(targetFolder, stats.getOrDefault(targetFolder, 0) + 1);
            }
        }

        return stats;
    }

    public WorkspaceStats getWorkspaceStats() {
        int totalFiles = 0;
        int totalDirs = 0;
        long totalBytes = 0;

        try (Stream<Path> walk = Files.walk(rootWorkspace)) {
            List<Path> all = walk.filter(p -> !p.equals(rootWorkspace)).collect(Collectors.toList());
            for (Path p : all) {
                if (Files.isDirectory(p)) {
                    totalDirs++;
                } else if (Files.isRegularFile(p)) {
                    totalFiles++;
                    try {
                        totalBytes += Files.size(p);
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException ignored) {}

        File rootFile = rootWorkspace.toFile();
        long freeSpace = rootFile.getFreeSpace();

        return new WorkspaceStats(
                totalFiles,
                totalDirs,
                totalBytes,
                formatFileSize(totalBytes),
                rootWorkspace.toString(),
                formatFileSize(freeSpace)
        );
    }

    private FileInfo buildFileInfo(Path path) {
        String name = path.getFileName().toString();
        boolean isDir = Files.isDirectory(path);
        long size = 0;
        int lineCount = 0;

        if (!isDir && Files.isRegularFile(path)) {
            try {
                size = Files.size(path);
                if (size < 2 * 1024 * 1024) { // Only count lines for files < 2MB
                    try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                        lineCount = (int) lines.count();
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
        }

        String lastModified = "";
        try {
            Instant instant = Files.getLastModifiedTime(path).toInstant();
            lastModified = DATE_FORMATTER.format(instant);
        } catch (IOException ignored) {}

        String relativePath = rootWorkspace.relativize(path).toString();
        String extension = isDir ? "folder" : getFileExtension(name);

        return new FileInfo(
                name,
                path.toString(),
                relativePath,
                size,
                isDir ? "-" : formatFileSize(size),
                isDir,
                lastModified,
                extension,
                lineCount
        );
    }

    private FileInfo buildFileInfoQuietly(Path path) {
        try {
            return buildFileInfo(path);
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}
