import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.io.*;
import java.awt.Desktop;

public class SimpleLiveSearchGUI extends JFrame {
    private final LiveFileSearch searchEngine;
    private final JTextField searchField;
    private final JTextArea resultsArea;
    private final JLabel statusLabel;
    private final JComboBox<String> searchTypeCombo;
    private final JTextField pathField;
    private final JTextField fileNumberField;
    private List<LiveFileSearch.SearchResult> lastResults;
    
    public SimpleLiveSearchGUI() {
        this.searchEngine = new LiveFileSearch();
        this.lastResults = new java.util.ArrayList<>();
        
        setTitle("Simple Live File Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Create components
        searchField = new JTextField(30);
        searchTypeCombo = new JComboBox<>(new String[]{"Name", "Content"});
        pathField = new JTextField(System.getProperty("user.home"), 30);
        fileNumberField = new JTextField(5);
        resultsArea = new JTextArea();
        statusLabel = new JLabel("Ready");
        
        // Setup layout
        setupLayout();
        setupActions();
        
        // Make results area read-only
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Top panel for search controls
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Search path
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Search Path:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        topPanel.add(pathField, gbc);
        
        // Search type and term
        gbc.gridx = 0; gbc.gridy = 1;
        topPanel.add(new JLabel("Search Type:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        topPanel.add(searchTypeCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        topPanel.add(new JLabel("Search Term:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        topPanel.add(searchField, gbc);
        
        // Search button
        JButton searchButton = new JButton("Search");
        gbc.gridx = 2; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        topPanel.add(searchButton, gbc);
        
        // Status label
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(statusLabel, gbc);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Results area with scroll pane
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel for actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        
        bottomPanel.add(new JLabel("File #:"));
        bottomPanel.add(fileNumberField);
        
        JButton openButton = new JButton("Open File");
        JButton clearButton = new JButton("Clear Results");
        bottomPanel.add(openButton);
        bottomPanel.add(clearButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupActions() {
        // Search button action
        JButton searchButton = (JButton) ((JPanel) getContentPane().getComponent(0)).getComponent(6);
        searchButton.addActionListener(e -> performSearch());
        
        // Enter key in search field
        searchField.addActionListener(e -> performSearch());
        
        // Enter key in file number field
        fileNumberField.addActionListener(e -> openFileByNumber());
        
        // Open button action
        JButton openButton = (JButton) ((JPanel) getContentPane().getComponent(2)).getComponent(2);
        openButton.addActionListener(e -> openFileByNumber());
        
        // Clear button action
        JButton clearButton = (JButton) ((JPanel) getContentPane().getComponent(2)).getComponent(3);
        clearButton.addActionListener(e -> {
            resultsArea.setText("");
            statusLabel.setText("Results cleared");
        });
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        String searchPath = pathField.getText().trim();
        String searchType = (String) searchTypeCombo.getSelectedItem();
        
        if (searchTerm.isEmpty()) {
            statusLabel.setText("Please enter a search term");
            return;
        }
        
        if (searchPath.isEmpty()) {
            statusLabel.setText("Please enter a search path");
            return;
        }
        
        // Validate path
        Path path = Paths.get(searchPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            statusLabel.setText("Invalid search path: " + searchPath);
            return;
        }
        
        // Perform search in background
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                statusLabel.setText("Searching...");
                
                long startTime = System.currentTimeMillis();
                
                if ("Content".equals(searchType)) {
                    lastResults = searchEngine.searchByContent(searchTerm, searchPath);
                } else {
                    lastResults = searchEngine.searchByName(searchTerm, searchPath);
                }
                
                long endTime = System.currentTimeMillis();
                long searchTime = endTime - startTime;
                
                SwingUtilities.invokeLater(() -> {
                    displayResults(searchType, searchTime);
                });
                
                return null;
            }
        };
        
        worker.execute();
    }
    
    private void displayResults(String searchType, long searchTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(searchType).append(" Search Results (").append(searchTime).append("ms) ===\n\n");
        
        if (lastResults.isEmpty()) {
            sb.append("No files found.\n");
        } else {
            for (int i = 0; i < lastResults.size(); i++) {
                LiveFileSearch.SearchResult result = lastResults.get(i);
                sb.append(String.format("%d. %s\n", i + 1, result.toString()));
            }
        }
        
        resultsArea.setText(sb.toString());
        statusLabel.setText(String.format("Found %d files in %dms", lastResults.size(), searchTime));
    }
    
    private void openFileByNumber() {
        String numberStr = fileNumberField.getText().trim();
        
        if (numberStr.isEmpty()) {
            statusLabel.setText("Please enter a file number");
            return;
        }
        
        try {
            int number = Integer.parseInt(numberStr);
            
            if (lastResults.isEmpty()) {
                statusLabel.setText("No search results available. Please perform a search first.");
                return;
            }
            
            if (number > 0 && number <= lastResults.size()) {
                LiveFileSearch.SearchResult result = lastResults.get(number - 1);
                statusLabel.setText("Opening file #" + number + ": " + result.getFileName());
                openFile(result.getFilePath());
                fileNumberField.setText(""); // Clear the field after opening
            } else {
                statusLabel.setText("Invalid file number: " + number + ". Available: 1-" + lastResults.size());
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid number format: " + numberStr);
        }
    }
    
    private void openFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (Files.exists(path)) {
                statusLabel.setText("Opening file with system default application...");
                boolean opened = false;
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(path.toFile());
                        statusLabel.setText("✓ File opened successfully with Desktop API: " + path.getFileName());
                        opened = true;
                    } catch (Exception e) {
                        statusLabel.setText("⚠ Desktop API failed: " + e.getMessage());
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
                        statusLabel.setText("✓ File opened successfully (fallback): " + path.getFileName());
                    } else {
                        statusLabel.setText("⚠ File may have opened, but process returned: " + process.exitValue());
                    }
                }
            } else {
                statusLabel.setText("❌ File not found: " + filePath);
            }
        } catch (IOException e) {
            statusLabel.setText("❌ Error opening file: " + e.getMessage());
        } catch (InterruptedException e) {
            statusLabel.setText("❌ Process interrupted: " + e.getMessage());
        } catch (Exception e) {
            statusLabel.setText("❌ Unexpected error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleLiveSearchGUI gui = new SimpleLiveSearchGUI();
            gui.setVisible(true);
        });
    }
} 