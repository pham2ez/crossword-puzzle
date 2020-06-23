package crossword;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mutable class that represents a crossword match
 */
public class Match {
    /*
     * AF(matchID, description, playerIDs, board) = 
     *      A crossword match with match ID matchID, description description, 
     *      board board, and containing players playerIDs.keys(). playerIDs.values()
     *      are the callback functions to update player screens
     *      
     * RI: 
     *  - matchID is not an empty string
     *  - playerIDs can never contain more than 2 array elements
     *  
     * SRE: 
     *  - fields are private and final
     *  - returned values are immutable (except getBoard())
     *  - match objects are created by the server and not accessible to the client so 
     *    a client will never be able to call getBoard() to have access to the crossword board
     *    
     * Thread Safety:
     *  - matchID and description are both immutable
     *  - board is a thread safe data type
     *  - playerIDs is a thread safe data type
     *  - Methods updating or viewing playerIDs or board are synchronized, so there can not be bad interleavings
     * 
     */
    
    public class TooManyPlayersException extends Exception{
        /**
         * Creates an error for when an additional player is added to a full match
         * @param message  output message 
         */
        public TooManyPlayersException(String message) {
            super(message);
        }
    }
    
    
    public interface MatchListener{
        /**
         * function to update player's screen during the PLAY state
         * 
         * @param firstCall   notify the method as to whether the client just transitioned to the PLAY state
         *                    so this is the first update to be done on the screen
         */
        void updateMatch(boolean firstCall);
        
        /**
         * function to update player's screen during the SCORE state
         * 
         * @param message     message to the player's SCORE state screen when this method is called
         */
        void endMatch(String message);
    }
    
    private final String matchID;
    private final String description;
    private final Map<String, MatchListener> playerIDs;
    private final CrosswordBoard board;
    
    /**
     * 
     * @param matchID      ID of Crossword Extravaganza Match
     * @param description  description of match
     * @param board        object describing the crossword board
     */
    public Match(String matchID, String description, CrosswordBoard board) {
        this.matchID = matchID;
        this.description = description;
        this.playerIDs = Collections.synchronizedMap(new HashMap<String, MatchListener>());
        this.board = new CrosswordBoard(board);
        checkRep();
    }
    
    /**
     * asserts the RI written above
     */
    private void checkRep() {
        assert !matchID.equals("");
        assert playerIDs.size() <= 2;
    }
    
    /**
     * add a player to the match. a maximum of two players can be added to any match. This also calls the 
     * updateGame() if the player being added is the second player, along with boolean true signifying 
     * this is the first transition from CHOOSE or WAIT state (when the match had only one player)
     * to the PLAY state.
     * 
     * @param playerID   ID representing the new player added to the board
     * @param listener   listener that can be used to update the board when either player makes a change
     * @throws TooManyPlayersException  throws when method is called on match already containing two players 
     */
    public synchronized void addPlayer(String playerID, MatchListener listener) throws TooManyPlayersException {
        if (playerIDs.size() < 2 ) {
            playerIDs.put(playerID, listener);
            updateGame(true);
        }
        else {
            throw new TooManyPlayersException("trying to add additional player to full match or two players");
        }
        checkRep();
    }
    
    /**
     * calls the callbacks for both players to update all player's PLAY state boards in the client
     * 
     * @param firstCall  boolean to determine if this is the first time the updateGame method is being called 
     *                   (meaning it is the first call after the state switched from CHOOSE to PLAY). This is 
     *                   useful if the server wants to send a specific method the first time the PLAY screen appears
     */
    public synchronized void updateGame(boolean firstCall) {
        for (MatchListener listener : playerIDs.values()) {
            listener.updateMatch(firstCall);
        }
    }
    
    /**
     * calls the callbacks for both players so, after one player has exited the match, both move on to SCORE state
     * 
     * @param message   message that will be passed to the listener. This can be useful if the caller wants a specific 
     *                  message to be shown on the screen 
     */
    public synchronized void endGame(String message) {
        for (MatchListener listener : playerIDs.values()) {
            listener.endMatch(message);
        }
    }
    
    /**
     * @return  number of players currently playing in the match
     */
    public synchronized int getNumPlayers() {
        return playerIDs.size();
    }
    
    /**
     * @return  Board used in this match
     */
    public synchronized CrosswordBoard getBoard() {
        return board;
    }
    
    /**
     * @return user specified description of the match
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @return list of the player ids of those int his match
     */
    public synchronized Set<String> getPlayerIds(){
        return playerIDs.keySet();
    }
    
    /**
     * @return  id of the match
     */
    public String getMatchId() {
        return matchID;
    }

}
