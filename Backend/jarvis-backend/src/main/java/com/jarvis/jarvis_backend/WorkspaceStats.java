package com.jarvis.jarvis_backend;

public class WorkspaceStats {
    private int totalFiles;
    private int totalDirectories;
    private long totalSizeBytes;
    private String formattedTotalSize;
    private String workspacePath;
    private String freeDiskSpace;

    public WorkspaceStats() {}

    public WorkspaceStats(int totalFiles, int totalDirectories, long totalSizeBytes, 
                          String formattedTotalSize, String workspacePath, String freeDiskSpace) {
        this.totalFiles = totalFiles;
        this.totalDirectories = totalDirectories;
        this.totalSizeBytes = totalSizeBytes;
        this.formattedTotalSize = formattedTotalSize;
        this.workspacePath = workspacePath;
        this.freeDiskSpace = freeDiskSpace;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalDirectories() {
        return totalDirectories;
    }

    public void setTotalDirectories(int totalDirectories) {
        this.totalDirectories = totalDirectories;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public String getFormattedTotalSize() {
        return formattedTotalSize;
    }

    public void setFormattedTotalSize(String formattedTotalSize) {
        this.formattedTotalSize = formattedTotalSize;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getFreeDiskSpace() {
        return freeDiskSpace;
    }

    public void setFreeDiskSpace(String freeDiskSpace) {
        this.freeDiskSpace = freeDiskSpace;
    }
}
