package com.jarvis.jarvis_backend;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JarvisService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String aiModel;

    private final FileService fileService;
    private final CodeGenerationService codeGenerationService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // Regex patterns for file commands
    private static final Pattern PATTERN_CREATE_WITH_CONTENT = Pattern.compile(
            "(?:create|make|write|generate|new)\\s+(?:a\\s+)?(?:file|document)\\s+(?:named\\s+|called\\s+)?([\\w\\-\\.\\/\\\\]+)\\s+(?:with\\s+(?:content|text|code)|containing)\\s+(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern PATTERN_CREATE_EMPTY = Pattern.compile(
            "(?:create|make|new)\\s+(?:a\\s+)?(?:file|document)\\s+(?:named\\s+|called\\s+)?([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_READ_FILE = Pattern.compile(
            "(?:read|open|show|display|view|check|get content(?:s)? of|cat)\\s+(?:file\\s+)?([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_APPEND_FILE = Pattern.compile(
            "(?:append|add|write)\\s+[\"']?(.*?)[\"']?\\s+(?:to|into)\\s+(?:file\\s+)?([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_DELETE_FILE = Pattern.compile(
            "(?:delete|remove|purge|erase|destroy)\\s+(?:file|document|folder|directory)?\\s*([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SEARCH_FILE = Pattern.compile(
            "(?:search|find|lookup)\\s+(?:for\\s+)?(?:files?\\s+)?(?:named\\s+|matching\\s+|with\\s+)?([\\w\\-\\.\\*]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FILE_INFO = Pattern.compile(
            "(?:info|details|metadata|properties|size|stats)\\s+(?:of|for|about)?\\s*(?:file\\s+)?([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_CREATE_DIR = Pattern.compile(
            "(?:create|make|new)\\s+(?:a\\s+)?(?:folder|directory|dir)\\s+(?:named\\s+|called\\s+)?([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_RENAME_MOVE = Pattern.compile(
            "(?:rename|move)\\s+(?:file\\s+)?([\\w\\-\\.\\/\\\\]+)\\s+(?:to|as)\\s+([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_COPY_FILE = Pattern.compile(
            "(?:copy|duplicate)\\s+(?:file\\s+)?([\\w\\-\\.\\/\\\\]+)\\s+(?:to|as)\\s+([\\w\\-\\.\\/\\\\]+)",
            Pattern.CASE_INSENSITIVE);

    public JarvisService(FileService fileService, CodeGenerationService codeGenerationService) {
        this.fileService = fileService;
        this.codeGenerationService = codeGenerationService;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
    }

    public String processMessage(String message) {
        JarvisResponse response = processCommand(message);
        return response.getReply();
    }

    public JarvisResponse processCommand(String message) {
        if (message == null || message.trim().isEmpty()) {
            return JarvisResponse.error("Sir, I didn't catch that. Please state your command.");
        }

        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();
        System.out.println("[JARVIS HUD] Direct command received: " + trimmed);

        try {
            // 1. System Status / Who are you / Help
            if (lower.contains("who are you") || lower.contains("what can you do") || lower.equals("help") || lower.contains("commands")) {
                return handleHelpAndStatus();
            }

            if (lower.equals("system status") || lower.equals("status report") || lower.contains("storage stats") || lower.equals("status")) {
                return handleSystemStatus();
            }

            // 2. Clear / Reset
            if (lower.equals("clear") || lower.equals("cls")) {
                return JarvisResponse.success("Display cleared, sir. Standing by.", "CLEAR", null);
            }

            // 3. Organize Workspace
            if (lower.contains("organize workspace") || lower.contains("organize files") || lower.contains("sort files") || lower.contains("clean up workspace")) {
                Map<String, Integer> orgStats = fileService.organizeWorkspace();
                String summary = "Workspace organized, sir. Classified files into: "
                        + "Documents (" + orgStats.get("Documents") + "), "
                        + "Code (" + orgStats.get("Code") + "), "
                        + "Images (" + orgStats.get("Images") + "), "
                        + "Data (" + orgStats.get("Data") + ").";
                return JarvisResponse.success(summary, "FILE_ORGANIZED", orgStats);
            }

            // 4. List Files
            if (lower.equals("list files") || lower.equals("show files") || lower.equals("ls") || lower.equals("dir") 
                    || lower.contains("list files in") || lower.contains("what files") || lower.contains("view workspace")
                    || lower.contains("browse files") || lower.equals("files")) {
                return handleListFiles(trimmed);
            }

            // 5. CODE GENERATION DIRECTIVES (e.g. "write code for calculator", "create python script for snake game", etc.)
            if (codeGenerationService.isCodeRequest(trimmed)) {
                CodeGenerationService.CodeRequest codeReq = codeGenerationService.parseRequest(trimmed);
                return handleCodeGeneration(codeReq);
            }

            // 6. Create File with Content
            Matcher createWithContentMatcher = PATTERN_CREATE_WITH_CONTENT.matcher(trimmed);
            if (createWithContentMatcher.find()) {
                String fileName = createWithContentMatcher.group(1).trim();
                String content = createWithContentMatcher.group(2).trim();
                FileInfo info = fileService.createFile(fileName, content);
                String reply = "Right away, sir. Created '" + info.getName() + "' (" + info.getFormattedSize() + ") in the vault.";
                return JarvisResponse.success(reply, "FILE_CREATED", info);
            }

            // 7. Create Empty File
            Matcher createEmptyMatcher = PATTERN_CREATE_EMPTY.matcher(trimmed);
            if (createEmptyMatcher.find()) {
                String fileName = createEmptyMatcher.group(1).trim();
                String defaultContent = generateStarterContent(fileName);
                FileInfo info = fileService.createFile(fileName, defaultContent);
                String reply = "File '" + info.getName() + "' initialized, sir. Awaiting your data.";
                return JarvisResponse.success(reply, "FILE_CREATED", info);
            }

            // 8. Read File
            Matcher readMatcher = PATTERN_READ_FILE.matcher(trimmed);
            if (readMatcher.find()) {
                String fileName = readMatcher.group(1).trim();
                String content = fileService.readFile(fileName);
                FileInfo info = fileService.getFileInfo(fileName);
                
                String reply = "Opening '" + info.getName() + "' (" + info.getLineCount() + " lines, " + info.getFormattedSize() + "). Here are the contents, sir:\n\n" + content;
                
                Map<String, Object> data = new HashMap<>();
                data.put("info", info);
                data.put("content", content);
                return JarvisResponse.success(reply, "FILE_READ", data);
            }

            // 9. Append to File
            Matcher appendMatcher = PATTERN_APPEND_FILE.matcher(trimmed);
            if (appendMatcher.find()) {
                String contentToAppend = appendMatcher.group(1).trim();
                String fileName = appendMatcher.group(2).trim();
                FileInfo info = fileService.appendToFile(fileName, contentToAppend);
                String reply = "Updated '" + info.getName() + "'. New size is " + info.getFormattedSize() + ", sir.";
                return JarvisResponse.success(reply, "FILE_APPENDED", info);
            }

            // 10. Delete File
            Matcher deleteMatcher = PATTERN_DELETE_FILE.matcher(trimmed);
            if (deleteMatcher.find()) {
                String target = deleteMatcher.group(1).trim();
                boolean deleted = fileService.deleteFile(target);
                if (deleted) {
                    return JarvisResponse.success("Purge complete, sir. '" + target + "' has been eliminated from storage.", "FILE_DELETED", target);
                } else {
                    return JarvisResponse.error("Unable to purge '" + target + "'. File was not found.");
                }
            }

            // 11. Create Directory
            Matcher dirMatcher = PATTERN_CREATE_DIR.matcher(trimmed);
            if (dirMatcher.find()) {
                String dirName = dirMatcher.group(1).trim();
                FileInfo info = fileService.createDirectory(dirName);
                return JarvisResponse.success("Directory '" + info.getName() + "' established, sir.", "DIR_CREATED", info);
            }

            // 12. Search Files
            Matcher searchMatcher = PATTERN_SEARCH_FILE.matcher(trimmed);
            if (searchMatcher.find()) {
                String query = searchMatcher.group(1).trim();
                List<FileInfo> matches = fileService.searchFiles(query);
                if (matches.isEmpty()) {
                    return JarvisResponse.success("Sensor scan complete, sir. No files matching '" + query + "' were detected in the vault.", "FILE_SEARCH", matches);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Located ").append(matches.size()).append(" matching item(s) for '").append(query).append("', sir:\n");
                    for (FileInfo f : matches) {
                        sb.append("- ").append(f.getRelativePath()).append(" (").append(f.getFormattedSize()).append(")\n");
                    }
                    return JarvisResponse.success(sb.toString().trim(), "FILE_SEARCH", matches);
                }
            }

            // 13. File Details / Info
            Matcher infoMatcher = PATTERN_FILE_INFO.matcher(trimmed);
            if (infoMatcher.find()) {
                String fileName = infoMatcher.group(1).trim();
                FileInfo info = fileService.getFileInfo(fileName);
                String reply = "Diagnostics for '" + info.getName() + "':\n"
                        + "Type: " + (info.isDirectory() ? "Directory" : info.getExtension().toUpperCase() + " File") + "\n"
                        + "Size: " + info.getFormattedSize() + "\n"
                        + "Lines: " + info.getLineCount() + "\n"
                        + "Last Modified: " + info.getLastModified() + "\n"
                        + "Path: " + info.getPath();
                return JarvisResponse.success(reply, "FILE_INFO", info);
            }

            // 14. Rename / Move File
            Matcher moveMatcher = PATTERN_RENAME_MOVE.matcher(trimmed);
            if (moveMatcher.find()) {
                String src = moveMatcher.group(1).trim();
                String dst = moveMatcher.group(2).trim();
                FileInfo info = fileService.renameOrMove(src, dst);
                return JarvisResponse.success("Relocated '" + src + "' to '" + info.getRelativePath() + "', sir.", "FILE_MOVED", info);
            }

            // 15. Copy File
            Matcher copyMatcher = PATTERN_COPY_FILE.matcher(trimmed);
            if (copyMatcher.find()) {
                String src = copyMatcher.group(1).trim();
                String dst = copyMatcher.group(2).trim();
                FileInfo info = fileService.copyFile(src, dst);
                return JarvisResponse.success("Duplicated '" + src + "' as '" + info.getRelativePath() + "', sir.", "FILE_COPIED", info);
            }

            // 16. Fallback to OpenAI AI Assistant or Local Stark Persona
            return handleGeneralConversation(trimmed);

        } catch (IOException e) {
            System.err.println("JARVIS File Operation Error: " + e.getMessage());
            return JarvisResponse.error("Storage protocol alert, sir: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("JARVIS General Error: " + e.getMessage());
            e.printStackTrace();
            return JarvisResponse.error("An anomaly occurred while processing your request: " + e.getMessage());
        }
    }

    private JarvisResponse handleCodeGeneration(CodeGenerationService.CodeRequest req) {
        String code = null;
        String provider = "J.A.R.V.I.S. Stark Code Matrix";

        // Try OpenAI if API key is present
        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("PASTE_KEY_HERE")) {
            try {
                String prompt = "Write clean, fully functional, working " + req.language.toUpperCase() 
                        + " code for: " + req.topic 
                        + ". Output only the source code with comments, no conversational preamble or markdown backticks.";
                String aiCode = callOpenAiChat(prompt, "You are a master software architect for Stark Industries.");
                if (aiCode != null && !aiCode.isBlank()) {
                    code = cleanSourceCode(aiCode);
                    provider = "Stark Neural Link (OpenAI)";
                }
            } catch (Exception e) {
                System.out.println("[JARVIS] OpenAI error or quota reached (" + e.getMessage() + "). Seamlessly deploying Stark Built-in Code Matrix.");
            }
        }

        // If OpenAI was unavailable or quota exhausted, generate via Stark Code Matrix
        if (code == null || code.isBlank()) {
            code = codeGenerationService.generateOfflineCode(req.topic, req.language, req.targetFileName);
        }

        try {
            FileInfo info = fileService.createFile(req.targetFileName, code);
            String reply = "I have generated the " + req.language.toUpperCase() + " code for '" + req.topic 
                    + "' and stored it as '" + info.getName() + "' (" + info.getFormattedSize() + ", " + info.getLineCount() + " lines) in your vault, sir. Code preview is displayed below and ready in the editor.";

            Map<String, Object> data = new HashMap<>();
            data.put("info", info);
            data.put("content", code);
            data.put("topic", req.topic);
            data.put("language", req.language);
            data.put("provider", provider);

            return JarvisResponse.success(reply, "FILE_CREATED", data);
        } catch (IOException e) {
            return JarvisResponse.error("Failed to store generated code: " + e.getMessage());
        }
    }

    private String cleanSourceCode(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private JarvisResponse handleListFiles(String trimmed) throws IOException {
        String subDir = null;
        if (trimmed.toLowerCase().contains(" in ")) {
            int idx = trimmed.toLowerCase().indexOf(" in ");
            subDir = trimmed.substring(idx + 4).trim();
        }

        List<FileInfo> files = fileService.listFiles(subDir);
        if (files.isEmpty()) {
            return JarvisResponse.success("The vault directory is currently empty, sir. Ready for file creation.", "FILE_LIST", files);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Scanning vault, sir. I count ").append(files.size()).append(" item(s):\n");
        for (FileInfo f : files) {
            String icon = f.isDirectory() ? "📁 " : "📄 ";
            sb.append(icon).append(f.getName()).append(" (").append(f.getFormattedSize()).append(")\n");
        }

        return JarvisResponse.success(sb.toString().trim(), "FILE_LIST", files);
    }

    private JarvisResponse handleSystemStatus() {
        WorkspaceStats stats = fileService.getWorkspaceStats();
        String reply = "JARVIS Diagnostic Report:\n"
                + "• Protocol: Arc Reactor v4 Online\n"
                + "• Storage Vault: " + stats.getWorkspacePath() + "\n"
                + "• Total Files: " + stats.getTotalFiles() + "\n"
                + "• Total Folders: " + stats.getTotalDirectories() + "\n"
                + "• Vault Size: " + stats.getFormattedTotalSize() + "\n"
                + "• Available Storage: " + stats.getFreeDiskSpace() + "\n"
                + "All subsystems operating at peak efficiency, sir.";
        return JarvisResponse.success(reply, "SYSTEM_STATUS", stats);
    }

    private JarvisResponse handleHelpAndStatus() {
        String reply = "J.A.R.V.I.S. (Just A Rather Very Intelligent System) file handling protocol online, sir.\n\n"
                + "Available Directives:\n"
                + "• \"Write code for calculator\"\n"
                + "• \"Create python code for snake game\"\n"
                + "• \"Write code for fibonacci in python\"\n"
                + "• \"Generate html for portfolio\"\n"
                + "• \"Create file <name> with content <text>\"\n"
                + "• \"Read file <name>\"\n"
                + "• \"Append <text> to <name>\"\n"
                + "• \"List files\" or \"Show files\"\n"
                + "• \"Search for <keyword>\"\n"
                + "• \"File info <name>\"\n"
                + "• \"Organize workspace\"\n"
                + "• \"Delete file <name>\"\n"
                + "• \"System status\"\n\n"
                + "Standing by for your instructions.";
        return JarvisResponse.success(reply, "HELP", null);
    }

    private JarvisResponse handleGeneralConversation(String userMessage) {
        // Try OpenAI if key is present
        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("PASTE_KEY_HERE")) {
            try {
                String systemPrompt = "You are J.A.R.V.I.S., Tony Stark's sophisticated, loyal, and witty AI assistant. Address the user with 'sir' or 'boss'. Keep responses concise, articulate, and helpful.";
                String aiReply = callOpenAiChat(userMessage, systemPrompt);
                if (aiReply != null && !aiReply.isBlank()) {
                    return JarvisResponse.success(aiReply, "CHAT", null);
                }
            } catch (Exception e) {
                System.out.println("OpenAI chat unavailable (" + e.getMessage() + "), using Stark local persona.");
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    return JarvisResponse.success("Sir, my cloud neural link reports that your OpenAI account quota is currently exhausted (HTTP 429). However, my local Stark systems and Code Generator are fully functional. You can ask me to write code for a calculator, games, algorithms, or manage files in your vault.", "CHAT", null);
                }
            }
        }

        // Offline Jarvis Persona Responses
        String reply = getOfflineJarvisReply(userMessage);
        return JarvisResponse.success(reply, "CHAT", null);
    }

    private String callOpenAiChat(String userPrompt, String systemPrompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiModel);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sys = new HashMap<>();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        messages.add(sys);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.7);

        String responseJson = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }

        return null;
    }

    private String getOfflineJarvisReply(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return "Good day, sir. J.A.R.V.I.S. online and standing by. What file or code directive can I execute for you?";
        }
        if (lower.contains("thank")) {
            return "Always a pleasure, sir. Let me know if you need any other modifications.";
        }
        if (lower.contains("tony") || lower.contains("stark") || lower.contains("iron man")) {
            return "Mr. Stark is currently in the workshop fine-tuning the Mark 85 armor. I have full authorization to generate code and manage your files in the meantime, sir.";
        }
        return "Understood, sir. J.A.R.V.I.S. is at your disposal. You can ask me to write code for a calculator, games, algorithms, or manage files in your vault.";
    }

    private String generateStarterContent(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".py")) {
            return "#!/usr/bin/env python3\n# J.A.R.V.I.S. Auto-generated Python script\n\ndef main():\n    print(\"JARVIS protocol active.\")\n\nif __name__ == '__main__':\n    main()\n";
        } else if (lower.endsWith(".java")) {
            String className = fileName.replaceAll("\\.java$", "");
            return "public class " + className + " {\n    public static void main(String[] args) {\n        System.out.println(\"JARVIS Protocol Online\");\n    }\n}\n";
        } else if (lower.endsWith(".html")) {
            return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>Stark Industries</title>\n</head>\n<body>\n    <h1>JARVIS System Active</h1>\n</body>\n</html>\n";
        } else if (lower.endsWith(".json")) {
            return "{\n  \"system\": \"JARVIS\",\n  \"status\": \"online\",\n  \"version\": \"4.0\"\n}\n";
        } else if (lower.endsWith(".md")) {
            return "# " + fileName + "\n\nCreated by J.A.R.V.I.S. at your instruction, sir.\n";
        }
        return "Created by J.A.R.V.I.S.\n";
    }
}
