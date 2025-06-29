import java.util.*;

public class Main {
    // Default directory to search if no argument provided
    private static final String DEFAULT_DIRECTORY = System.getProperty("user.home");
    
    public static void main(String[] args) {
        String rootPath;
        
        if (args.length == 0) {
            System.out.println("File Search Engine");
            System.out.println("No directory specified, using default: " + DEFAULT_DIRECTORY);
            System.out.println("Usage: java Main <directory_path> [search_term]");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("  java Main .                    - Start CLI with current directory");
            System.out.println("  java Main /path/to/dir         - Start CLI with specified directory");
            System.out.println("  java Main . \"hello world\"     - Quick search in current directory");
            System.out.println();
            
            // Use default directory
            rootPath = DEFAULT_DIRECTORY;
        } else {
            rootPath = args[0];
        }
        
        if (args.length <= 1) {
            // Start CLI mode
            FileSearchCLI cli = new FileSearchCLI(rootPath);
            cli.start();
        } else {
            // Quick search mode
            String searchTerm = args[1];
            performQuickSearch(rootPath, searchTerm);
        }
    }
    
    private static void performQuickSearch(String rootPath, String searchTerm) {
        System.out.println("Building index for: " + rootPath);
        FileSearchEngine searchEngine = new FileSearchEngine(rootPath);
        
        System.out.println("\nSearching for: '" + searchTerm + "'");
        List<FileSearchEngine.FileInfo> results = searchEngine.searchByNameAndContent(searchTerm);
        
        if (results.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Found " + results.size() + " file(s):");
            System.out.println();
            
            for (int i = 0; i < results.size(); i++) {
                FileSearchEngine.FileInfo file = results.get(i);
                System.out.println((i + 1) + ". " + file.toString());
            }
        }
        
        // Show statistics
        Map<String, Object> stats = searchEngine.getStatistics();
        System.out.println("\nStatistics:");
        System.out.println("- Total files indexed: " + stats.get("totalFiles"));
        System.out.println("- Text files: " + stats.get("textFiles"));
    }
} 