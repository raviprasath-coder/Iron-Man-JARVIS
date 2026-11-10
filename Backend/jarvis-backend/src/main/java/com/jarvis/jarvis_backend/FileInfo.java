package com.jarvis.jarvis_backend;

public class FileInfo {
    private String name;
    private String path;
    private String relativePath;
    private long size;
    private String formattedSize;
    private boolean isDirectory;
    private String lastModified;
    private String extension;
    private int lineCount;

    public FileInfo() {}

    public FileInfo(String name, String path, String relativePath, long size, String formattedSize, 
                    boolean isDirectory, String lastModified, String extension, int lineCount) {
        this.name = name;
        this.path = path;
        this.relativePath = relativePath;
        this.size = size;
        this.formattedSize = formattedSize;
        this.isDirectory = isDirectory;
        this.lastModified = lastModified;
        this.extension = extension;
        this.lineCount = lineCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    public void setFormattedSize(String formattedSize) {
        this.formattedSize = formattedSize;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
}
