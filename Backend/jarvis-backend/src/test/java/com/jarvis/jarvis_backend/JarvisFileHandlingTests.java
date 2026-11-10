package com.jarvis.jarvis_backend;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

public class JarvisFileHandlingTests {

    private FileService fileService;
    private CodeGenerationService codeGenService;
    private JarvisService jarvisService;

    @TempDir
    Path tempVault;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "configuredWorkspacePath", tempVault.toString());
        fileService.init();

        codeGenService = new CodeGenerationService();
        jarvisService = new JarvisService(fileService, codeGenService);
        ReflectionTestUtils.setField(jarvisService, "apiKey", "");
    }

    @Test
    void testDirectFileOperations() throws IOException {
        // 1. Create file
        FileInfo created = fileService.createFile("ironman.txt", "Arc reactor at 100%");
        assertNotNull(created);
        assertEquals("ironman.txt", created.getName());
        assertEquals("ironman.txt", created.getRelativePath());
        assertTrue(created.getSize() > 0);

        // 2. Read file
        String content = fileService.readFile("ironman.txt");
        assertEquals("Arc reactor at 100%", content);

        // 3. Append to file
        FileInfo appended = fileService.appendToFile("ironman.txt", "Suit integrity optimal");
        assertNotNull(appended);
        String updated = fileService.readFile("ironman.txt");
        assertTrue(updated.contains("Arc reactor at 100%"));
        assertTrue(updated.contains("Suit integrity optimal"));

        // 4. File Info
        FileInfo info = fileService.getFileInfo("ironman.txt");
        assertEquals("txt", info.getExtension());
        assertFalse(info.isDirectory());
        assertEquals(2, info.getLineCount());

        // 5. List Files
        List<FileInfo> list = fileService.listFiles(null);
        assertTrue(list.stream().anyMatch(f -> f.getName().equals("ironman.txt")));

        // 6. Search Files
        List<FileInfo> searchResults = fileService.searchFiles("iron");
        assertEquals(1, searchResults.size());
        assertEquals("ironman.txt", searchResults.get(0).getName());

        // 7. Delete File
        boolean deleted = fileService.deleteFile("ironman.txt");
        assertTrue(deleted);
        assertFalse(Files.exists(tempVault.resolve("ironman.txt")));
    }

    @Test
    void testNaturalLanguageFileCommands() {
        // Create file via voice/text directive
        JarvisResponse r1 = jarvisService.processCommand("Jarvis, create file protocol.txt with content Mark 85 initialized");
        assertTrue(r1.isSuccess());
        assertEquals("FILE_CREATED", r1.getActionType());
        assertTrue(r1.getReply().contains("protocol.txt"));

        // Read file via voice/text directive
        JarvisResponse r2 = jarvisService.processCommand("Jarvis, read file protocol.txt");
        assertTrue(r2.isSuccess());
        assertEquals("FILE_READ", r2.getActionType());
        assertTrue(r2.getReply().contains("Mark 85 initialized"));

        // Append to file
        JarvisResponse r3 = jarvisService.processCommand("Jarvis, append Weapons online to file protocol.txt");
        assertTrue(r3.isSuccess());
        assertEquals("FILE_APPENDED", r3.getActionType());

        // List files
        JarvisResponse r4 = jarvisService.processCommand("list files");
        assertTrue(r4.isSuccess());
        assertEquals("FILE_LIST", r4.getActionType());
        assertTrue(r4.getReply().contains("protocol.txt"));

        // Search files
        JarvisResponse r5 = jarvisService.processCommand("search for protocol");
        assertTrue(r5.isSuccess());
        assertEquals("FILE_SEARCH", r5.getActionType());
        assertTrue(r5.getReply().contains("protocol.txt"));

        // System status
        JarvisResponse r6 = jarvisService.processCommand("system status");
        assertTrue(r6.isSuccess());
        assertEquals("SYSTEM_STATUS", r6.getActionType());
        assertTrue(r6.getReply().contains("JARVIS Diagnostic Report"));

        // Delete file
        JarvisResponse r7 = jarvisService.processCommand("Jarvis, purge file protocol.txt");
        assertTrue(r7.isSuccess());
        assertEquals("FILE_DELETED", r7.getActionType());
        assertTrue(r7.getReply().contains("Purge complete"));
    }

    @Test
    void testCodeGenerationDirectives() throws IOException {
        // 1. Calculator
        JarvisResponse r1 = jarvisService.processCommand("Jarvis, write code for calculator");
        assertTrue(r1.isSuccess());
        assertEquals("FILE_CREATED", r1.getActionType());
        assertTrue(r1.getReply().contains("calculator.py"));
        String calcCode = fileService.readFile("calculator.py");
        assertTrue(calcCode.contains("def add"));
        assertTrue(calcCode.contains("def divide"));

        // 2. Snake game in python
        JarvisResponse r2 = jarvisService.processCommand("create python code for snake game");
        assertTrue(r2.isSuccess());
        assertEquals("FILE_CREATED", r2.getActionType());
        assertTrue(r2.getReply().contains("snake"));
        String snakeCode = fileService.readFile("snake_game.py");
        assertTrue(snakeCode.contains("snake"));

        // 3. Fibonacci in python
        JarvisResponse r3 = jarvisService.processCommand("write code for fibonacci in python");
        assertTrue(r3.isSuccess());
        assertEquals("FILE_CREATED", r3.getActionType());
        assertTrue(fileService.readFile("fibonacci.py").contains("fib_iterative"));

        // 4. Portfolio in HTML
        JarvisResponse r4 = jarvisService.processCommand("generate html for portfolio");
        assertTrue(r4.isSuccess());
        assertEquals("FILE_CREATED", r4.getActionType());
        assertTrue(fileService.readFile("portfolio.html").contains("TONY STARK"));

        // 5. Primes in Java
        JarvisResponse r5 = jarvisService.processCommand("write a java program for primes");
        assertTrue(r5.isSuccess());
        assertEquals("FILE_CREATED", r5.getActionType());
        assertTrue(fileService.readFile("Primes.java").contains("isPrime"));
    }

    @Test
    void testOrganizeWorkspace() throws IOException {
        fileService.createFile("report.docx", "Document text");
        fileService.createFile("script.py", "print('hello')");
        fileService.createFile("image.png", "fake image");

        JarvisResponse response = jarvisService.processCommand("organize workspace");
        assertTrue(response.isSuccess());
        assertEquals("FILE_ORGANIZED", response.getActionType());

        assertTrue(Files.exists(tempVault.resolve("Documents").resolve("report.docx")));
        assertTrue(Files.exists(tempVault.resolve("Code").resolve("script.py")));
        assertTrue(Files.exists(tempVault.resolve("Images").resolve("image.png")));
    }
}
