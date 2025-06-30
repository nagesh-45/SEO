package searchengine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.Desktop;

public class SimpleLiveSearchCLI {
    private final LiveFileSearch searchEngine;
    private final Scanner scanner;
    private String currentSearchPath;
    private List<LiveFileSearch.SearchResult> lastResults;
    
    public SimpleLiveSearchCLI() {
        this.searchEngine = new LiveFileSearch();
        this.scanner = new Scanner(System.in);
        this.currentSearchPath = System.getProperty("user.home");
        this.lastResults = new ArrayList<>();
    }
    
    public void start() {
        System.out.println("=== Simple Live File Search ===");
        System.out.println("Search path: " + currentSearchPath);
        System.out.println("Commands:");
        System.out.println("  <search term> - Search by file name");
        System.out.println("  content <term> - Search by file content");
        System.out.println("  fuzzy <term1 term2 ...> - Search by file name (all terms must be present)");
        System.out.println("  regex <term> - Search by file name using regex");
        System.out.println("  content-regex <term> - Search by file content using regex");
        System.out.println("  path <directory> - Change search directory");
        System.out.println("  open <number> - Open file by number");
        System.out.println("  delete <number> - Delete file by number");
        System.out.println("  <number> - Open file by number (shortcut)");
        System.out.println("  quit - Exit");
        System.out.println();
        
        while (true) {
            System.out.print("Search> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if (input.equalsIgnoreCase("quit")) {
                break;
            }
            
            if (input.startsWith("content ")) {
                String term = input.substring(8).trim();
                if (!term.isEmpty()) {
                    searchByContent(term, false);
                }
            } else if (input.startsWith("regex ")) {
                String term = input.substring(6).trim();
                if (!term.isEmpty()) {
                    searchByName(term, true);
                }
            } else if (input.startsWith("content-regex ")) {
                String term = input.substring(14).trim();
                if (!term.isEmpty()) {
                    searchByContent(term, true);
                }
            } else if (input.startsWith("fuzzy ")) {
                String term = input.substring(6).trim();
                if (!term.isEmpty()) {
                    searchByNameFuzzy(term);
                }
            } else if (input.startsWith("path ")) {
                String path = input.substring(5).trim();
                changeSearchPath(path);
            } else if (input.startsWith("open ")) {
                String numberStr = input.substring(5).trim();
                openFileByNumber(numberStr);
            } else if (input.startsWith("delete ")) {
                String numberStr = input.substring(7).trim();
                deleteFileByNumber(numberStr);
            } else if (input.matches("\\d+")) {
                // Just a number - open file by number
                openFileByNumber(input);
            } else {
                // Default: search by file name
                searchByName(input, false);
            }
        }
        
        searchEngine.shutdown();
        scanner.close();
    }
    
    private void searchByName(String searchTerm, boolean useRegex) {
        String searchType = useRegex ? "Regex name search" : "Name search";
        System.out.println("Searching for files with name " + (useRegex ? "matching regex" : "containing") + ": " + searchTerm);
        System.out.println("Searching in: " + currentSearchPath);
        
        long startTime = System.currentTimeMillis();
        lastResults = searchEngine.searchByName(searchTerm, currentSearchPath, useRegex);
        long endTime = System.currentTimeMillis();
        
        displayResults(searchType, endTime - startTime);
    }
    
    private void searchByContent(String searchTerm, boolean useRegex) {
        String searchType = useRegex ? "Regex content search" : "Content search";
        System.out.println("Searching for files with content " + (useRegex ? "matching regex" : "containing") + ": " + searchTerm);
        System.out.println("Searching in: " + currentSearchPath);
        
        long startTime = System.currentTimeMillis();
        lastResults = searchEngine.searchByContent(searchTerm, currentSearchPath, useRegex);
        long endTime = System.currentTimeMillis();
        
        displayResults(searchType, endTime - startTime);
    }
    
    private void searchByNameFuzzy(String searchTerm) {
        System.out.println("Fuzzy searching for files with name containing all terms: " + searchTerm);
        System.out.println("Searching in: " + currentSearchPath);
        
        long startTime = System.currentTimeMillis();
        lastResults = searchEngine.searchByNameFuzzy(searchTerm, currentSearchPath);
        long endTime = System.currentTimeMillis();
        
        displayResults("Fuzzy name search", endTime - startTime);
    }
    
    private void displayResults(String searchType, long searchTime) {
        System.out.println();
        System.out.println("=== " + searchType + " Results (" + searchTime + "ms) ===");
        
        if (lastResults.isEmpty()) {
            System.out.println("No files found.");
        } else {
            // Print table header
            System.out.println(String.format("%-4s | %-50s | %-10s | %-12s | %s", 
                "#", "File Name", "Size", "Type", "Modified"));
            System.out.println(String.format("%-4s-+-%-50s-+-%-10s-+-%-12s-+-%s", 
                "", "", "", "", ""));
            
            // Print results
            for (int i = 0; i < lastResults.size(); i++) {
                LiveFileSearch.SearchResult result = lastResults.get(i);
                System.out.printf("%-4d | %s%n", i + 1, result.toString());
            }
            
            // Print table footer
            System.out.println(String.format("%-4s-+-%-50s-+-%-10s-+-%-12s-+-%s", 
                "", "", "", "", ""));
        }
        System.out.println();
    }
    
    private void changeSearchPath(String path) {
        Path newPath = Paths.get(path);
        if (Files.exists(newPath) && Files.isDirectory(newPath)) {
            currentSearchPath = newPath.toAbsolutePath().toString();
            System.out.println("Search path changed to: " + currentSearchPath);
        } else {
            System.out.println("Invalid directory: " + path);
        }
    }
    
    private void openFileByNumber(String numberStr) {
        try {
            int number = Integer.parseInt(numberStr);
            
            if (lastResults.isEmpty()) {
                System.out.println("❌ No search results available. Please perform a search first.");
                return;
            }
            
            if (number > 0 && number <= lastResults.size()) {
                LiveFileSearch.SearchResult result = lastResults.get(number - 1);
                System.out.println("Opening file #" + number + ": " + result.getFileName());
                openFile(result.getFilePath());
            } else {
                System.out.println("❌ Invalid file number: " + number);
                System.out.println("Available files: 1-" + lastResults.size());
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format: " + numberStr);
        }
    }
    
    private void openFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            System.out.println("Attempting to open: " + filePath);
            
            if (Files.exists(path)) {
                System.out.println("File exists, opening with system default application...");
                boolean opened = false;
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(path.toFile());
                        System.out.println("✓ File opened successfully with Desktop API: " + path.getFileName());
                        opened = true;
                    } catch (Exception e) {
                        System.out.println("⚠ Desktop API failed: " + e.getMessage());
                    }
                }
                if (!opened) {
                    String os = System.getProperty("os.name").toLowerCase();
                    Process process = null;
                    if (os.contains("mac")) {
                        process = Runtime.getRuntime().exec("open", new String[]{filePath});
                    } else if (os.contains("win")) {
                        process = Runtime.getRuntime().exec("cmd /c start " + filePath);
                    } else {
                        process = Runtime.getRuntime().exec("xdg-open", new String[]{filePath});
                    }
                    boolean success = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                    if (success && process.exitValue() == 0) {
                        System.out.println("✓ File opened successfully (fallback): " + path.getFileName());
                    } else {
                        System.out.println("⚠ File may have opened, but process returned: " + process.exitValue());
                        System.out.println("The file should open in your default application.");
                    }
                }
            } else {
                System.out.println("❌ File not found: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("❌ Error opening file: " + e.getMessage());
            System.out.println("Try opening manually: " + filePath);
        } catch (InterruptedException e) {
            System.out.println("❌ Process interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getMessage());
        }
    }
    
    private void deleteFileByNumber(String numberStr) {
        try {
            int number = Integer.parseInt(numberStr);
            
            if (lastResults.isEmpty()) {
                System.out.println("❌ No search results available. Please perform a search first.");
                return;
            }
            
            if (number > 0 && number <= lastResults.size()) {
                LiveFileSearch.SearchResult result = lastResults.get(number - 1);
                System.out.println("About to delete file #" + number + ": " + result.getFileName());
                System.out.println("Full path: " + result.getFilePath());
                System.out.print("Are you sure? Type 'yes' to confirm: ");
                
                String confirmation = scanner.nextLine().trim();
                if ("yes".equalsIgnoreCase(confirmation)) {
                    deleteFile(result.getFilePath());
                    // Remove from results list
                    lastResults.remove(number - 1);
                    System.out.println("✓ File deleted successfully. Removed from results.");
                } else {
                    System.out.println("❌ Deletion cancelled.");
                }
            } else {
                System.out.println("❌ Invalid file number: " + number);
                System.out.println("Available files: 1-" + lastResults.size());
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format: " + numberStr);
        }
    }
    
    private void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            System.out.println("Attempting to delete: " + filePath);
            
            if (Files.exists(path)) {
                boolean deleted = Files.deleteIfExists(path);
                if (deleted) {
                    System.out.println("✓ File deleted successfully: " + path.getFileName());
                } else {
                    System.out.println("❌ Failed to delete file: " + path.getFileName());
                }
            } else {
                System.out.println("❌ File not found: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SimpleLiveSearchCLI cli = new SimpleLiveSearchCLI();
        cli.start();
    }
} 