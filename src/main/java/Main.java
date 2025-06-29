import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            // Command line mode
            if (args[0].equalsIgnoreCase("cli")) {
                SimpleLiveSearchCLI cli = new SimpleLiveSearchCLI();
                cli.start();
            } else if (args[0].equalsIgnoreCase("gui")) {
                // GUI mode
                javax.swing.SwingUtilities.invokeLater(() -> {
                    SimpleLiveSearchGUI gui = new SimpleLiveSearchGUI();
                    gui.setVisible(true);
                });
            } else {
                System.out.println("Usage: java -cp target/classes src.Main [cli|gui]");
                System.out.println("  cli - Command line interface");
                System.out.println("  gui - Graphical user interface");
                System.out.println("  (no args) - Interactive mode selection");
            }
        } else {
            // Interactive mode selection
            System.out.println("=== Simple Live File Search ===");
            System.out.println("Choose interface:");
            System.out.println("1. Command Line Interface (CLI)");
            System.out.println("2. Graphical User Interface (GUI)");
            System.out.print("Enter choice (1 or 2): ");
            
            Scanner scanner = new Scanner(System.in);
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("1")) {
                SimpleLiveSearchCLI cli = new SimpleLiveSearchCLI();
                cli.start();
            } else if (choice.equals("2")) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    SimpleLiveSearchGUI gui = new SimpleLiveSearchGUI();
                    gui.setVisible(true);
                });
            } else {
                System.out.println("Invalid choice. Starting CLI by default.");
                SimpleLiveSearchCLI cli = new SimpleLiveSearchCLI();
                cli.start();
            }
            
            scanner.close();
        }
    }
} 