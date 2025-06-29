import java.util.*;

public class IndexerMain {
    // Default directory to index if no argument provided
    private static final String DEFAULT_DIRECTORY = System.getProperty("user.home");
    
    public static void main(String[] args) {
        String rootPath;
        
        if (args.length == 0) {
            System.out.println("File Indexer");
            System.out.println("No directory specified, using default: " + DEFAULT_DIRECTORY);
            System.out.println("Usage: java IndexerMain <directory_path>");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("  java IndexerMain .                    - Index current directory");
            System.out.println("  java IndexerMain /path/to/dir         - Index specified directory");
            System.out.println();
            
            // Use default directory
            rootPath = DEFAULT_DIRECTORY;
        } else {
            rootPath = args[0];
        }
        
        // Build index
        System.out.println("Starting index build for: " + rootPath);
        FileIndexer indexer = new FileIndexer(rootPath);
        indexer.buildIndex();
        
        // Show statistics
        System.out.println("\n=== Indexing Complete ===");
        System.out.println("Total files indexed: " + indexer.getFileInfoMap().size());
        System.out.println("Files with content: " + indexer.getFilePathToContent().size());
        System.out.println("Root directory: " + indexer.getRootDirectory().toAbsolutePath());
        
        long totalSize = indexer.getFileInfoMap().values().stream()
                               .mapToLong(FileSearchEngine.FileInfo::getSize)
                               .sum();
        System.out.println("Total size indexed: " + formatSize(totalSize));
    }
    
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
} 