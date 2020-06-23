package crossword;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class GameMain {
    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 900;
    private static final int CANVAS_OFFSET = 50;
    private static final int TEXTBOX_SIZE = 30;
    private static final int BUTTON_SIZE = 10;

    /**
     * Start a Crossword Extravaganza client.
     * @param args The command line arguments should include only the server address.
     * @throws IOException if the arguments are malformed 
     *          *or* the client is unable to connect to the server
     */
    public static void main(String[] args) throws IOException {
        final List<String> arguments = List.of(args);
        
        if (arguments.size() == 0) {
            throw new IllegalArgumentException("missing hostname");
        } if (arguments.size() > 1) {
            throw new IllegalArgumentException("Unexpected arguments: "+ arguments.subList(1, arguments.size()).toString());
        }
        
        final String host = arguments.get(0);
        
        final Client client;
        try {
            client = new Client(host, Server.PORT);
        } catch (IOException e) { //If can't connect to server app doesn't start
            System.out.println("Unable to connect to server");
            System.out.println("Please check server is running on specified hostname");
            return;
        }
        
        final CrosswordCanvas canvas = new CrosswordCanvas(client);
        
        client.startResponsesThread(() -> canvas.repaint());
        
        launchGameWindow(canvas, client);
    }
    
    /**
     * Display a window with a CrosswordCanvas,
     * a text box to enter commands and an Enter button.
     * @throws IOException 
     */
    private static void launchGameWindow(CrosswordCanvas canvas, Client client) throws IOException {        
        canvas.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);

        JTextField textbox = new JTextField(TEXTBOX_SIZE);
        textbox.addActionListener((event) -> {
            String command = textbox.getText();
            client.sendCommand(command);
            textbox.setText("");
        });

        JButton enterButton = new JButton("Enter");
        enterButton.addActionListener((event) -> {
            String command = textbox.getText();
            client.sendCommand(command);
            textbox.setText("");
        });
        enterButton.setSize(BUTTON_SIZE, BUTTON_SIZE);

        JFrame window = new JFrame("Crossword Client");
        window.setLayout(new BorderLayout());
        window.add(canvas, BorderLayout.CENTER);

        JPanel contentPane = new JPanel();
        contentPane.add(textbox);
        contentPane.add(enterButton);

        window.add(contentPane, BorderLayout.SOUTH);

        window.setSize(CANVAS_WIDTH + CANVAS_OFFSET, CANVAS_HEIGHT + CANVAS_OFFSET);

        window.getContentPane().add(contentPane);

        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setVisible(true);
    }
}
