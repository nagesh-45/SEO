import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class FileIndexer {
    private Tree fileNameTrie;
    private Map<String, String> filePathToContent;
    private Map<String, FileSearchEngine.FileInfo> fileInfoMap;
    private Path rootDirectory;
    
    public FileIndexer(String rootPath) {
        this.rootDirectory = Paths.get(rootPath);
        this.fileNameTrie = new Tree();
        this.filePathToContent = new HashMap<>();
        this.fileInfoMap = new HashMap<>();
    }
    
    public void buildIndex() {
        try {
            if (!Files.exists(rootDirectory)) {
                System.err.println("Root directory does not exist: " + rootDirectory);
                return;
            }
            
            System.out.println("Building index for: " + rootDirectory.toAbsolutePath());
            System.out.println("This may take a few moments...");
            dfsTraverse(rootDirectory);
            System.out.println("Index built successfully. Total files indexed: " + fileInfoMap.size());
            
        } catch (Exception e) {
            System.err.println("Error building index: " + e.getMessage());
        }
    }
    
    // DFS traversal to index all files
    private void dfsTraverse(Path currentPath) {
        try {
            if (Files.isDirectory(currentPath)) {
                // Skip system directories that are usually not needed
                String dirName = currentPath.getFileName().toString().toLowerCase();
                if (dirName.equals(".git") || dirName.equals("node_modules") || 
                    dirName.equals("target") || dirName.equals("build") ||
                    dirName.equals(".idea") || dirName.equals(".vscode") ||
                    dirName.equals("library") || dirName.equals("system") ||
                    dirName.equals("cache") || dirName.equals("logs")) {
                    return; // Skip these directories
                }
                
                // Recursively traverse subdirectories
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
                    for (Path entry : stream) {
                        dfsTraverse(entry);
                    }
                } catch (AccessDeniedException e) {
                    // Skip directories we can't access
                    System.err.println("Skipping directory (access denied): " + currentPath);
                } catch (IOException e) {
                    // Skip directories with other IO errors
                    System.err.println("Skipping directory (IO error): " + currentPath + " - " + e.getMessage());
                }
            } else if (Files.isRegularFile(currentPath)) {
                // Skip very large files (> 50MB) to speed up indexing
                try {
                    long fileSize = Files.size(currentPath);
                    if (fileSize > 50 * 1024 * 1024) { // 50MB limit
                        return; // Skip large files
                    }
                    
                    // Index the file
                    indexFile(currentPath);
                    
                    // Show progress every 100 files
                    if (fileInfoMap.size() % 100 == 0) {
                        System.out.println("Indexed " + fileInfoMap.size() + " files...");
                    }
                } catch (IOException e) {
                    System.err.println("Could not index file: " + currentPath + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Catch any other unexpected errors and continue
            System.err.println("Error processing: " + currentPath + " - " + e.getMessage());
        }
    }
    
    // Index a single file
    private void indexFile(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        String absolutePath = filePath.toAbsolutePath().toString();
        
        // Get file info
        FileSearchEngine.FileInfo fileInfo = new FileSearchEngine.FileInfo(
            fileName,
            absolutePath,
            Files.size(filePath),
            Files.getLastModifiedTime(filePath).toMillis()
        );
        
        // Add to Trie for name-based search
        fileNameTrie.insert(fileName, absolutePath);
        
        // Add to HashMap for quick lookup
        fileInfoMap.put(absolutePath, fileInfo);
        
        // Index content for text, Excel, and PDF files
        if (fileName.toLowerCase().endsWith(".xls") || fileName.toLowerCase().endsWith(".xlsx")) {
            try {
                String content = extractExcelContent(filePath);
                filePathToContent.put(absolutePath, content);
            } catch (Exception e) {
                System.err.println("Could not read Excel content of: " + absolutePath);
            }
        } else if (fileName.toLowerCase().endsWith(".pdf")) {
            try {
                String content = extractPdfContent(filePath);
                filePathToContent.put(absolutePath, content);
            } catch (Exception e) {
                System.err.println("Could not read PDF content of: " + absolutePath);
            }
        } else if (isTextFile(fileName)) {
            try {
                String content = readFileContent(filePath);
                filePathToContent.put(absolutePath, content);
            } catch (Exception e) {
                System.err.println("Could not read content of: " + absolutePath);
            }
        }
    }
    
    // Check if file is a text file
    private boolean isTextFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".txt") || 
               lowerName.endsWith(".java") || 
               lowerName.endsWith(".py") || 
               lowerName.endsWith(".js") || 
               lowerName.endsWith(".html") || 
               lowerName.endsWith(".css") || 
               lowerName.endsWith(".md") || 
               lowerName.endsWith(".json") || 
               lowerName.endsWith(".xml") ||
               lowerName.endsWith(".csv") ||
               lowerName.endsWith(".pdf");
    }
    
    // Read file content
    private String readFileContent(Path filePath) throws IOException {
        return new String(Files.readAllBytes(filePath));
    }
    
    // Extract text from PDF files
    private String extractPdfContent(Path filePath) throws IOException {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    // Extract text from Excel files
    private String extractExcelContent(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        StringBuilder sb = new StringBuilder();
        try (InputStream is = Files.newInputStream(filePath)) {
            Workbook workbook = null;
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(is);
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(is);
            }
            if (workbook != null) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            sb.append(cell.toString()).append(" ");
                        }
                    }
                }
                workbook.close();
            }
        }
        return sb.toString();
    }
    
    // Getters for the search engine
    public Tree getFileNameTrie() {
        return fileNameTrie;
    }
    
    public Map<String, String> getFilePathToContent() {
        return filePathToContent;
    }
    
    public Map<String, FileSearchEngine.FileInfo> getFileInfoMap() {
        return fileInfoMap;
    }
    
    public Path getRootDirectory() {
        return rootDirectory;
    }
} 