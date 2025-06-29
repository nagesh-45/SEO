import java.util.*;

public class Tree {
    private TrieNode root;
    
    public Tree() {
        root = new TrieNode();
    }
    
    // Insert a file path into the trie
    public void insert(String fileName, String filePath) {
        TrieNode current = root;
        String lowerFileName = fileName.toLowerCase();
        
        for (char c : lowerFileName.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }
        
        current.isEndOfWord = true;
        current.filePaths.add(filePath);
    }
    
    // Search for files with given prefix
    public List<String> searchByPrefix(String prefix) {
        List<String> result = new ArrayList<>();
        TrieNode current = root;
        String lowerPrefix = prefix.toLowerCase();
        
        // Navigate to the node representing the prefix
        for (char c : lowerPrefix.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return result; // Prefix not found
            }
            current = current.children.get(c);
        }
        
        // Collect all file paths from this node and its descendants
        collectAllPaths(current, result);
        return result;
    }
    
    // Search for exact file name match
    public List<String> searchExact(String fileName) {
        TrieNode current = root;
        String lowerFileName = fileName.toLowerCase();
        
        for (char c : lowerFileName.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return new ArrayList<>();
            }
            current = current.children.get(c);
        }
        
        return current.isEndOfWord ? new ArrayList<>(current.filePaths) : new ArrayList<>();
    }
    
    // Helper method to collect all file paths from a node and its descendants
    private void collectAllPaths(TrieNode node, List<String> result) {
        if (node.isEndOfWord) {
            result.addAll(node.filePaths);
        }
        
        for (TrieNode child : node.children.values()) {
            collectAllPaths(child, result);
        }
    }
    
    // Inner class representing a trie node
    private static class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEndOfWord;
        Set<String> filePaths;
        
        TrieNode() {
            children = new HashMap<>();
            isEndOfWord = false;
            filePaths = new HashSet<>();
        }
    }
} 