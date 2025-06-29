# Simple Live File Search

A lightweight Java file search engine that searches the filesystem directly without any indexing. Perfect for quick searches without the overhead of building and maintaining an index.

## Features

- **Live Search**: Searches files directly on the filesystem - no indexing required
- **Name Search**: Find files by name (supports partial matches)
- **Content Search**: Search within text file contents
- **Cross-platform**: Works on Windows, macOS, and Linux
- **Dual Interface**: Both command-line (CLI) and graphical (GUI) interfaces
- **File Opening**: Open found files directly with system default applications
- **Smart Filtering**: Automatically skips system directories and large files

## Quick Start

### Running the JAR

1. **Double-click the JAR** (on supported systems):
   ```
   target/simple-live-search-1.0.0.jar
   ```

2. **Command line**:
   ```bash
   # Interactive mode selection
   java -jar target/simple-live-search-1.0.0.jar
   
   # Direct CLI mode
   java -jar target/simple-live-search-1.0.0.jar cli
   
   # Direct GUI mode
   java -jar target/simple-live-search-1.0.0.jar gui
   ```

### Building from Source

```bash
# Compile
mvn compile

# Build JAR
mvn package

# Run tests
mvn test
```

## Usage

### Command Line Interface (CLI)

The CLI provides an interactive search experience:

```
=== Simple Live File Search ===
Search path: /Users/username
Commands:
  <search term> - Search by file name
  content <term> - Search by file content
  path <directory> - Change search directory
  open <number> - Open file by number
  quit - Exit

Search> resume
Searching for files with name containing: resume
Searching in: /Users/username

=== Name search Results (245ms) ===

1. resume.pdf (NAME, 1024000 bytes, Mon Jun 30 00:30:15 IST 2025)
2. resume_updated.docx (NAME, 2048000 bytes, Mon Jun 30 00:25:30 IST 2025)

Search> open 1
Opening: /Users/username/Documents/resume.pdf
```

### Graphical User Interface (GUI)

The GUI provides a user-friendly interface with:
- Search path configuration
- Search type selection (Name/Content)
- Real-time search results
- File opening capabilities
- Status updates

## Search Features

### Name Search
- Searches file names (case-insensitive)
- Supports partial matches
- Results sorted by relevance (exact matches first)

### Content Search
- Searches within text file contents
- Supports common text file formats (.txt, .md, .java, .py, .js, .html, .css, .xml, .json, .csv, etc.)
- Skips binary files and files larger than 10MB
- Results sorted by file size (smaller files first)

### Smart Directory Filtering
Automatically skips system directories:
- `.git`, `.svn`, `.hg`
- `node_modules`, `target`, `build`, `bin`, `obj`
- `Library`, `System`, `Applications` (macOS)
- `private`, `var`, `tmp`, `usr` (Unix)

## Performance

- **No Indexing**: Searches are performed live, so results are always current
- **Fast Name Search**: File system traversal optimized for name matching
- **Content Search**: Limited to text files under 10MB for performance
- **Background Processing**: GUI searches run in background threads

## System Requirements

- Java 11 or higher
- Cross-platform compatibility (Windows, macOS, Linux)

## File Opening

The application can open files using the system's default applications:
- **Windows**: Uses `cmd /c start`
- **macOS**: Uses `open`
- **Linux**: Uses `xdg-open`

## Limitations

- Content search is limited to text files under 10MB
- No support for searching within binary files (PDF, Excel, etc.)
- Search speed depends on filesystem size and structure
- No persistent search history or bookmarks

## Advantages of Live Search

✅ **Always Current**: Results reflect the current state of the filesystem
✅ **No Setup**: No indexing required - works immediately
✅ **Lightweight**: Minimal memory and disk usage
✅ **Simple**: Easy to understand and maintain
✅ **Cross-platform**: Works consistently across operating systems

## Disadvantages of Live Search

❌ **Slower**: Each search scans the filesystem from scratch
❌ **No History**: No persistent search history or caching
❌ **Limited Content**: Only searches text files, not binary formats
❌ **Resource Usage**: Can be resource-intensive on large filesystems

## Troubleshooting

### JAR Won't Open
If double-clicking the JAR doesn't work:
1. Open terminal/command prompt
2. Navigate to the JAR directory
3. Run: `java -jar simple-live-search-1.0.0.jar`

### Permission Issues
If you get permission errors:
1. Ensure you have read access to the search directory
2. Try running with elevated privileges if needed
3. Check file system permissions

### Slow Performance
If searches are slow:
1. Try searching in smaller directories
2. Use more specific search terms
3. Consider using name search instead of content search for large directories

## Development

The project uses Maven for build management. Key classes:
- `LiveFileSearch`: Core search engine
- `SimpleLiveSearchCLI`: Command-line interface
- `SimpleLiveSearchGUI`: Graphical interface
- `Main`: Entry point with mode selection
