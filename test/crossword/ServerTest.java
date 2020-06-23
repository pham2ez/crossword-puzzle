package crossword;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


public class ServerTest {
    
    /*
     * Partition:
     *   - START state:
     *      - enter valid player ID
     *      - enter already used player ID
     *      - enter invalid player ID (contains new lines, or empty string)
     *      
     *   - CHOOSE state
     *      - start a new match from a loaded board
     *      - select a non-existant board (use incorrect puzzle id)
     *      - join an existing match
     *      - try to join match with non-existant match ID
     *      - exit
     *      
     *   - WAIT state
     *      - add another player to match you are waiting for
     *      
     *   - PLAY state
     *      - try a word that is correct
     *      - try incorrect word (does not fit in grid spots, and fits but incorrect)
     *      - challenge a correct word
     *      - challenge an incorrect word
     *      - try and challenge using nonexistant id
     *      - exit
     *      
     *   - SCORE state
     *      - show score
     *      - use command NEW MACTH to go to a selection of new matches
     *      - exit
     *      
     */
    
    private static final String LOCALHOST = "127.0.0.1";
    private static final int MAX_CONNECTION_ATTEMPTS = 5;
    
    @Test public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> {
            assert false;
        }, "make sure assertions are enabled with VM argument '-ea'");
    }
    
    /* Start server on its own thread. */
    private static Thread startServer(final Server server) {
        Thread thread = new Thread(() ->  {
            try {
                server.serve();
            } catch (IOException ioe) {
                throw new RuntimeException("serve() threw IOException", ioe);
            }
        });
        thread.start();
        return thread;
    }
    
    /* Connect to server with retries on failure. */
    private static Socket connectToServer(final Thread serverThread, final Server server) throws IOException {
        final int port = server.port();
        assertTrue(port > 0, "server.port() returned " + port);
        for (int attempt = 0; attempt < MAX_CONNECTION_ATTEMPTS; attempt++) {
            try { Thread.sleep(attempt * 10); } catch (InterruptedException ie) { }
            if ( ! serverThread.isAlive()) {
                throw new IOException("server thread no longer running");
            }
            try {
                final Socket socket = new Socket(LOCALHOST, port);
                socket.setSoTimeout(1000 * 3);
                return socket;
            } catch (ConnectException ce) {
                // may try again
            }
        }
        throw new IOException("unable to connect after " + MAX_CONNECTION_ATTEMPTS + " attempts");
    }
    
    // START state
    
    //input valid ID
    @Test @Tag("no-didit")
    public void testStartValidId() throws IOException {
        final Server s = new Server("puzzles", 0);
        final Socket socket = connectToServer(startServer(s), s);
        
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        assertTrue(expectedOutput(in, ServerResponse.State.START), "should start in START state");
        
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have switched to CHOOSE state after entering valid ID");
        socket.close(); 
    }
    
    //input invalid ID (includes newline character)
    @Test @Tag("no-didit")
    public void testStartInvalidId() throws IOException {
        final Server s = new Server("puzzles", 0);
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

        assertTrue(expectedOutput(in, ServerResponse.State.START), "Did not begin in START state");
        out.println("TATUM \n");
        assertTrue(expectedOutput(in, ServerResponse.State.START), "should have stayed in the START state after entering invalid ID");
        out.println("T AT U M");
        assertTrue(expectedOutput(in, ServerResponse.State.START), "should have stayed in the START state after entering invalid ID");
        out.println("");
        assertTrue(expectedOutput(in, ServerResponse.State.START), "should have stayed in the START state after entering invalid ID");
        out.println("EXIT");
    }
    
    //input ID of player that is already in use
    @Test @Tag("no-didit")
    public void testStartAlreadyUsedId() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);

        //add TATUM as id using socket 1
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have moved to CHOOSE state after valid ID entered");
        
        //try to add TATUM as id using socket 2, should not work and stay in choose state
        out2.println("TATUM");
        assertTrue(expectedOutput(in2, ServerResponse.State.START), "should have stayed in START state after previously used ID entered");
        
        socket.close(); 
        socket2.close(); 
    }
    
    
    //CHOOSE and WAIT state
    
    //select to start a new match from a loaded board
    @Test @Tag("no-didit")
    public void testChooseNewMatch() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

        //add TATUM as id
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have moved to CHOOSE state after valid ID entered");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"THE OFFICE IS THE GREATEST SHOW\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should have moved to WAIT state after valid ID entered");

        socket.close(); 
    }
    
    //join previously made match, also tests WAIT state
    @Test @Tag("no-didit")
    public void testChooseWaitJoinMatch() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);

        //add TATUM as id
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have moved to CHOOSE state after valid ID entered");
        
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"END OF SEMESTER\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should have moved to WAIT state after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should have moved to the PLAY state after joining existing match");
        
        //check TATUM in game
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should have moved to the PLAY state after someone joined match");
        out2.println("EXIT");
        out2.println("EXIT");
        out.println("EXIT");

    }
    
    //select to start a new match from a nonexistant loaded board
    @Test @Tag("no-didit")
    public void testChooseNewMatchBadBoard() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

        //add TATUM as id
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have moved to CHOOSE state after valid ID entered");
        
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 PUZZLE2 \"test puzzle\"");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have stayed in CHOOSE after invalid board entered");
            
        socket.close(); 
    }
    
    //join non existant match
    @Test @Tag("no-didit")
    public void testChooseBadMatch() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

        //add TATUM as id
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "Did not change to state CHOOSE");
        
        //now in choose. Select a loaded board
        out.println("PLAY MATCH2");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have stayed in the CHOOSE state after invalid match entered");

        socket.close(); 
    }
    
    //exit in CHOOSE state
    @Test @Tag("no-didit")
    public void testChooseExit() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

        //add TATUM as id
        out.println("TATUM");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "Did not change to state CHOOSE");
        
        //now in choose. exit
        out.println("EXIT");
        socket.close(); 
    }
    
    
    //PLAY state
    
    //test guessing correct word (no confirmation yet)
    @Test @Tag("no-didit")
    public void testPlayCorrectWord() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);

        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"TEST\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should have moved to PLAY state after joined match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should have moved to PLAY state after other player entered");
        
        //BOB guess a word
        out2.println("TRY 1DOWN CAT");
        ServerResponse serverResponse2 = lastServerResponse(in2);
        assertFalse(serverResponse2 == null, "an exception occured while waiting for the Server to return a play object");
        
        assertEquals('c', serverResponse2.charBoard().get(0).get(0).getChar(), "did not add c to the correct crossword spot");
        assertEquals('a', serverResponse2.charBoard().get(1).get(0).getChar(), "did not add a to the correct crossword spot");
        assertEquals('t', serverResponse2.charBoard().get(2).get(0).getChar(), "did not add t to the correct crossword spot");
        
        out2.println("EXIT");
        out2.println("EXIT");
        out.println("EXIT"); 
    }
    
    //test confirming incorrect word
    @Test @Tag("no-didit")
    public void testPlayConfirmIncorrectWord() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"TEST PUZZLE\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word, places CAT on board
        out2.println("TRY 1DOWN CAR");
        ServerResponse serverResponse2 = lastServerResponse(in2);
        assertEquals('c', serverResponse2.charBoard().get(0).get(0).getChar(), "did not add c to the correct crossword spot");
        assertEquals('a', serverResponse2.charBoard().get(1).get(0).getChar(), "did not add a to the correct crossword spot");
        assertEquals('r', serverResponse2.charBoard().get(2).get(0).getChar(), "did not add r to the correct crossword spot");
        
        //now TATUM challenges, which confirms BOB's word car
        out.println("CHALLENGE 1DOWN CAT");
        ServerResponse serverResponse = lastServerResponse(in);
        assertTrue(serverResponse.charBoard().get(0).get(0).isConfirmed(), "should not have confirmed c");
        assertTrue(serverResponse.charBoard().get(1).get(0).isConfirmed(), "should not have confirmed a");
        assertTrue(serverResponse.charBoard().get(2).get(0).isConfirmed(), "should not have confirmed r");
        out.println("EXIT");
        out.println("EXIT");
        out2.println("EXIT");
    }
    
    //test confirming correct word
    @Test @Tag("no-didit")
    public void testPlayConfirmCorrectWord() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"TEST PUZZLE\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word, places CAT on board
        out2.println("TRY 1DOWN CAT");
        ServerResponse serverResponse2 = lastServerResponse(in2);
        assertEquals('c', serverResponse2.charBoard().get(0).get(0).getChar(), "did not add c to the correct crossword spot");
        assertEquals('a', serverResponse2.charBoard().get(1).get(0).getChar(), "did not add a to the correct crossword spot");
        assertEquals('t', serverResponse2.charBoard().get(2).get(0).getChar(), "did not add t to the correct crossword spot");
        
        //now TATUM challenges, which confirms BOB's word car
        out.println("CHALLENGE 1DOWN CAR");
        ServerResponse serverResponse = lastServerResponse(in);
        assertTrue(serverResponse.charBoard().get(0).get(0).isConfirmed(), "did not confirm c");
        assertTrue(serverResponse.charBoard().get(1).get(0).isConfirmed(), "did not confirm a");
        assertTrue(serverResponse.charBoard().get(2).get(0).isConfirmed(), "did not confirm t");
        out.println("EXIT");
        out.println("EXIT");
        out2.println("EXIT");
    }
    
    //test guessing word with incorrect length (will not show up in puzzle)
    @Test @Tag("no-didit")
    public void testPlayIncorrectWordLength() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"TEST\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word
        out2.println("TRY 1DOWN CATS");
        ServerResponse serverResponse2 = lastServerResponse(in2);
        assertEquals('_', serverResponse2.charBoard().get(0).get(0).getChar(), "should not have added word that was too long for spot");
        assertEquals('_', serverResponse2.charBoard().get(1).get(0).getChar(), "should not have added word that was too long for spot");
        assertEquals('_', serverResponse2.charBoard().get(2).get(0).getChar(), "should not have added word that was too long for spot");
        out.println("EXIT");
        out.println("EXIT");
        out2.println("EXIT");
    }
    
    
    //try guessing word with incorrect id
    @Test @Tag("no-didit")
    public void testPlayTryIncorrectId() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id=
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"PUZZLE TIME\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        ServerResponse serverResponse = lastServerResponse(in);
        
        //BOB guess a word
        out2.println("TRY 3ACROSS CAT");
        ServerResponse serverResponse2 = lastServerResponse(in2);
        
        for (int r=0; r<serverResponse2.charBoard().size(); r ++) {
            for (int c=0; c<serverResponse2.charBoard().get(0).size(); c ++) {
                assertEquals(serverResponse2.charBoard().get(r).get(c).getChar(), serverResponse.charBoard().get(r).get(c).getChar(), "board changed after invalid word id tried");
            }
        }
        out2.println("EXIT");
        out2.println("EXIT");
        out.println("EXIT");
    }
    
    //try challenging word with incorrect id
    @Test @Tag("no-didit")
    public void testPlayChallengeIncorrectId() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"I LOVE PUZZLES\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word
        out2.println("TRY 1DOWN CAT");
        ServerResponse serverResponse = lastServerResponse(in);
        
        //TATUM challenge with incorrect ID
        out.println("CHALLENGE 21DOWN CAT");
        ServerResponse serverResponse2 = lastServerResponse(in2);
        
        for (int r=0; r<serverResponse2.charBoard().size(); r ++) {
            for (int c=0; c<serverResponse2.charBoard().get(0).size(); c ++) {
                assertEquals(serverResponse2.charBoard().get(r).get(c).getChar(), serverResponse.charBoard().get(r).get(c).getChar(), "board changed after invalid word id tried");
            }
        }
        out.println("EXIT");
        out.println("EXIT");
        out2.println("EXIT");
    }
    
    //test exit while in the play state
    @Test @Tag("no-didit")
    public void testPlayExit() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"HELLO\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word, places CAT on board
        out2.println("TRY 1DOWN CAT");
        
        //now TATUM exits, sending both BOB and TATUM into SCORE state
        out.println("EXIT");
        assertTrue(expectedOutput(in, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        //BOB in SCORE state
        assertTrue(expectedOutput(in2, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        out.println("EXIT");
        out2.println("EXIT");
    }
    
    
    //SCORE state
    
    //show scores of players
    @Test @Tag("no-didit")
    public void testScore() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"HEHE\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word, places CAT on board
        out2.println("TRY 1DOWN CAT");
        
        //now TATUM exits, sending both BOB and TATUM into SCORE state
        out.println("EXIT");
        assertTrue(expectedOutput(in, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        //BOB in SCORE state
        assertTrue(expectedOutput(in2, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        out.println("EXIT");
        out2.println("EXIT");
        //both close connections
    }
    
    //player selects to go to NEW MATCH (goes to CHOOSE state) 
    @Test @Tag("no-didit")
    public void testScoreNewMatch() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"TEST\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word, places CAT on board
        out2.println("TRY 1DOWN CAT");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //now TATUM exits, sending both BOB and TATUM into SCORE state
        out.println("EXIT");
        assertTrue(expectedOutput(in, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        //BOB ALSO MOVED TO SCORE
        assertTrue(expectedOutput(in2, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        //TATUM wants a new match, goes to the CHOOSE state
        out.println("NEW MATCH");
        assertTrue(expectedOutput(in, ServerResponse.State.CHOOSE), "should have moved to the CHOOSE state after selecting NEW MATCH");
        out2.println("EXIT");
        out.println("EXIT");

    }
    
    //player selects to go to EXIT (terminates connection)
    @Test @Tag("no-didit")
    public void testScoreExit() throws IOException {
        final Server s = new Server("puzzles", 0);
        
        final Socket socket = connectToServer(startServer(s), s);
        final ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        
        final Socket socket2 = connectToServer(startServer(s), s);
        final ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream());
        final PrintWriter out2 = new PrintWriter(new OutputStreamWriter(socket2.getOutputStream(), UTF_8), true);
        
        //add TATUM as id
        out.println("TATUM");
        //now in choose. Select a loaded board
        out.println("NEW MATCH1 SIMPLE_PUZZLE \"6031 PROJECT\"");
        assertTrue(expectedOutput(in, ServerResponse.State.WAIT), "should be waiting after creating new match");
        
        //create new player BOB and join TATUM's match
        out2.println("BOB");
        out2.println("PLAY MATCH1");
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //TATUM game also now in PLAY mode 
        assertTrue(expectedOutput(in2, ServerResponse.State.PLAY), "should now be playing the match");
        
        //BOB guess a word, places CAT on board
        out2.println("TRY 1DOWN CAT");
        
        //now TATUM exits, sending both BOB and TATUM into SCORE state
        out.println("EXIT");
        assertTrue(expectedOutput(in, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        //BOB in SCORE state
        assertTrue(expectedOutput(in2, ServerResponse.State.SCORE), "should have moved to the SCORE state after exit");
        
        //BOB decides to quit
        out2.println("EXIT");
        assertEquals(0, in2.available(), "should have closed connection");
        
        //TATUM also decides to quit
        out.println("EXIT");
        assertEquals(0, in.available(), "should have closed connection");
    }
    
    /**
     * 
     * @param in               stream to receive ServerResponse objects from server
     * @param expectedState    state you are expecting the client to transition
     * @return     true if ServerRepsonse objects indicate client reaches the expectedState, false otherwise
     */
    private boolean expectedOutput(ObjectInputStream in, ServerResponse.State expectedState) {
        try {
            ServerResponse response = (ServerResponse) in.readObject();
            while (response.state() != expectedState) {
                response = (ServerResponse) (ServerResponse) in.readObject();
            }
            return true;
        }
        catch (SocketTimeoutException e) {
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    
    /**
     * 
     * @param in    stream to receive ServerResponse objects from server
     * @return      final ServerRepsonse object that is received from the server by the client (this is the most up to date
     *              version of all parameters). null if no response is received
     */
    private ServerResponse lastServerResponse(ObjectInputStream in) {
        ServerResponse response = null;
        try {
            response = (ServerResponse) in.readObject();
            while (true) {
                response = (ServerResponse) (ServerResponse) in.readObject();
            }
        }
        catch (Exception e) {
            return response;
        }
    }
    
}
