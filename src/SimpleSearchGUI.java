import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class SimpleSearchGUI {
    private JFrame frame;
    private JTextField searchField;
    private JTextArea resultsArea;
    private JButton searchButton;
    private JButton buildButton;
    private FileSearchEngine searchEngine;
    private String rootDirectory;
    
    public SimpleSearchGUI() {
        // Use user's home directory as default
        this.rootDirectory = System.getProperty("user.home");
        this.searchEngine = new FileSearchEngine(rootDirectory);
        
        createGUI();
    }
    
    private void createGUI() {
        frame = new JFrame("File Search Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create top panel for search
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        
        // Search field
        searchField = new JTextField(30);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.addActionListener(e -> performSearch());
        
        // Search button
        searchButton = new JButton("Search");
        searchButton.setFont(new Font("Arial", Font.BOLD, 12));
        searchButton.addActionListener(e -> performSearch());
        
        // Build button
        buildButton = new JButton("Build Index");
        buildButton.setFont(new Font("Arial", Font.BOLD, 12));
        buildButton.addActionListener(e -> buildIndex());
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(searchButton);
        buttonPanel.add(buildButton);
        
        topPanel.add(new JLabel("Enter file name to search:"), BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Results area
        resultsArea = new JTextArea();
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Ready to search. Click 'Build Index' first for best results.");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        
        // Set focus to search field
        searchField.requestFocusInWindow();
        
        // Add key listener for Enter key
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });
    }
    
    private void buildIndex() {
        buildButton.setEnabled(false);
        buildButton.setText("Building...");
        
        // Run indexing in background thread
        SwingUtilities.invokeLater(() -> {
            try {
                searchEngine.buildIndex();
                resultsArea.setText("Index built successfully!\n\nReady to search.");
                buildButton.setText("Rebuild Index");
            } catch (Exception e) {
                resultsArea.setText("Error building index: " + e.getMessage());
            } finally {
                buildButton.setEnabled(true);
            }
        });
    }
    
    private void performSearch() {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            resultsArea.setText("Please enter a search term.");
            return;
        }
        
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");
        
        // Run search in background thread
        SwingUtilities.invokeLater(() -> {
            try {
                List<FileSearchEngine.FileInfo> results = searchEngine.searchByNamePrefix(searchTerm);
                
                if (results.isEmpty()) {
                    resultsArea.setText("No files found matching: '" + searchTerm + "'\n\nTry building the index first.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Found ").append(results.size()).append(" file(s) matching: '").append(searchTerm).append("'\n\n");
                    
                    for (int i = 0; i < results.size(); i++) {
                        FileSearchEngine.FileInfo file = results.get(i);
                        sb.append(i + 1).append(". ").append(file.getFileName()).append("\n");
                        sb.append("   Path: ").append(file.getAbsolutePath()).append("\n");
                        sb.append("   Size: ").append(formatFileSize(file.getSize())).append("\n\n");
                    }
                    
                    resultsArea.setText(sb.toString());
                }
            } catch (Exception e) {
                resultsArea.setText("Error during search: " + e.getMessage());
            } finally {
                searchButton.setEnabled(true);
                searchButton.setText("Search");
            }
        });
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    public void show() {
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        SwingUtilities.invokeLater(() -> {
            SimpleSearchGUI gui = new SimpleSearchGUI();
            gui.show();
        });
    }
} 