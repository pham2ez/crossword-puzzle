/* Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package crossword;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;

import crossword.ServerResponse.State;

/**
 * Canvas for the game.
 * @author asolar, tim
 */
class CrosswordCanvas extends JComponent {
    private final Client client;
    
    /*
     * AF(client) = A canvas displaying the data stored in client
     * 
     * RI 
     *      true
     * RE
     *      client is mutable but cannot reach an invalid state, private and final
     *      No other mutable objects inputed or returned      
     *      
     * Thread Safety
     *      Client is a thread safe datatype
     *      synchronized
     */
    
    /**
     * Creates a JComponent drawable interface and connects it to the game client
     * @param client the client object
     * @throws IOException throws IOException if game client is unable to connect to server
     */
    public CrosswordCanvas(Client client) throws IOException {
        this.client = client;
    }
    

    private static final int TEXT_X_OFFSET = 500;   // Horizontal offset from originX for text.
    private static final int ORIGIN_X = 100;        // Horizontal offset from corner for first cell.
    private static final int ORIGIN_Y = 60;         // Vertical offset from corner for first cell.
    private static final int DELTA = 30;            // Size of each cell in crossword
    private static final Font LETTER_FONT = new Font("Arial", Font.PLAIN, DELTA * 4 / 5);   // Font for letters
    private static final Font INDEX_FONT = new Font("Arial", Font.PLAIN, DELTA / 3);        // Font for IDs in the crossword.
    private static final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 16);                // Font for plain text printouts

    /**
     * Draw a cell at position (row, col) in a crossword.
     * @param row Row where the cell is to be placed.
     * @param col Column where the cell is to be placed.
     * @param g Graphics environment used to draw the cell.
     */
    private static void drawCell(int row, int col, Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(Color.BLACK);
        g.drawRect(ORIGIN_X + col * DELTA,
                   ORIGIN_Y + row * DELTA, DELTA, DELTA);
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(ORIGIN_X + col * DELTA + 1,
                ORIGIN_Y + row * DELTA + 1, DELTA-1, DELTA-1);
        g.setColor(oldColor);
    }

    /**
     * Place a letter inside the cell at position (row, col) in a crossword.
     * @param letter Letter to add to the cell.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param g Graphics environment to use.
     */
    private static void letterInCell(String letter, int row, int col, boolean bold, Graphics g) {
        g.setFont(LETTER_FONT);
        if (bold) {
            g.setFont(g.getFont().deriveFont(Font.BOLD));
        } else {
            g.setFont(g.getFont().deriveFont(Font.PLAIN));
        }        
        Color oldColor = g.getColor();
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(letter, ORIGIN_X + col * DELTA + DELTA / 6,
                             ORIGIN_Y + row * DELTA + fm.getAscent() + DELTA / 10);
        g.setColor(oldColor);
    }

    /**
     * Add a vertical ID for the cell at position (row, col).
     * @param id ID to add to the position.
     * @param row Row position of the cell.
     * @param col Column position of the cell.
     * @param g Graphics environment to use.
     */
    private static void printID(String id, int row, int col, Graphics g) {
        Color oldColor = g.getColor();
        g.setColor(Color.BLACK);
        g.setFont(INDEX_FONT);
        g.drawString(id, ORIGIN_X + col * DELTA + DELTA / 8,
                         ORIGIN_Y + row * DELTA + DELTA *5 / 15);
        g.setColor(oldColor);
    }

    private int line = 0; // The current line that println is on
    private void resetLine() {
        line = 0;
    }

    private void println(String s, Graphics g) {
        g.setFont(TEXT_FONT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, ORIGIN_X + TEXT_X_OFFSET, ORIGIN_Y + line * fm.getAscent() * 6 / 5);
        line++;
    }
    
    private void println(String s, Graphics g, boolean error) {
        g.setFont(TEXT_FONT);
        Color oldColor = g.getColor();
        if (error) {
            g.setColor(Color.RED);
        }
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, ORIGIN_X + TEXT_X_OFFSET, ORIGIN_Y + line * fm.getAscent() * 6 / 5);
        g.setColor(oldColor);
        line++;
    }
    
    private static void paintLetter(CrosswordCharacter c, int row, int col, Graphics g) {
        String letter = ((Character)c.getChar()).toString();
        boolean confirmed = c.isConfirmed();
        
        drawCell(row, col, g);
        if (c.startOfWord()) {
            printID(c.getID(), row, col, g);
        }
        if (!c.isEmpty()) {
            letterInCell(letter, row, col, confirmed, g);
        }
    }
    
    private static void paintBoard(List<List<CrosswordCharacter>> board, Graphics g) {
        for (int y = 0; y < board.size(); y++) {
            for (int x = 0; x < board.get(0).size(); x++) {
                CrosswordCharacter c = board.get(y).get(x);
                if (!c.isBlack())
                    paintLetter(c, y, x, g);
            }
        }
    }
    
    private void paintPlay(ServerResponse resp, Graphics g) {
        println("Enter one of the following commands", g);
        println("TRY [id] [word]", g);
        println("CHALLENGE [id] [word]", g);
        println("EXIT", g);
        
        if (resp.hasMessage())
            println(resp.message(), g, true);
        
        println("", g);
        println("Clues", g);
        for (String c : resp.clues()) {
            println(c, g);
        }
        
        println("", g);
        println("Scores", g);
        for (String s : resp.scores()) {
            println(s, g);
        }
        
        paintBoard(resp.charBoard(), g);
        
    }

    private void paintStart(ServerResponse resp, Graphics g) {
        println("Enter a player ID", g);
        println("Only alphanumeric characters are acceptable", g);
        if (resp.hasMessage()) {
            println(resp.message(), g, true);
        }
    }
    
    private void paintChoose(ServerResponse resp, Graphics g) {
        println("Enter one of the following commands", g);
        println("PLAY [Match_ID]", g);
        println("NEW [Match_ID] [Puzzle_ID] \"[Description]\"", g);
        println("EXIT", g);
        if (resp.hasMessage())
            println(resp.message(), g, true);
        println("",g);
        println("Games", g);
        for (String game : resp.availGames()) {
            println(game, g);
        }
    }
    
    private void paintWait(ServerResponse resp, Graphics g) {
        println("Currently waiting for a player to join", g);
        println("To exit enter: EXIT", g);
        if (resp.hasMessage())
            println(resp.message(), g, true);
    }
    
    private void paintScore(ServerResponse resp, Graphics g) {
        println("Enter one of the following commands", g);
        println("NEW MATCH", g);
        println("EXIT", g);
        if (resp.hasMessage())
            println(resp.message(), g, true);
        println("",g);
        println("Scores", g);
        for (String score : resp.scores()) {
            println(score, g);
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        ServerResponse resp = client.currentResponse();
        switch (resp.state()) {
            case START: 
                paintStart(resp, g);
                break;
            case CHOOSE:
                paintChoose(resp, g);
                break;
            case WAIT:
                paintWait(resp, g);
                break;
            case PLAY:
                paintPlay(resp, g);
                break;
            case SCORE:
                paintScore(resp, g);
                break;
            default:
                throw new AssertionError("Received response with unexpected state");
            
        }
        resetLine();
    }
}
