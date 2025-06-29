import java.util.*;
import java.io.*;

public class FileSearchCLI {
    private FileSearchEngine searchEngine;
    private Scanner scanner;
    private boolean running;
    private List<FileSearchEngine.FileInfo> lastSearchResults = new ArrayList<>();
    
    public FileSearchCLI(String rootPath) {
        this.searchEngine = new FileSearchEngine(rootPath);
        this.scanner = new Scanner(System.in);
        this.running = true;
    }
    
    public void start() {
        System.out.println("=== File Search Engine CLI ===");
        System.out.println("Type 'help' for available commands");
        System.out.println();
        
        while (running) {
            System.out.print("search> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            processCommand(input);
        }
        
        scanner.close();
    }
    
    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : "";
        
        // Check if input is just a number (for opening files)
        try {
            int fileNumber = Integer.parseInt(input.trim());
            openFileByNumber(input.trim());
            return;
        } catch (NumberFormatException e) {
            // Not a number, continue with normal command processing
        }
        
        switch (command) {
            case "help":
                showHelp();
                break;
            case "build":
                buildIndex();
                break;
            case "search":
                if (argument.isEmpty()) {
                    System.out.println("Usage: search <search_term>");
                } else {
                    searchByNameAndContent(argument);
                }
                break;
            case "name":
                if (argument.isEmpty()) {
                    System.out.println("Usage: name <file_name>");
                } else {
                    searchByName(argument);
                }
                break;
            case "prefix":
                if (argument.isEmpty()) {
                    System.out.println("Usage: prefix <file_prefix>");
                } else {
                    searchByNamePrefix(argument);
                }
                break;
            case "content":
                if (argument.isEmpty()) {
                    System.out.println("Usage: content <search_term>");
                } else {
                    searchByContent(argument);
                }
                break;
            case "open":
                if (argument.isEmpty()) {
                    System.out.println("Usage: open <file_number> or open <file_path>");
                } else {
                    openFile(argument);
                }
                break;
            case "refresh":
                refreshIndex();
                break;
            case "stats":
                showStatistics();
                break;
            case "quit":
            case "exit":
                running = false;
                System.out.println("Goodbye!");
                break;
            default:
                // Treat any unrecognized input as a search by file name
                searchByFileName(input.trim());
        }
    }
    
    private void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  build             - Build the search index");
        System.out.println("  search <term>     - Search by both name and content");
        System.out.println("  name <file>       - Search by exact file name");
        System.out.println("  prefix <prefix>   - Search by file name prefix");
        System.out.println("  content <term>    - Search by file content");
        System.out.println("  open <number>     - Open file by number from last search");
        System.out.println("  open <path>       - Open file by full path");
        System.out.println("  <number>          - Open file by number (shortcut)");
        System.out.println("  refresh           - Refresh the search index");
        System.out.println("  stats             - Show search engine statistics");
        System.out.println("  help              - Show this help message");
        System.out.println("  quit/exit         - Exit the application");
        System.out.println();
        System.out.println("  <any text>        - Search for files with that name/prefix");
        System.out.println();
    }
    
    private void searchByNameAndContent(String searchTerm) {
        System.out.println("\nSearching for: '" + searchTerm + "' (name and content)");
        List<FileSearchEngine.FileInfo> results = searchEngine.searchByNameAndContent(searchTerm);
        displayResults(results);
    }
    
    private void searchByName(String fileName) {
        System.out.println("\nSearching for files with name: '" + fileName + "'");
        List<FileSearchEngine.FileInfo> results = searchEngine.searchByName(fileName);
        displayResults(results);
    }
    
    private void searchByNamePrefix(String prefix) {
        System.out.println("\nSearching for files with prefix: '" + prefix + "'");
        List<FileSearchEngine.FileInfo> results = searchEngine.searchByNamePrefix(prefix);
        displayResults(results);
    }
    
    private void searchByContent(String searchTerm) {
        System.out.println("\nSearching for content: '" + searchTerm + "'");
        List<FileSearchEngine.FileInfo> results = searchEngine.searchByContent(searchTerm);
        displayResults(results);
    }
    
    private void searchByFileName(String fileName) {
        System.out.println("\nSearching for files with name: '" + fileName + "'");
        List<FileSearchEngine.FileInfo> results = searchEngine.searchByNamePrefix(fileName);
        displayResults(results);
    }
    
    private void displayResults(List<FileSearchEngine.FileInfo> results) {
        lastSearchResults.clear();
        lastSearchResults.addAll(results);
        
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
        System.out.println();
    }
    
    private void showStatistics() {
        Map<String, Object> stats = searchEngine.getStatistics();
        System.out.println("\n=== Search Engine Statistics ===");
        System.out.println("Root Directory: " + stats.get("rootDirectory"));
        System.out.println("Status: " + stats.get("status"));
        
        if (searchEngine.isIndexed()) {
            System.out.println("Total Files: " + stats.get("totalFiles"));
            System.out.println("Text Files: " + stats.get("textFiles"));
            System.out.println("Total Size: " + formatFileSize((Long) stats.get("totalSize")));
        } else {
            System.out.println("Index not built yet. Use 'build' command to create index.");
        }
        System.out.println();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private void openFileByNumber(String numberStr) {
        try {
            int fileNumber = Integer.parseInt(numberStr);
            if (fileNumber < 1 || fileNumber > lastSearchResults.size()) {
                System.out.println("Invalid file number. Please use a number between 1 and " + lastSearchResults.size());
                return;
            }
            
            FileSearchEngine.FileInfo file = lastSearchResults.get(fileNumber - 1);
            System.out.println("Opening: " + file.getFileName());
            FileSearchEngine.openFile(file.getAbsolutePath());
            
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }
    
    private void refreshIndex() {
        System.out.println("\nRefreshing the search index...");
        searchEngine.refreshIndex();
        System.out.println("Index refreshed successfully!");
    }
    
    private void buildIndex() {
        System.out.println("\nBuilding the search index...");
        searchEngine.buildIndex();
        System.out.println("Index built successfully!");
    }
    
    private void openFile(String argument) {
        // Check if it's a number (from search results)
        try {
            int fileNumber = Integer.parseInt(argument);
            openFileByNumber(argument);
            return;
        } catch (NumberFormatException e) {
            // Not a number, treat as file path
        }
        
        // Treat as file path
        try {
            java.io.File file = new java.io.File(argument);
            if (!file.exists()) {
                System.out.println("File not found: " + argument);
                return;
            }
            
            System.out.println("Opening: " + file.getName());
            FileSearchEngine.openFile(file.getAbsolutePath());
            
        } catch (Exception e) {
            System.out.println("Could not open file: " + argument);
            System.out.println("Error: " + e.getMessage());
        }
    }
} 