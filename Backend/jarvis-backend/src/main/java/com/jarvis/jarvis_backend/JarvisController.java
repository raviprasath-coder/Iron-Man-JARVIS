package com.jarvis.jarvis_backend;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class JarvisController {

    private final JarvisService jarvisService;
    private final FileService fileService;

    public JarvisController(JarvisService jarvisService, FileService fileService) {
        this.jarvisService = jarvisService;
        this.fileService = fileService;
    }

    @PostMapping("/api/jarvis")
    public JarvisResponse jarvis(@RequestBody JarvisRequest request) {
        System.out.println("[JARVIS INBOUND] Directive: " + request.getMessage());
        return jarvisService.processCommand(request.getMessage());
    }

    @GetMapping("/api/files/list")
    public ResponseEntity<?> listFiles(@RequestParam(required = false) String subDir) {
        try {
            List<FileInfo> files = fileService.listFiles(subDir);
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/files/content")
    public ResponseEntity<?> getFileContent(@RequestParam String fileName) {
        try {
            String content = fileService.readFile(fileName);
            FileInfo info = fileService.getFileInfo(fileName);
            return ResponseEntity.ok(Map.of(
                    "info", info,
                    "content", content
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/files/save")
    public ResponseEntity<?> saveFile(@RequestBody SaveFileRequest request) {
        try {
            FileInfo info = fileService.createFile(request.getFileName(), request.getContent());
            return ResponseEntity.ok(info);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/files/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String fileName) {
        try {
            boolean deleted = fileService.deleteFile(fileName);
            return ResponseEntity.ok(Map.of("success", deleted, "fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/files/stats")
    public ResponseEntity<WorkspaceStats> getStats() {
        return ResponseEntity.ok(fileService.getWorkspaceStats());
    }

    @PostMapping("/api/files/organize")
    public ResponseEntity<?> organizeFiles() {
        try {
            Map<String, Integer> stats = fileService.organizeWorkspace();
            return ResponseEntity.ok(stats);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
