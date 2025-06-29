import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileSearchEngine {
    private FileIndexer indexer;
    private boolean isIndexed = false;
    
    public FileSearchEngine(String rootPath) {
        this.indexer = new FileIndexer(rootPath);
        // Don't build index automatically - let user control when to build it
    }
    
    // Build the index when needed
    public void buildIndex() {
        if (!isIndexed) {
            indexer.buildIndex();
            isIndexed = true;
        }
    }
    
    // Check if index is built
    public boolean isIndexed() {
        return isIndexed;
    }
    
    // Search files by name (exact match)
    public List<FileInfo> searchByName(String fileName) {
        if (!isIndexed) {
            System.out.println("Index not built yet. Use 'build' command first.");
            return new ArrayList<>();
        }
        List<String> paths = indexer.getFileNameTrie().searchExact(fileName);
        return paths.stream()
                   .map(indexer.getFileInfoMap()::get)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }
    
    // Search files by name prefix
    public List<FileInfo> searchByNamePrefix(String prefix) {
        if (!isIndexed) {
            System.out.println("Index not built yet. Use 'build' command first.");
            return new ArrayList<>();
        }
        List<String> paths = indexer.getFileNameTrie().searchByPrefix(prefix);
        return paths.stream()
                   .map(indexer.getFileInfoMap()::get)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }
    
    // Search files by content
    public List<FileInfo> searchByContent(String searchTerm) {
        if (!isIndexed) {
            System.out.println("Index not built yet. Use 'build' command first.");
            return new ArrayList<>();
        }
        List<FileInfo> results = new ArrayList<>();
        String lowerSearchTerm = searchTerm.toLowerCase();
        
        for (Map.Entry<String, String> entry : indexer.getFilePathToContent().entrySet()) {
            String content = entry.getValue();
            if (content.toLowerCase().contains(lowerSearchTerm)) {
                FileInfo fileInfo = indexer.getFileInfoMap().get(entry.getKey());
                if (fileInfo != null) {
                    results.add(fileInfo);
                }
            }
        }
        
        return results;
    }
    
    // Search files by both name and content
    public List<FileInfo> searchByNameAndContent(String searchTerm) {
        if (!isIndexed) {
            System.out.println("Index not built yet. Use 'build' command first.");
            return new ArrayList<>();
        }
        Set<FileInfo> results = new HashSet<>();
        
        // Search by name
        results.addAll(searchByName(searchTerm));
        results.addAll(searchByNamePrefix(searchTerm));
        
        // Search by content
        results.addAll(searchByContent(searchTerm));
        
        return new ArrayList<>(results);
    }
    
    // Get file statistics
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        if (!isIndexed) {
            stats.put("totalFiles", 0);
            stats.put("textFiles", 0);
            stats.put("rootDirectory", indexer.getRootDirectory().toAbsolutePath().toString());
            stats.put("totalSize", 0L);
            stats.put("status", "Index not built yet");
        } else {
            stats.put("totalFiles", indexer.getFileInfoMap().size());
            stats.put("textFiles", indexer.getFilePathToContent().size());
            stats.put("rootDirectory", indexer.getRootDirectory().toAbsolutePath().toString());
            
            long totalSize = indexer.getFileInfoMap().values().stream()
                                   .mapToLong(FileInfo::getSize)
                                   .sum();
            stats.put("totalSize", totalSize);
            stats.put("status", "Index ready");
        }
        
        return stats;
    }
    
    // Refresh the index (rebuild it)
    public void refreshIndex() {
        System.out.println("Building fresh index for: " + indexer.getRootDirectory().toAbsolutePath());
        indexer.buildIndex();
        isIndexed = true;
    }
    
    public static void openFile(String filePath) {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
        } catch (Exception e) {
            System.err.println("Could not open file: " + filePath);
        }
    }
    
    // Inner class to store file information
    public static class FileInfo {
        private String fileName;
        private String absolutePath;
        private long size;
        private long lastModified;
        
        public FileInfo(String fileName, String absolutePath, long size, long lastModified) {
            this.fileName = fileName;
            this.absolutePath = absolutePath;
            this.size = size;
            this.lastModified = lastModified;
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public String getAbsolutePath() { return absolutePath; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
        
        @Override
        public String toString() {
            return String.format("File: %s | Path: %s | Size: %d bytes | Modified: %s", 
                               fileName, absolutePath, size, 
                               new java.util.Date(lastModified));
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FileInfo fileInfo = (FileInfo) obj;
            return Objects.equals(absolutePath, fileInfo.absolutePath);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(absolutePath);
        }
    }
} 