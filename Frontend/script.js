// ==========================================
// STARK INDUSTRIES // J.A.R.V.I.S. CLIENT ENGINE
// ==========================================

const API_BASE = "http://localhost:8080";

// DOM Elements
const micButton = document.getElementById("micButton");
const arcReactor = document.getElementById("arcReactor");
const statusText = document.getElementById("status");
const stateBadge = document.getElementById("stateBadge");
const conversation = document.getElementById("conversation");
const commandForm = document.getElementById("commandForm");
const textInput = document.getElementById("textInput");
const clearChatBtn = document.getElementById("clearChatBtn");
const hudClock = document.getElementById("hudClock");
const connectionStatus = document.getElementById("connectionStatus");
const voiceToggleBtn = document.getElementById("voiceToggleBtn");
const voiceToggleIcon = document.getElementById("voiceToggleIcon");

// Vault DOM Elements
const statFiles = document.getElementById("statFiles");
const statDirs = document.getElementById("statDirs");
const statSize = document.getElementById("statSize");
const vaultFileList = document.getElementById("vaultFileList");
const vaultSearchInput = document.getElementById("vaultSearchInput");
const refreshVaultBtn = document.getElementById("refreshVaultBtn");
const newFileBtn = document.getElementById("newFileBtn");
const newFolderBtn = document.getElementById("newFolderBtn");
const organizeBtn = document.getElementById("organizeBtn");

// Modal Elements
const fileModal = document.getElementById("fileModal");
const modalFileName = document.getElementById("modalFileName");
const modalFileMeta = document.getElementById("modalFileMeta");
const modalEditor = document.getElementById("modalEditor");
const modalSaveBtn = document.getElementById("modalSaveBtn");
const modalDownloadBtn = document.getElementById("modalDownloadBtn");
const modalCloseBtn = document.getElementById("modalCloseBtn");

// State
let isVoiceMuted = false;
let currentOpenFile = null;
let vaultFilesCache = [];

// ==========================================
// AUDIO SYNTHESIZER (Sci-Fi Sound Effects)
// ==========================================
class SoundFX {
    constructor() {
        this.ctx = null;
    }
    init() {
        if (!this.ctx) {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            if (AudioCtx) this.ctx = new AudioCtx();
        }
    }
    playTone(freq, type = 'sine', duration = 0.1, gain = 0.05) {
        try {
            this.init();
            if (!this.ctx) return;
            const osc = this.ctx.createOscillator();
            const g = this.ctx.createGain();
            osc.type = type;
            osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
            g.gain.setValueAtTime(gain, this.ctx.currentTime);
            g.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);
            osc.connect(g);
            g.connect(this.ctx.destination);
            osc.start();
            osc.stop(this.ctx.currentTime + duration);
        } catch (e) {}
    }
    chirpStart() {
        this.playTone(520, 'sine', 0.1, 0.08);
        setTimeout(() => this.playTone(780, 'triangle', 0.12, 0.08), 70);
    }
    chirpSuccess() {
        this.playTone(440, 'triangle', 0.08, 0.06);
        setTimeout(() => this.playTone(880, 'sine', 0.15, 0.08), 80);
    }
    chirpError() {
        this.playTone(300, 'sawtooth', 0.15, 0.08);
        setTimeout(() => this.playTone(200, 'sawtooth', 0.2, 0.08), 100);
    }
}
const sfx = new SoundFX();

// ==========================================
// CLOCK & HEALTH CHECK
// ==========================================
function updateClock() {
    const now = new Date();
    hudClock.textContent = now.toTimeString().split(' ')[0];
}
setInterval(updateClock, 1000);
updateClock();

async function checkBackendHealth() {
    try {
        const res = await fetch(`${API_BASE}/api/files/stats`);
        if (res.ok) {
            connectionStatus.textContent = "CORE ONLINE";
            connectionStatus.className = "value status-online";
        } else {
            throw new Error();
        }
    } catch (e) {
        connectionStatus.textContent = "CORE OFFLINE";
        connectionStatus.className = "value";
        connectionStatus.style.color = "#ff4444";
    }
}
setInterval(checkBackendHealth, 10000);
checkBackendHealth();

// ==========================================
// SPEECH RECOGNITION (Voice Control)
// ==========================================
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;

if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.lang = "en-US";
    recognition.continuous = false;
    recognition.interimResults = false;

    micButton.addEventListener("click", () => {
        sfx.chirpStart();
        setReactorState("listening");
        statusText.textContent = "Jarvis listening... speak directive";
        stateBadge.textContent = "AUDIO STREAM // LISTENING";
        try {
            recognition.start();
        } catch (e) {
            console.log("Recognition already running or re-triggered");
        }
    });

    recognition.onresult = (event) => {
        const text = event.results[0][0].transcript;
        console.log("Voice transcript:", text);
        submitDirective(text);
    };

    recognition.onerror = (event) => {
        console.warn("Speech recognition error:", event.error);
        setReactorState("idle");
        statusText.textContent = "Voice input error: " + event.error;
        stateBadge.textContent = "IDLE // STANDBY";
        sfx.chirpError();
    };

    recognition.onend = () => {
        if (!arcReactor.classList.contains("arc-speaking")) {
            setReactorState("idle");
            stateBadge.textContent = "IDLE // STANDBY";
        }
    };
} else {
    statusText.textContent = "Speech recognition unavailable in this browser. Use text input below.";
}

// ==========================================
// TEXT & DIRECTIVE SUBMISSION
// ==========================================
commandForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const text = textInput.value.trim();
    if (text) {
        textInput.value = "";
        submitDirective(text);
    }
});

// Quick action chips
document.querySelectorAll(".chip-btn[data-command]").forEach(btn => {
    btn.addEventListener("click", () => {
        const cmd = btn.getAttribute("data-command");
        submitDirective(cmd);
    });
});

clearChatBtn.addEventListener("click", () => {
    conversation.innerHTML = `
        <div class="message jarvis-msg">
            <div class="msg-header">
                <span class="msg-sender">J.A.R.V.I.S.</span>
                <span class="msg-time">${getTimeStamp()}</span>
            </div>
            <div class="msg-body">
                Display cleared, sir. Standing by for instructions.
            </div>
        </div>
    `;
    sfx.chirpSuccess();
});

async function submitDirective(message) {
    if (!message || !message.trim()) return;

    // Append user message to chat
    appendMessage("YOU", message, "user-msg");
    setReactorState("processing");
    statusText.textContent = "Processing directive...";
    stateBadge.textContent = "NEURAL CORE // COMPUTING";

    try {
        const response = await fetch(`${API_BASE}/api/jarvis`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: message })
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const data = await response.json();
        const reply = data.reply || "Directive executed, sir.";

        setReactorState("speaking");
        statusText.textContent = "J.A.R.V.I.S. responding";
        stateBadge.textContent = "AUDIO VOCALIZER // ACTIVE";

        // Append Jarvis message + rich card if applicable
        appendJarvisMessage(reply, data);

        // Speak reply via speech synthesis
        speak(reply, () => {
            setReactorState("idle");
            statusText.textContent = "Standing by for directive";
            stateBadge.textContent = "IDLE // STANDBY";
        });

        sfx.chirpSuccess();

        // Refresh File Vault
        loadVault();

    } catch (error) {
        console.error("Jarvis communication error:", error);
        setReactorState("idle");
        statusText.textContent = "Error: Unable to connect to backend core.";
        stateBadge.textContent = "SYSTEM ALERT // ERROR";
        sfx.chirpError();

        appendMessage("J.A.R.V.I.S.", "Sir, I encountered a communication barrier with the backend server. Please ensure the Spring Boot server is online at port 8080.", "jarvis-msg");
    }
}

// ==========================================
// FEED MESSAGES & RICH CARDS
// ==========================================
function appendMessage(sender, text, cssClass) {
    const msgDiv = document.createElement("div");
    msgDiv.className = `message ${cssClass}`;
    msgDiv.innerHTML = `
        <div class="msg-header">
            <span class="msg-sender">${sender}</span>
            <span class="msg-time">${getTimeStamp()}</span>
        </div>
        <div class="msg-body">${escapeHTML(text)}</div>
    `;
    conversation.appendChild(msgDiv);
    conversation.scrollTop = conversation.scrollHeight;
}

function appendJarvisMessage(reply, fullData) {
    const msgDiv = document.createElement("div");
    msgDiv.className = "message jarvis-msg";

    let richCardHtml = "";
    if (fullData && fullData.actionType) {
        switch (fullData.actionType) {
            case "FILE_CREATED":
                const file = fullData.data && fullData.data.info ? fullData.data.info : fullData.data;
                const fileContent = fullData.data && fullData.data.content ? fullData.data.content : null;
                const provider = fullData.data && fullData.data.provider ? fullData.data.provider : null;
                if (file && file.name) {
                    richCardHtml = `
                        <div class="feed-card">
                            <div class="card-info">
                                <span class="card-icon">${getFileIcon(file.name)}</span>
                                <div>
                                    <div class="card-title">${escapeHTML(file.name)}</div>
                                    <div class="card-meta">${file.formattedSize || ''} • ${file.lineCount || 0} lines${provider ? ' • ' + escapeHTML(provider) : ''}</div>
                                </div>
                            </div>
                            <div class="card-actions">
                                <button class="card-btn" onclick="openFileViewer('${escapeHTML(file.name)}')">VIEW IN EDITOR</button>
                                <button class="card-btn" onclick="downloadFile('${escapeHTML(file.name)}')">DOWNLOAD</button>
                            </div>
                        </div>
                    `;
                    if (fileContent) {
                        richCardHtml += `
                            <div class="code-preview-container">
                                <div class="code-preview-header">
                                    <span class="code-title">${escapeHTML(file.name)} (Source Preview)</span>
                                    <button class="code-copy-btn" onclick="copyCodeSnippet(this)">COPY CODE</button>
                                </div>
                                <pre class="code-box"><code>${escapeHTML(fileContent)}</code></pre>
                            </div>
                        `;
                    }
                }
                break;
            case "FILE_READ":
                if (fullData.data && fullData.data.info) {
                    const info = fullData.data.info;
                    richCardHtml = `
                        <div class="feed-card">
                            <div class="card-info">
                                <span class="card-icon">${getFileIcon(info.name)}</span>
                                <div>
                                    <div class="card-title">${escapeHTML(info.name)}</div>
                                    <div class="card-meta">${info.formattedSize} • ${info.lineCount} lines</div>
                                </div>
                            </div>
                            <button class="card-btn" onclick="openFileViewer('${escapeHTML(info.name)}')">OPEN EDITOR</button>
                        </div>
                    `;
                }
                break;
        }
    }

    msgDiv.innerHTML = `
        <div class="msg-header">
            <span class="msg-sender">J.A.R.V.I.S.</span>
            <span class="msg-time">${getTimeStamp()}</span>
        </div>
        <div class="msg-body">${escapeHTML(reply)}</div>
        ${richCardHtml}
    `;

    conversation.appendChild(msgDiv);
    conversation.scrollTop = conversation.scrollHeight;
}

// ==========================================
// SPEECH SYNTHESIS (Voice Output)
// ==========================================
voiceToggleBtn.addEventListener("click", () => {
    isVoiceMuted = !isVoiceMuted;
    voiceToggleIcon.textContent = isVoiceMuted ? "🔇" : "🔊";
    if (isVoiceMuted && window.speechSynthesis) {
        window.speechSynthesis.cancel();
    }
});

function speak(text, onComplete) {
    if (isVoiceMuted || !('speechSynthesis' in window)) {
        if (onComplete) onComplete();
        return;
    }

    window.speechSynthesis.cancel();

    // Clean up code or Markdown blocks before speaking so speech sounds natural
    let spokenText = text.replace(/```[\s\S]*?```/g, "Code block generated, sir.")
                         .replace(/[#*`_]/g, "")
                         .replace(/\n+/g, ". ");

    // Truncate spoken text if it's exceptionally long (e.g. large file listing)
    if (spokenText.length > 280) {
        spokenText = spokenText.substring(0, 280) + "... and several additional items, sir.";
    }

    const utterance = new SpeechSynthesisUtterance(spokenText);
    utterance.lang = "en-US";
    utterance.rate = 1.0;
    utterance.pitch = 0.85; // Slightly deeper tone for Jarvis feel

    // Pick best available English voice
    const voices = window.speechSynthesis.getVoices();
    const jarvisVoice = voices.find(v => v.name.includes("UK English") || v.name.includes("Daniel") || v.name.includes("Google UK English Male") || v.lang === "en-GB")
                     || voices.find(v => v.lang.startsWith("en"));
    if (jarvisVoice) {
        utterance.voice = jarvisVoice;
    }

    utterance.onend = () => {
        if (onComplete) onComplete();
    };

    utterance.onerror = () => {
        if (onComplete) onComplete();
    };

    window.speechSynthesis.speak(utterance);
}

// ==========================================
// FILE VAULT MANAGER
// ==========================================
async function loadVault() {
    try {
        // 1. Fetch Stats
        const statsRes = await fetch(`${API_BASE}/api/files/stats`);
        if (statsRes.ok) {
            const stats = await statsRes.json();
            statFiles.textContent = stats.totalFiles || 0;
            statDirs.textContent = stats.totalDirectories || 0;
            statSize.textContent = stats.formattedTotalSize || "0 B";
        }

        // 2. Fetch Files
        const filesRes = await fetch(`${API_BASE}/api/files/list`);
        if (filesRes.ok) {
            vaultFilesCache = await filesRes.json();
            renderVaultFiles(vaultFilesCache);
        }
    } catch (e) {
        console.error("Vault sync failed:", e);
        vaultFileList.innerHTML = `<div class="vault-empty">Vault connection offline. Start backend on port 8080.</div>`;
    }
}

function renderVaultFiles(files) {
    const filter = (vaultSearchInput.value || "").toLowerCase().trim();
    const filtered = files.filter(f => f.name.toLowerCase().includes(filter));

    if (filtered.length === 0) {
        vaultFileList.innerHTML = `<div class="vault-empty">${files.length === 0 ? "Storage vault empty. Direct Jarvis to create a file." : "No matching files."}</div>`;
        return;
    }

    vaultFileList.innerHTML = "";
    filtered.forEach(file => {
        const item = document.createElement("div");
        item.className = "vault-item";
        item.innerHTML = `
            <div class="item-left">
                <span class="item-icon">${getFileIcon(file.name, file.directory)}</span>
                <div class="item-details">
                    <span class="item-name" title="${escapeHTML(file.name)}">${escapeHTML(file.name)}</span>
                    <span class="item-meta">${file.directory ? "Directory" : file.formattedSize + " • " + (file.lastModified ? file.lastModified.split(" ")[0] : "")}</span>
                </div>
            </div>
            <div class="item-actions">
                ${file.directory ? '' : `<button class="item-btn" onclick="openFileViewer('${escapeHTML(file.name)}')">View</button>`}
                ${file.directory ? '' : `<button class="item-btn" onclick="downloadFile('${escapeHTML(file.name)}')">⬇</button>`}
                <button class="item-btn item-btn-del" onclick="deleteVaultFile('${escapeHTML(file.name)}')">✕</button>
            </div>
        `;
        vaultFileList.appendChild(item);
    });
}

vaultSearchInput.addEventListener("input", () => {
    renderVaultFiles(vaultFilesCache);
});

refreshVaultBtn.addEventListener("click", () => {
    sfx.chirpStart();
    loadVault();
});

// Quick vault actions
newFileBtn.addEventListener("click", () => {
    const fileName = prompt("Enter file name (e.g. notes.txt, script.py):");
    if (fileName && fileName.trim()) {
        submitDirective(`Jarvis, create file ${fileName.trim()}`);
    }
});

newFolderBtn.addEventListener("click", () => {
    const folderName = prompt("Enter folder name:");
    if (folderName && folderName.trim()) {
        submitDirective(`Jarvis, create folder ${folderName.trim()}`);
    }
});

organizeBtn.addEventListener("click", () => {
    submitDirective("Jarvis, organize workspace");
});

// Delete file
window.deleteVaultFile = async function(fileName) {
    if (confirm(`Sir, confirm purge protocol for: ${fileName}?`)) {
        try {
            const res = await fetch(`${API_BASE}/api/files/delete?fileName=${encodeURIComponent(fileName)}`, {
                method: "DELETE"
            });
            if (res.ok) {
                sfx.chirpSuccess();
                loadVault();
                appendMessage("J.A.R.V.I.S.", `Purge confirmed. '${fileName}' has been eliminated, sir.`, "jarvis-msg");
            } else {
                sfx.chirpError();
            }
        } catch (e) {
            console.error(e);
        }
    }
};

// ==========================================
// MODAL FILE VIEWER / EDITOR
// ==========================================
window.openFileViewer = async function(fileName) {
    try {
        const res = await fetch(`${API_BASE}/api/files/content?fileName=${encodeURIComponent(fileName)}`);
        if (!res.ok) throw new Error("Could not read file");

        const data = await res.json();
        currentOpenFile = fileName;
        modalFileName.textContent = fileName;
        modalFileMeta.textContent = `${data.info.formattedSize} • ${data.info.lineCount} lines`;
        modalEditor.value = data.content;

        fileModal.style.display = "flex";
        sfx.chirpStart();
    } catch (e) {
        alert("Error loading file: " + e.message);
    }
};

modalCloseBtn.addEventListener("click", () => {
    fileModal.style.display = "none";
    currentOpenFile = null;
});

modalSaveBtn.addEventListener("click", async () => {
    if (!currentOpenFile) return;
    try {
        const res = await fetch(`${API_BASE}/api/files/save`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                fileName: currentOpenFile,
                content: modalEditor.value
            })
        });

        if (res.ok) {
            sfx.chirpSuccess();
            alert(`Changes to '${currentOpenFile}' saved successfully, sir.`);
            loadVault();
        } else {
            throw new Error();
        }
    } catch (e) {
        sfx.chirpError();
        alert("Failed to save changes.");
    }
});

window.downloadFile = function(fileName) {
    fetch(`${API_BASE}/api/files/content?fileName=${encodeURIComponent(fileName)}`)
        .then(res => res.json())
        .then(data => {
            const blob = new Blob([data.content], { type: "text/plain;charset=utf-8" });
            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = fileName;
            link.click();
        })
        .catch(err => alert("Download failed: " + err));
};

modalDownloadBtn.addEventListener("click", () => {
    if (currentOpenFile) {
        downloadFile(currentOpenFile);
    }
});

// ==========================================
// HELPER UTILITIES
// ==========================================
function setReactorState(state) {
    arcReactor.classList.remove("arc-listening", "arc-speaking");
    if (state === "listening") {
        arcReactor.classList.add("arc-listening");
    } else if (state === "speaking") {
        arcReactor.classList.add("arc-speaking");
    }
}

function getFileIcon(fileName, isDir = false) {
    if (isDir) return "📁";
    const ext = fileName.split('.').pop().toLowerCase();
    switch (ext) {
        case "py": return "🐍";
        case "java": return "☕";
        case "js":
        case "ts": return "⚡";
        case "html":
        case "css": return "🌐";
        case "json":
        case "xml": return "⚙️";
        case "md": return "📝";
        case "txt":
        case "log": return "📄";
        case "csv":
        case "sql": return "📊";
        case "png":
        case "jpg":
        case "jpeg": return "🖼️";
        case "zip":
        case "tar": return "📦";
        default: return "📄";
    }
}

function getTimeStamp() {
    const d = new Date();
    return d.toTimeString().split(' ')[0];
}

function escapeHTML(str) {
    if (!str) return "";
    return str.replace(/[&<>'"]/g, 
        tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag)
    );
}

window.copyCodeSnippet = function(btn) {
    const code = btn.parentElement.nextElementSibling.querySelector('code').textContent;
    navigator.clipboard.writeText(code).then(() => {
        const orig = btn.textContent;
        btn.textContent = "COPIED!";
        sfx.chirpSuccess();
        setTimeout(() => { btn.textContent = orig; }, 1500);
    });
};

// Initial vault sync on load
window.addEventListener("DOMContentLoaded", () => {
    loadVault();
});
