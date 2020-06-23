package crossword;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An immutable server response which specifies a state and 
 * holds data specific to that state 
 * @author tim
 */
public class ServerResponse implements Serializable {
    public enum State {START, CHOOSE, WAIT, PLAY, SCORE}
    
    private final State state;
    
    // Used if an invalid command is entered 
    // or invalid ID, etc. "" if no message to display
    private final String message; 
    
    // List of available matches & boards
    // CHOOSE
    private final List<String> availGames;
    
    // game data to display puzzle
    // PLAY
    private final List<List<CrosswordCharacter>> charBoard;
    private final List<String> clues;
    
    // The user scores
    // PLAY, SCORE
    private final List<String> scores;    
    
    /*
     * AF(state, message, availGames, charBoard, clues, scores) =
     *      An object holding display data to be displayed by the client.
     *      Data available depends on the state, for each state, 
     *          fields not specified are null, except for state which is always present & nonnull
     *      
     *      START
     *          message
     *      CHOOSE
     *          message, availGames
     *      WAIT
     *          message
     *      PLAY
     *          message, charBoard, clues, scores
     *      SCORE 
     *          message, scores
     * RI
     *      For state s all of the fields specified in AF for s are nonnull
     *      charBoard obeys RI of CrosswordBoard
     * Rep Exposure
     *      All class variables are private, final and if mutable defensive copied
     *      
     * Thread Safety
     *      Immutable
     */
    
    /**
     * Will depreciate / set private soon. @ServerDevs please migrate to given factory methods 
     * @param state s
     * @param message s
     * @param availGames s
     * @param charBoard s 
     * @param clues s
     * @param scores s
     */
    private ServerResponse(State state, String message, List<String> availGames, List<List<CrosswordCharacter>> charBoard, List<String> clues, List<String> scores) {
        this.state = state;
        this.message = message;
        this.availGames = availGames;
        this.charBoard = charBoard;
        this.clues = clues;
        this.scores = scores;
    }
    
    /**
     * Generates a START response
     * @param message The message returned - Used to inform user they inputed invalid playerID
     * @return the specified START response
     */
    public static ServerResponse createStart(String message) {
        return new ServerResponse(State.START, message, null, null, null, null);
    }
    
    /**
     * Generates a START response
     * @return the specified START response
     */
    public static ServerResponse createStart() {
        return createStart("");
    }
    
    /**
     * Generates a CHOOSE response
     * @param message The message returned - Used to inform user they inputed invalid command
     * @param availGames The matches and boards available to the users in a human readable format
     * @return the specified CHOOSE response
     */
    public static ServerResponse createChoose(String message, List<String> availGames) {
        return new ServerResponse(State.CHOOSE, message, new ArrayList<>(availGames), null, null, null);
    }
    
    /**
     * Generates a CHOOSE response
     * @param availGames The matches and boards available to the users in a human readable format
     * @return the specified CHOOSE response
     */
    public static ServerResponse createChoose(List<String> availGames) {
        return createChoose("", availGames);
    }
    
    /**
     * Generates a WAIT response
     * @param message The message returned - Used to inform user they inputed invalid command
     * @return the specified WAIT response
     */
    public static ServerResponse createWait(String message) {
        return new ServerResponse(State.WAIT, message, null, null, null, null);
    }
    
    /**
     * Generates a WAIT response
     * @return the specified WAIT response
     */
    public static ServerResponse createWait() {
        return createWait("");
    }
    
    /**
     * Generates a PLAY response
     * @param message The message returned - Used to inform user they inputed invalid command
     * @param charBoard the current board to display to the user
     * @param clues The puzzle clues in a human readable format
     * @param scores The scores in a human readable format
     * @return the specified PLAY response
     */
    public static ServerResponse createPlay(String message, List<List<CrosswordCharacter>> charBoard, List<String> clues, List<String> scores) {
        return new ServerResponse(State.PLAY, message, null, charBoard, new ArrayList<>(clues), new ArrayList<>(scores));
    }
    
    /**
     * Generates a PLAY response
     * @param charBoard the current board to display to the user
     * @param clues The puzzle clues in a human readable format
     * @param scores The scores in a human readable format
     * @return the specified PLAY response
     */
    public static ServerResponse createPlay(List<List<CrosswordCharacter>> charBoard, List<String> clues, List<String> scores) {
        return createPlay("", charBoard, clues, scores);
    }
    
    /**
     * Generates a SCORE response
     * @param message The message returned - Used to inform user they inputed invalid command
     * @param scores the scores in a human readable format
     * @return the specified SCORE response
     */
    public static ServerResponse createScore(String message, List<String> scores) {
        return new ServerResponse(State.SCORE, message, null, null, null, new ArrayList<>(scores));
    }

    /**
     * Generates a SCORE response
     * @param scores the scores in a human readable format
     * @return the specified SCORE response
     */
    public static ServerResponse createScore(List<String> scores) {
        return createScore("", scores);
    }
    
    /**
     * @return the current state
     */
    public State state() {
        return state;
    }
    
    /**
     * @return the message
     */
    public String message() {
        if (message.equals(""))
            throw new NoSuchFieldError("No message present");
        return message;
    }
    
    /**
     * @return if the response contains a message
     */
    public boolean hasMessage() {
        return ! message.equals("");
    }
    
    /**
     * @return immutable copy of the available games to join or start
     * @throws NoSuchFieldError if the response is not in the CHOOSE state
     */
    public List<String> availGames() {
        if (state != State.CHOOSE)
            throw new NoSuchFieldError("Not in state CHOOSE");
        return availGames;
    }
    
    /**
     * @return immutable copy of the board of characters
     * @throws NoSuchFieldError if the response is not in the PLAY state
     */
    public List<List<CrosswordCharacter>> charBoard() {
        if (state != State.PLAY)
            throw new NoSuchFieldError("Not in state PLAY");
        return charBoard;
    }
    
    /**
     * @return immutable copy of the word clues
     * @throws NoSuchFieldError if the response is not in the PLAY state
     */
    public List<String> clues() {
        if (state != State.PLAY)
            throw new NoSuchFieldError("Not in state PLAY");
        return clues;
    }
    
    /**
     * @return immutable copy of the player scores
     * @throws NoSuchFieldError if the response is not in the PLAY or SCORE state
     */
    public List<String> scores() {
        if (state != State.PLAY && state != State.SCORE)
            throw new NoSuchFieldError("Not in state PLAY or SCORE");
        return scores;
    }
    
    /*
     * For developing only - will remove in final version
     * DO NOT RELY ON
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("State: %s \n"
                            + "    Message: %s \n"
                            + "    Games: %s \n"
                            + "    Clues: %s \n"
                            + "    Scores: %s", state, message, availGames, charBoard, clues, scores);
    }
}
