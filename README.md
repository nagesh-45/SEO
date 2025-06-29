# File Search Engine

A powerful file search engine implemented in Java that uses **Trie**, **HashMap**, and **DFS** to efficiently search files by name and content.

## Features

- **Fast File Name Search**: Uses Trie data structure for efficient prefix-based and exact name matching
- **Content Search**: Searches through file contents using HashMap for quick lookups
- **DFS Traversal**: Recursively traverses the file system to build a comprehensive index
- **CLI Interface**: Interactive command-line interface for easy searching
- **Multiple Search Modes**: Search by exact name, name prefix, content, or combined search
- **File Statistics**: Get detailed statistics about indexed files

## Data Structures Used

1. **Trie**: For efficient prefix-based file name searches
2. **HashMap**: For quick file path to content mapping and metadata storage
3. **DFS**: For recursive file system traversal during indexing

## Supported File Types

The engine indexes content for the following text file types:
- `.txt`, `.java`, `.py`, `.js`, `.html`, `.css`, `.md`, `.json`, `.xml`, `.csv`

## How to Compile and Run

### Compilation
```bash
javac *.java
```

### Usage

#### 1. CLI Mode (Interactive)
```bash
# Start CLI with current directory
java Main .

# Start CLI with specific directory
java Main /path/to/your/directory
```

#### 2. Quick Search Mode
```bash
# Quick search in current directory
java Main . "search term"

# Quick search in specific directory
java Main /path/to/directory "search term"
```

## CLI Commands

Once in CLI mode, you can use the following commands:

- `search <term>` - Search by both name and content
- `name <file>` - Search by exact file name
- `prefix <prefix>` - Search by file name prefix
- `content <term>` - Search by file content only
- `stats` - Show search engine statistics
- `help` - Show available commands
- `quit` or `exit` - Exit the application

## Example Usage

### Starting the CLI
```bash
$ java Main .
=== File Search Engine CLI ===
Type 'help' for available commands

Building index for: /Users/username/projects
Index built successfully. Total files indexed: 1250

search> help

Available commands:
  search <term>     - Search by both name and content
  name <file>       - Search by exact file name
  prefix <prefix>   - Search by file name prefix
  content <term>    - Search by file content
  stats             - Show search engine statistics
  help              - Show this help message
  quit/exit         - Exit the application

search> search "hello world"
Searching for: 'hello world' (name and content)
Found 3 file(s):

1. File: hello.txt | Path: /Users/username/projects/hello.txt | Size: 1024 bytes | Modified: Mon Dec 18 10:30:00 EST 2023
2. File: world.java | Path: /Users/username/projects/src/world.java | Size: 2048 bytes | Modified: Mon Dec 18 11:15:00 EST 2023
3. File: greeting.py | Path: /Users/username/projects/python/greeting.py | Size: 512 bytes | Modified: Mon Dec 18 09:45:00 EST 2023

search> stats

=== Search Engine Statistics ===
Root Directory: /Users/username/projects
Total Files: 1250
Text Files: 890
Total Size: 45.2 MB
```

### Quick Search
```bash
$ java Main . "main function"
Building index for: /Users/username/projects

Searching for: 'main function'
Found 5 file(s):

1. File: Main.java | Path: /Users/username/projects/src/Main.java | Size: 1024 bytes | Modified: Mon Dec 18 10:30:00 EST 2023
2. File: App.java | Path: /Users/username/projects/src/App.java | Size: 2048 bytes | Modified: Mon Dec 18 11:15:00 EST 2023

Statistics:
- Total files indexed: 1250
- Text files: 890
```

## Performance Characteristics

- **Trie Search**: O(m) where m is the length of the search term
- **HashMap Lookup**: O(1) average case for file metadata and content
- **DFS Traversal**: O(n) where n is the total number of files and directories
- **Content Search**: O(n * m) where n is the number of text files and m is the average file size

## Architecture

```
Main.java              - Entry point and command-line argument handling
├── FileSearchCLI.java - Interactive CLI interface
├── FileSearchEngine.java - Core search engine logic
│   ├── Trie.java     - Trie data structure for name-based search
│   ├── HashMap       - File path to content mapping
│   └── DFS           - File system traversal
```

## Key Components

1. **Trie.java**: Implements the Trie data structure for efficient prefix-based searches
2. **FileSearchEngine.java**: Main engine that coordinates Trie, HashMap, and DFS
3. **FileSearchCLI.java**: Command-line interface for user interaction
4. **Main.java**: Entry point with argument parsing

## Future Enhancements

- GUI interface using Swing or JavaFX
- Real-time file system monitoring
- Advanced search filters (file size, date, type)
- Search result ranking and relevance scoring
- Support for more file formats
- Index persistence for faster subsequent searches 