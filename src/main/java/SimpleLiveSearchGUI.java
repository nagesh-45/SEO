import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final JList<String> resultsList;
    private final JLabel statusLabel;
    private final JComboBox<String> searchTypeCombo;
    private final JTextField pathField;
    private List<LiveFileSearch.SearchResult> lastResults;
    
    public SimpleLiveSearchGUI() {
        this.searchEngine = new LiveFileSearch();
        this.lastResults = new java.util.ArrayList<>();
        
        setTitle("Simple Live File Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 600);
        setLocationRelativeTo(null);
        
        // Create components
        searchField = new JTextField(30);
        searchTypeCombo = new JComboBox<>(new String[]{"Name", "Content"});
        pathField = new JTextField(System.getProperty("user.home"), 30);
        resultsList = new JList<>();
        statusLabel = new JLabel("Ready");
        
        // Setup layout
        setupLayout();
        setupActions();
        
        // Setup list with double-click functionality
        setupResultsList();
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
        JScrollPane scrollPane = new JScrollPane(resultsList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results (Double-click to open)"));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel for actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        
        JButton clearButton = new JButton("Clear Results");
        bottomPanel.add(clearButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupActions() {
        // Search button action
        JButton searchButton = (JButton) ((JPanel) getContentPane().getComponent(0)).getComponent(6);
        searchButton.addActionListener(e -> performSearch());
        
        // Enter key in search field
        searchField.addActionListener(e -> performSearch());
        
        // Clear button action
        JButton clearButton = (JButton) ((JPanel) getContentPane().getComponent(2)).getComponent(0);
        clearButton.addActionListener(e -> {
            resultsList.setListData(new String[]{});
            lastResults.clear();
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
        if (lastResults.isEmpty()) {
            resultsList.setListData(new String[]{"No files found."});
        } else {
            // Create header and data
            String header = String.format("%-100s  %-8s  %-8s  %s", 
                "File Name", "Size", "Type", "Modified");
            String separator = String.format("%-100s  %-8s  %-8s  %s", 
                "", "", "", "").replace(" ", "-");
            
            // Convert results to display strings
            String[] displayData = new String[lastResults.size() + 2];
            displayData[0] = header;
            displayData[1] = separator;
            
            for (int i = 0; i < lastResults.size(); i++) {
                displayData[i + 2] = lastResults.get(i).toString();
            }
            
            resultsList.setListData(displayData);
        }
        
        statusLabel.setText(String.format("Found %d files in %dms", lastResults.size(), searchTime));
    }
    
    private void setupResultsList() {
        // Set monospaced font for better column alignment
        resultsList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click
                    int index = resultsList.locationToIndex(e.getPoint());
                    // Skip header (index 0) and separator (index 1)
                    if (index >= 2 && index < lastResults.size() + 2) {
                        LiveFileSearch.SearchResult result = lastResults.get(index - 2);
                        openFile(result.getFilePath());
                    }
                }
            }
        });
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