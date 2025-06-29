import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class LiveFileSearch {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> SKIP_DIRECTORIES = new HashSet<>(Arrays.asList(
        ".git", ".svn", ".hg", "node_modules", "target", "build", "bin", "obj",
        "Library", "System", "Applications", "private", "var", "tmp", "usr"
    ));
    
    private final ExecutorService executorService;
    
    public LiveFileSearch() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
    
    public List<SearchResult> searchByName(String searchTerm, String rootPath) {
        List<SearchResult> results = new ArrayList<>();
        Path root = Paths.get(rootPath);
        
        if (!Files.exists(root)) {
            return results;
        }
        
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName().toString();
                    if (SKIP_DIRECTORIES.contains(dirName) || dirName.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        String fileName = file.getFileName().toString();
                        String fileNameLower = fileName.toLowerCase();
                        String searchLower = searchTerm.toLowerCase();
                        
                        if (fileNameLower.contains(searchLower)) {
                            try {
                                SearchResult result = new SearchResult(
                                    file.toString(),
                                    fileName,
                                    attrs.size(),
                                    attrs.lastModifiedTime().toMillis(),
                                    SearchType.NAME
                                );
                                results.add(result);
                            } catch (Exception e) {
                                // Skip files with access issues
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error searching files: " + e.getMessage());
        }
        
        // Sort by relevance (exact matches first, then by name similarity)
        results.sort((a, b) -> {
            String aName = a.getFileName().toLowerCase();
            String bName = b.getFileName().toLowerCase();
            String searchLower = searchTerm.toLowerCase();
            
            boolean aExact = aName.equals(searchLower);
            boolean bExact = bName.equals(searchLower);
            
            if (aExact && !bExact) return -1;
            if (!aExact && bExact) return 1;
            
            boolean aStarts = aName.startsWith(searchLower);
            boolean bStarts = bName.startsWith(searchLower);
            
            if (aStarts && !bStarts) return -1;
            if (!aStarts && bStarts) return 1;
            
            return aName.compareTo(bName);
        });
        
        return results;
    }
    
    public List<SearchResult> searchByContent(String searchTerm, String rootPath) {
        List<SearchResult> results = new ArrayList<>();
        Path root = Paths.get(rootPath);
        
        if (!Files.exists(root)) {
            return results;
        }
        
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName().toString();
                    if (SKIP_DIRECTORIES.contains(dirName) || dirName.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && attrs.size() <= MAX_FILE_SIZE) {
                        String fileName = file.getFileName().toString();
                        String fileNameLower = fileName.toLowerCase();
                        
                        // Skip binary files and large files
                        if (isTextFile(fileNameLower)) {
                            try {
                                if (searchInFile(file, searchTerm)) {
                                    SearchResult result = new SearchResult(
                                        file.toString(),
                                        fileName, // Keep original case
                                        attrs.size(),
                                        attrs.lastModifiedTime().toMillis(),
                                        SearchType.CONTENT
                                    );
                                    results.add(result);
                                }
                            } catch (Exception e) {
                                // Skip files with access issues
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error searching files: " + e.getMessage());
        }
        
        // Sort by file size (smaller files first)
        results.sort(Comparator.comparingLong(SearchResult::getSize));
        
        return results;
    }
    
    private boolean isTextFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".txt") || lowerName.endsWith(".md") || 
               lowerName.endsWith(".java") || lowerName.endsWith(".py") || 
               lowerName.endsWith(".js") || lowerName.endsWith(".html") || 
               lowerName.endsWith(".css") || lowerName.endsWith(".xml") || 
               lowerName.endsWith(".json") || lowerName.endsWith(".csv") ||
               lowerName.endsWith(".log") || lowerName.endsWith(".properties") ||
               lowerName.endsWith(".yml") || lowerName.endsWith(".yaml") ||
               !lowerName.contains("."); // Files without extension
    }
    
    private boolean searchInFile(Path file, String searchTerm) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(searchTerm.toLowerCase())) {
                    return true;
                }
            }
        } catch (IOException e) {
            // File cannot be read, skip it
        }
        return false;
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
    
    public enum SearchType {
        NAME, CONTENT
    }
    
    public static class SearchResult {
        private final String filePath;
        private final String fileName;
        private final long size;
        private final long lastModified;
        private final SearchType searchType;
        
        public SearchResult(String filePath, String fileName, long size, long lastModified, SearchType searchType) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.size = size;
            this.lastModified = lastModified;
            this.searchType = searchType;
        }
        
        public String getFilePath() { return filePath; }
        public String getFileName() { return fileName; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
        public SearchType getSearchType() { return searchType; }
        
        @Override
        public String toString() {
            return String.format("%-50s  %-10s  %-12s  %s", 
                truncateFileName(fileName, 47),
                formatSize(size),
                searchType.toString(),
                new java.text.SimpleDateFormat("MMM dd HH:mm").format(new Date(lastModified)));
        }
        
        public String getDisplayName() {
            return fileName; // Just the file name for simple display
        }
        
        private String truncateFileName(String name, int maxLength) {
            if (name.length() <= maxLength) return name;
            return name.substring(0, maxLength - 3) + "...";
        }
        
        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
} 