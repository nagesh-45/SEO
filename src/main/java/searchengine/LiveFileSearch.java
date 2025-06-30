package searchengine;

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
    
    public List<SearchResult> searchByName(String searchTerm, String rootPath, boolean useRegex) {
        return searchByName(searchTerm, rootPath, useRegex, false);
    }
    
    public List<SearchResult> searchByName(String searchTerm, String rootPath, boolean useRegex, boolean useFuzzy) {
        if (useFuzzy) {
            return searchByNameFuzzy(searchTerm, rootPath);
        }
        
        final List<SearchResult> results = new ArrayList<>();
        final Path root = Paths.get(rootPath);
        final String searchTermFinal = searchTerm;
        if (!Files.exists(root)) {
            return results;
        }
        final java.util.regex.Pattern pattern;
        if (useRegex) {
            try {
                pattern = java.util.regex.Pattern.compile(searchTerm, java.util.regex.Pattern.CASE_INSENSITIVE);
            } catch (java.util.regex.PatternSyntaxException e) {
                System.err.println("Invalid regex pattern: " + e.getMessage());
                return results;
            }
        } else {
            pattern = null;
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
                        String searchLower = searchTermFinal.toLowerCase();
                        
                        boolean matches = false;
                        if (useRegex && pattern != null) {
                            matches = pattern.matcher(fileName).find();
                        } else {
                            matches = fileNameLower.contains(searchLower);
                        }
                        
                        if (matches) {
                            try {
                                SearchResult result = new SearchResult(
                                    file.toString(),
                                    fileName, // Keep original case
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
            String searchLower = searchTermFinal.toLowerCase();
            
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
    
    // Overload for backward compatibility
    public List<SearchResult> searchByName(String searchTerm, String rootPath) {
        return searchByName(searchTerm, rootPath, false);
    }
    
    // New method for fuzzy search - splits terms and finds files containing all terms
    public List<SearchResult> searchByNameFuzzy(String searchTerm, String rootPath) {
        String[] terms = searchTerm.toLowerCase().split("\\s+");
        List<String> searchTerms = new ArrayList<>();
        for (String term : terms) {
            if (!term.trim().isEmpty()) {
                searchTerms.add(term.trim());
            }
        }
        
        if (searchTerms.isEmpty()) {
            return new ArrayList<>();
        }
        
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
                        
                        // Check if ALL search terms are present in the filename
                        boolean allTermsFound = true;
                        for (String term : searchTerms) {
                            if (!fileNameLower.contains(term)) {
                                allTermsFound = false;
                                break;
                            }
                        }
                        
                        if (allTermsFound) {
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
        
        results.sort((a, b) -> {
            String aName = a.getFileName().toLowerCase();
            String bName = b.getFileName().toLowerCase();
            
            int aExactMatches = 0, bExactMatches = 0;
            for (String term : searchTerms) {
                if (aName.equals(term)) aExactMatches++;
                if (bName.equals(term)) bExactMatches++;
            }
            
            if (aExactMatches != bExactMatches) {
                return Integer.compare(bExactMatches, aExactMatches);
            }
            
            int aStartsWith = 0, bStartsWith = 0;
            for (String term : searchTerms) {
                if (aName.startsWith(term)) aStartsWith++;
                if (bName.startsWith(term)) bStartsWith++;
            }
            
            if (aStartsWith != bStartsWith) {
                return Integer.compare(bStartsWith, aStartsWith);
            }
            
            return aName.compareTo(bName);
        });
        
        return results;
    }
    
    public List<SearchResult> searchByContent(String searchTerm, String rootPath, boolean useRegex) {
        return searchByContent(searchTerm, rootPath, useRegex, false);
    }
    
    public List<SearchResult> searchByContent(String searchTerm, String rootPath, boolean useRegex, boolean useFuzzy) {
        // For content search, fuzzy search doesn't make sense, so we ignore the useFuzzy parameter
        final List<SearchResult> results = new ArrayList<>();
        final Path root = Paths.get(rootPath);
        final String searchTermFinal = searchTerm;
        if (!Files.exists(root)) {
            return results;
        }
        final java.util.regex.Pattern pattern;
        if (useRegex) {
            try {
                pattern = java.util.regex.Pattern.compile(searchTerm, java.util.regex.Pattern.CASE_INSENSITIVE);
            } catch (java.util.regex.PatternSyntaxException e) {
                System.err.println("Invalid regex pattern: " + e.getMessage());
                return results;
            }
        } else {
            pattern = null;
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
                                boolean found = false;
                                if (useRegex && pattern != null) {
                                    found = searchInFileWithRegex(file, pattern);
                                } else {
                                    found = searchInFile(file, searchTermFinal);
                                }
                                
                                if (found) {
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
    
    // Overload for backward compatibility
    public List<SearchResult> searchByContent(String searchTerm, String rootPath) {
        return searchByContent(searchTerm, rootPath, false);
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
    
    private boolean searchInFileWithRegex(Path file, java.util.regex.Pattern pattern) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (pattern.matcher(line).find()) {
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
            return String.format("%-100s  %-8s  %-8s  %s", 
                fileName, // Show entire filename without truncation
                formatSize(size),
                searchType.toString(),
                new java.text.SimpleDateFormat("MMM dd HH:mm").format(new Date(lastModified)));
        }
        
        public String getDisplayName() {
            return fileName; // Just the file name for simple display
        }
        
        private String formatPath(String fullPath) {
            // Show the full directory path without truncation
            Path path = Paths.get(fullPath);
            Path parent = path.getParent();
            if (parent != null) {
                return parent.toString();
            }
            return ".";
        }
        
        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
} 