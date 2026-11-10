# 🦾 J.A.R.V.I.S. // Stark Industries File Handling System

An advanced Iron Man-themed voice & text AI assistant engineered for comprehensive file handling, management, and code generation.

---

## ⚡ Features Overview

### 1. 🎙️ Voice & Text Command Interface
- **Iron Man Holographic HUD**: Animated Arc Reactor that pulses and rotates depending on state (*Idle*, *Listening*, *Computing*, *Vocalizing*).
- **Dual Input Modes**: Use voice recognition via the glowing Arc Reactor microphone, or type commands into the Transmit bar.
- **Natural Voice Synthesis**: J.A.R.V.I.S. speaks back to you with Tony Stark's AI assistant persona.
- **Audio Feedback**: Built-in Sci-Fi audio synthesizer using the Web Audio API (zero external audio files needed).

### 2. 🗄️ Stark File Vault (Full File Handling)
- **Create Files**: Creates text, code, or markdown files with specified content.
- **Read & Inspect Files**: Opens files in the conversation feed and in an interactive live holographic editor modal.
- **Live File Editor**: Click **"View"** on any file to edit its contents directly in the browser and save changes back to disk.
- **Append to Files**: Adds lines or logs to existing files cleanly.
- **Search Files**: Real-time scan and wildcard search for files in the storage vault.
- **File Metadata & Stats**: Inspect file sizes, line counts, created timestamps, and storage metrics.
- **Directory Management**: Create custom folders and subfolders.
- **Auto-Organize**: Automatically organizes loose files into categorized folders (`Documents/`, `Code/`, `Images/`, `Data/`, `Archives/`).
- **Purge / Delete**: Safely eliminate files and directories.

### 3. 🤖 Hybrid AI Intelligence
- **Native File Operations**: File directives run with 0-latency offline using Java NIO.
- **OpenAI Integration**: Configured for `gpt-4o-mini` with `/chat/completions`. If online with an API key, Jarvis can generate code or documents on the fly and save them directly into files.
- **Offline Fallback**: If offline or without an API key, Jarvis continues to work seamlessly with built-in templates and in-character responses.

---

## 🗣️ Supported Directives (Voice & Text)

| Action | Voice / Text Directive Example |
| :--- | :--- |
| **Create File** | *"Jarvis, create file notes.txt with content Meeting with Pepper at 3 PM"* |
| **Create Empty / Code** | *"Jarvis, create file script.py"* |
| **AI Generate & Save** | *"Jarvis, write a python script for fibonacci and save it as fib.py"* |
| **Read / Open File** | *"Jarvis, read file notes.txt"* or *"Jarvis, show contents of script.py"* |
| **Append to File** | *"Jarvis, append 'Prepare Mark 85 armor' to notes.txt"* |
| **List Files** | *"Jarvis, list files"* or *"Jarvis, show files in vault"* |
| **Search Files** | *"Jarvis, search for notes"* or *"Jarvis, find *.java"* |
| **File Details / Info** | *"Jarvis, file info notes.txt"* or *"Jarvis, size of script.py"* |
| **Create Folder** | *"Jarvis, create folder Protocols"* |
| **Organize Workspace** | *"Jarvis, organize workspace"* |
| **Purge / Delete File** | *"Jarvis, purge file temp.txt"* |
| **Diagnostic Report** | *"Jarvis, system status"* or *"Jarvis, storage stats"* |
| **Help** | *"Jarvis, what can you do?"* or *"Jarvis, help"* |

---

## 🚀 How to Run

### Step 1: Start the Backend (Spring Boot)
Open a terminal in `Backend/jarvis-backend` and run:
```bash
./mvnw spring-boot:run
```
*(Or in VS Code / Eclipse: Open `JarvisBackendApplication.java` and click **Run**).*
The server will start on port `8080`.

### Step 2: Open the Frontend
Simply open `Frontend/index.html` in your browser (e.g. Google Chrome or Microsoft Edge for best Web Speech API support), or serve it with VS Code Live Server.

---

## 📁 Project Architecture
```
Jarvis/
├── Backend/
│   └── jarvis-backend/
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/jarvis/jarvis_backend/
│           │   │   ├── JarvisBackendApplication.java
│           │   │   ├── JarvisController.java     # REST API & File Endpoints
│           │   │   ├── JarvisService.java        # NLP Intent Parser & AI Handler
│           │   │   ├── FileService.java          # Java NIO File System Operations
│           │   │   ├── FileInfo.java             # File metadata model
│           │   │   ├── WorkspaceStats.java       # Storage diagnostics model
│           │   │   ├── JarvisResponse.java       # Unified response DTO
│           │   │   ├── JarvisRequest.java        # Inbound chat request
│           │   │   └── SaveFileRequest.java      # File save DTO
│           │   └── resources/
│           │       └── application.properties    # Configuration
│           └── test/
│               └── java/com/jarvis/jarvis_backend/
│                   ├── JarvisBackendApplicationTests.java
│                   └── JarvisFileHandlingTests.java  # 4 Comprehensive Unit Tests
└── Frontend/
    ├── index.html        # Iron Man HUD Holographic Interface
    ├── style.css         # Arc Reactor animations & Stark theme styling
    └── script.js         # Web Speech, Audio Synthesis & File Vault Client
```
