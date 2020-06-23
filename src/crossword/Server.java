/* Copyright (c) 2019 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package crossword;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import crossword.CrosswordBoard.Outcome;
import crossword.Match.MatchListener;
import crossword.Match.TooManyPlayersException;
import crossword.ServerResponse.State;

/**
 * class that will handle client requests to play crossword extravagana
 */

public class Server {
    
    public interface ChooseUpdateCallBack {
        /**
         * Callback function to update client's CHOOSE state
         */
        public void call();
    }
    
    private static final String STARTING_ID = "!newuser";
    private static final String PLAY_REGEX = "PLAY [A-Z0-9]+";
    private static final String NEW_REGEX = "NEW [A-Z0-9]+ [A-Z0-9_()]+ \"[A-Z0-9 ]+\"";
    public static final String ID_REGEX = "[A-Z0-9]+";
    public static final String IDWORD_REGEX = "[0-9]+(ACROSS|DOWN) [A-Z\\-]+";
    public static final String PUZZLE_EXTENSION = ".puzzle";
    public static final int PORT = 4949;
    private final ServerSocket serverSocket;
    private final Map<String, State> playerStates;
    private final Map<String, Match> playerMatches;
    private final Map<String, CrosswordBoard> loadedBoards;
    private final Map<String, ChooseUpdateCallBack> inChoose;
    
    /*
     * AF(serverSocket, playerStates, playerMatches, loadedBoards, inChoose) =
     *      A server operating on the socket ServerSocket
     *      With connected players playerStates.keys() with their states mapped by playerStates
     *      With players currently in states WAIT, PLAY, SCORE mapped to their matches by playerMatches
     *      With inChoose.keys() as players currently in CHOOSE state mapped to callback functions to update their screens
     *      And the valid loaded game boards loadedBoards
     *                                                                
     * RI:
     *  - if playerStates[id] == PLAY, WAIT, or SCORE, playerMatches[id] should be a valid match
     *  - if playerStates[id] == CHOOSE, inChoose[id] is valid callback
     *  - players not in those states are not in playerMatches.keys()
     * 
     * SRE:
     *  - fields are private and final and client never has access to them (they are instantiated inside the constructor)
     *  - returned objects are immutable
     *  - defensive copy of board
     *  
     *  Thread Safety:
     *   - playerStates, playerMatches, loadedBoards, and inChoose are all thread safe data types
     *   - methods that change or access the playerMatches, playerStates, inChoose, and loadedBoards are synchronized which will prevent bad interleavings
     *   - serverSocket is not run or changed on multiple threads so there are not be bad interleavings
     * 
     */
    
    /**
     * Start a Crossword Extravaganza server.
     * @param args The command line arguments should include only the folder where
     *             the puzzles are located.
     * @throws IOException if the board is unable to be read or the server cannot start
     */
    public static void main(String[] args) throws IOException {
        final List<String> arguments = List.of(args);
        
        if (arguments.size() == 0) {
            throw new IllegalArgumentException("missing folder");
        } if (arguments.size() > 1) {
            throw new IllegalArgumentException("Unexpected arguments: "+ arguments.subList(1, arguments.size()).toString());
        }
        
        final String folder = arguments.get(0);
        
        new Server(folder, PORT).serve();
    }
    
    /**
     * Make a new text game server using given board
     * @param folder the folder holding game boards
     * @param port 
     * @throws IOException if an error occurs opening the server socket
     */
    public Server(String folder, int port) throws IOException {
        System.out.println("Starting server on:"+port);
        this.serverSocket = new ServerSocket(port);
        this.playerStates = Collections.synchronizedMap(new HashMap<String, ServerResponse.State>());
        this.playerMatches = Collections.synchronizedMap(new HashMap<String, Match>());
        this.loadedBoards = Collections.synchronizedMap(new HashMap<String, CrosswordBoard>());
        this.inChoose = Collections.synchronizedMap(new HashMap<>());
        loadBoards(new File(folder));
        checkrep();
    }
    
    private void checkrep() {
        assert serverSocket != null;
    }
    
    private void loadBoards(File folder) {
        List<File> files = Arrays.asList(folder.listFiles());
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(PUZZLE_EXTENSION)) {
                try {
                    CrosswordBoard board = new CrosswordBoard(file.getAbsolutePath());
                    int i = 2;
                    String name = board.getName();
                    while (loadedBoards.containsKey(name)) {
                        name = board.getName() + "(" + i + ")";
                        i++;
                    }
                    loadedBoards.put(name, board);
                    System.out.println("Loaded: " + fileName + " as " + name);
                } catch (Exception e) 
                    {System.out.println("Failed to Load: " + fileName);}
            }
        }
    }
    
    /**
     * @return the port on which this server is listening for connections
     */
    public int port() {
        return serverSocket.getLocalPort();
    }
    
    /**
     * Run the server, listening for and handling client connections.
     * Never returns normally.
     * 
     * @throws IOException if an error occurs waiting for a connection
     */
    public void serve() throws IOException {
        checkrep();
        while (true) {
            // block until a client connects
            Socket socket = serverSocket.accept();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        handleConnection(socket);
                    } catch (TooManyPlayersException e) {
                        e.printStackTrace();
                        System.out.println("Fatal Error Reached. Closing Server");
                        System.exit(0);
                    } catch (IOException ioe) {
                        ioe.printStackTrace(); // but do not stop serving
                    } finally {
                        try { socket.close(); }
                        catch (IOException e) {}
                    }
                }
            }).start();
        }
    }
    
    /**
     * Handle a single client connection. For each state the client is in, it calls the corresponding 
     * handleStart(), handleChoose, handlePlay(), handleWait(), and handleScore() methods. Reads state 
     * from playerStates[playerID] or, if the playerID is the original STARTING_ID, the methods knows 
     * the player is in the START state 
     * 
     * Returns when the client disconnects.
     * 
     * @param socket    socket connected to client
     * @throws IOException if the connection encounters an error or closes unexpectedly
     */
    private void handleConnection(Socket socket) throws IOException, TooManyPlayersException {
        checkrep();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        String playerID = STARTING_ID;
        try {
            out.writeObject(ServerResponse.createStart()); //Starting message
            
            for (String input = in.readLine(); input != null; input = in.readLine()) {
                System.out.println(playerStates.keySet());
                System.out.println(playerID + ":" + input);
                
                //START state, check if input is valid id
                if (playerID.equals(STARTING_ID)) { 
                    playerID = handleStart(input, out);
                    continue;
                }
                
                if (input.equals("EXIT") && 
                        playerStates.get(playerID) != State.PLAY && 
                        playerStates.get(playerID) != State.WAIT) { 
                    break;
                }
                
                // Below code will only run if not in START state
                switch (playerStates.get(playerID)) {
                    case CHOOSE:  
                        handleChoose(playerID, input, out);
                        break;
                    case WAIT:
                        handleWait(playerID, input, out);
                        break;
                    case PLAY:
                        handlePlay(playerID, input, out);
                        break;
                    case SCORE:
                        handleScore(playerID, input, out);
                        break;
                    default:
                        throw new AssertionError("Unexpected state encountered");
                }
            }
        } finally {
            if (playerStates.getOrDefault(playerID, State.START) == State.PLAY) {
                playerMatches.get(playerID).endGame("");
            }
            playerStates.remove(playerID);
            if (playerMatches.containsKey(playerID))
                playerMatches.remove(playerID);
            if (inChoose.containsKey(playerID))
                inChoose.remove(playerID);

            updateChoosePlayers();
            
            try {
                out.close();
                in.close();
            } catch (SocketException e) {} //Could have failed if client closed without EXIT command, error unimportant
            System.out.println(playerID + " Disconnected");
        }
    }
    
    /**
     * If ID is valid, adds player id to playerStates map, updates the playerState to CHOOSE, 
     * and sends a response to the client that shows the player is now in the CHOOSE state.
     * Otherwise, return original STARTING_ID ID and sends message to client saying the ID was invalid 
     * because it is already in use or is not alphanumeric
     * 
     * @param input  input string from client which is the client's proposed player ID
     * @param out    output stream that can be used to send response objects to the client
     * @return       if the client input an unusable player ID (meaning the ID is already in use or 
     *               the ID is not alphanumeric), this will return STARTING_ID. Otherwise, this will return 
     *               the valid player ID that was inputed
     * @throws IOException
     */
    private synchronized String handleStart(String input, ObjectOutputStream out) throws IOException {
        if (input.matches(ID_REGEX) && !playerStates.containsKey(input)) {
            String playerID = input;
            playerStates.put(playerID, ServerResponse.State.CHOOSE);
            inChoose.put(playerID, () -> {
                    try {
                        out.writeObject(ServerResponse.createChoose(getGames()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }}); //Makes the choose state callback
            out.writeObject(ServerResponse.createChoose(getGames()));
            return input;
        }
        else if (playerStates.containsKey(input)) {
            out.writeObject(ServerResponse.createStart(input + " player ID already in use"));
            return STARTING_ID;
        }
        else{
            out.writeObject(ServerResponse.createStart("Invalid player ID. ID must be alphanumeric"));
            return STARTING_ID;
        }
    }
    
    /**
     * Method to handle the CHOOSE state meaning this will allow players to use commands PLAY, 
     * NEW, and EXIT. If the player uses the PLAY command, they will be added to an existing 
     * match that already contains another player and the state will switch to PLAY. If the 
     * player decides to make a NEW match, the player must input a valid puzzleID which will be the key 
     * for the board in the loadedBoards map. A new match with board corresponding to
     * the puzzle ID and will be created and this player will be moved to the WAIT state. If the player 
     * chooses to EXIT, the session will terminate. If the command was not a command following the 6.031 project guidelines, 
     * a message saying "Couldn't understand command" will be sent
     * 
     * @param playerID   ID of the player that is trying to choose between starting a new match or joining a match 
     *                   with one player
     * @param input      the player's input command
     * @param out        output stream that can be used to send response objects to the client
     * @throws TooManyPlayersException   if a player is trying to be added to a match that already contains 2 players
     * @throws IOException     if the response could not be sent to the client
     */
    private synchronized void handleChoose(String playerID, String input, ObjectOutputStream out) throws TooManyPlayersException, IOException {
        if (input.matches(PLAY_REGEX)) {
            String matchId = input.split(" ")[1];
            Set<String> openGames = new HashSet<String>();
            Set<String> allMatchIds = new HashSet<String>();
            for (Match tempMatch : playerMatches.values()) {
                if (tempMatch.getNumPlayers() < 2) {
                    openGames.add(tempMatch.getMatchId());
                }
                allMatchIds.add(tempMatch.getMatchId());
            }
            if (allMatchIds.contains(matchId) && !openGames.contains(matchId)) {
                //if the player typed in a valid match id but the game is already full
                out.writeObject(ServerResponse.createChoose("That match already has two players", getGames()));
                return;
            }
            for (String otherPlayer : playerMatches.keySet()) {
                Match match = playerMatches.get(otherPlayer);
                if (match.getMatchId().equals(matchId)){ //match is the match this player wants to join
                    playerMatches.put(playerID, match);
                    match.addPlayer(playerID, new MatchListener() {
                        public void updateMatch(boolean firstCall) {
                            try {
                                String message = "";
                                if (firstCall) {
                                    message = "You have joined a match with " + otherPlayer;
                                }
                                out.writeObject(ServerResponse.createPlay(message, match.getBoard().getPlayBoard(), getClues(match), getScores(match)));
                                playerStates.replace(playerID, ServerResponse.State.PLAY);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        public void endMatch(String message) {
                            try {
                                out.writeObject(ServerResponse.createScore(message, getScores(match)));
                                playerStates.replace(playerID, ServerResponse.State.SCORE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            
                        }
                        }); //add player to the match object
                    playerStates.replace(playerID, ServerResponse.State.PLAY); //change this player's state
                    inChoose.remove(playerID);
                    updateChoosePlayers();
                    match.updateGame(true);
                    return;
                }
            }
            out.writeObject(ServerResponse.createChoose("Couldn't find match by that ID", getGames())); //gets here when there is no match with that match ID 
        } else if (input.matches(NEW_REGEX)) { 
            String[] descriptionSplit = input.split("\"");
            String[] tokens = descriptionSplit[0].split(" ");
            
            if (!loadedBoards.containsKey(tokens[2])) {
                out.writeObject(ServerResponse.createChoose("Couldn't find board by that ID", getGames()));
                return;
            }
            boolean unqName = playerMatches.values().stream().filter(m -> m.getMatchId().equals(tokens[1])).count() == 0;
            if (!unqName) {
                out.writeObject(ServerResponse.createChoose("Please specify a unique Match ID", getGames()));
                return;
            }
            
            CrosswordBoard board = loadedBoards.get(tokens[2]);
            Match match = new Match(tokens[1], descriptionSplit[1], board);
            match.addPlayer(playerID, new MatchListener() {
                public void updateMatch(boolean firstCall) {
                    try {
                        for (String otherPlayer : playerMatches.keySet()) {
                            if (!otherPlayer.equals(playerID) && playerMatches.get(otherPlayer).getMatchId().equals(match.getMatchId())){
                                String message = "";
                                if (firstCall) {
                                    message = otherPlayer + " has joined your match";
                                }
                                out.writeObject(ServerResponse.createPlay(message, playerMatches.get(otherPlayer).getBoard().getPlayBoard(), getClues(match), getScores(match)));
                            }
                        }
                        //out.writeObject(ServerResponse.createPlay(board.getPlayBoard(), getClues(match), getScores(match)));
                        playerStates.replace(playerID, ServerResponse.State.PLAY);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                public void endMatch(String message) {
                    try {
                        out.writeObject(ServerResponse.createScore(message, getScores(match)));
                        playerStates.replace(playerID, ServerResponse.State.SCORE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                }
                });
            playerMatches.put(playerID, match);
            inChoose.remove(playerID);
            updateChoosePlayers();
            playerStates.replace(playerID, State.WAIT);
            out.writeObject(ServerResponse.createWait());            
        } else {
            out.writeObject(ServerResponse.createChoose("Couldn't understand command", getGames()));
        }
    }
    
    /**
     * Method to handle the WAIT state. It will allow players to write EXIT which will bring the player 
     * to the CHOOSE state. Otherwise, it will just send a message to the player to wait for another 
     * player to join. Moving a player from the WAIT state to the PLAY state is not done here, it is done using 
     * callbacks given as parameters to the Match
     * 
     * @param playerID    ID of player
     * @param input       string the player had sent server while in WAIT state
     * @param out         output stream that can be used to send response objects to the client
     * @throws IOException   if the out has troubles converting and sending the response object
     */
    private synchronized void handleWait(String playerID, String input, ObjectOutputStream out) throws IOException {
        if (input.equals("EXIT")) {
            playerMatches.remove(playerID);
            inChoose.put(playerID, () -> {
                try {
                    out.writeObject(ServerResponse.createChoose(getGames()));
                } catch (IOException e) {
                    e.printStackTrace();
                }});
            playerStates.replace(playerID, ServerResponse.State.CHOOSE);
            updateChoosePlayers();
            out.writeObject(ServerResponse.createChoose("You stopped waiting for another player to join. Choose a new option", getGames()));
        }
        else {
            out.writeObject(ServerResponse.createWait("Please wait for another player to join.")); //Does nothing until another player joins && new message sent w callback
        }
    }
    
    /**
     * Method to handle the PLAY state, where the player can make moves by using TRY command (to guess a word) 
     * or CHALLENGE command (to challenge a word already placed on the board). When the board is completed correctly 
     * or either player uses the EXIT command, this method will automatically transition the player from the 
     * PLAY state to the SCORE state.
     * 
     * @param playerID   ID of the player that is playing the match 
     * @param input      the move the player wants to make
     * @param out        output stream that can be used to send response objects to the client
     * @throws IOException   if the out has troubles converting and sending the response object
     */
    private synchronized void handlePlay(String playerID, String input, ObjectOutputStream out) throws IOException {
        
        String[] tokens = input.split(" ");
        Match match = playerMatches.get(playerID);
        CrosswordBoard board = match.getBoard();
        if (input.matches("TRY " + IDWORD_REGEX)) {
            String id = tokens[1];
            String word = tokens[2];
            Outcome outcome = board.tryWord(id, word, playerID);
            match.updateGame(false);
            System.out.println(outcome);
            switch (outcome){
                case SUCCESS:
                    out.writeObject(ServerResponse.createPlay("successfully placed word "+word,board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case CONFLICT:
                    out.writeObject(ServerResponse.createPlay(word+" conflicts with another word already on the board",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case WORD_OWNED:
                    out.writeObject(ServerResponse.createPlay("Opponent has already placed a word at "+ id,board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case WRONG_LENGTH:
                    out.writeObject(ServerResponse.createPlay(word + " is incorrect length",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case CONFIRMED:
                    out.writeObject(ServerResponse.createPlay("the word at "+id+" has already been confirmed",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case FINISHED:
                    match.endGame("Congrats! All correct words were placed on the board!");
                    break;
                case NONEXISTENT:
                    out.writeObject(ServerResponse.createPlay(id+" is a nonexistant ID",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                default:
                    out.writeObject(ServerResponse.createPlay(""+ id,board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
            }            
        }
        else if (input.matches("CHALLENGE " + IDWORD_REGEX)){
            String id = tokens[1];
            String word = tokens[2];
            Outcome outcome = board.tryChallenge(id, word, playerID);
            match.updateGame(false);
            switch (outcome){
                case SUCCESS:
                    out.writeObject(ServerResponse.createPlay("successfully removed previous word and replaced with "+word,board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case FAILED: 
                    out.writeObject(ServerResponse.createPlay("your challenge was unsuccesful",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case SAME_WORD:
                    out.writeObject(ServerResponse.createPlay("you challenged with the same word that was on the board",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case CANT_CHALLENGE:
                    out.writeObject(ServerResponse.createPlay("you have challenged your own word or there is no word at "+id,board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case CONFIRMED:
                    out.writeObject(ServerResponse.createPlay("the word at "+id+" has already been confirmed",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case WRONG_LENGTH:
                    out.writeObject(ServerResponse.createPlay(word + " is incorrect length",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                case FINISHED:
                    match.endGame("Congrats! All correct words were placed on the board!");
                    break;
                case NONEXISTENT:
                    out.writeObject(ServerResponse.createPlay(id+" is a nonexistant ID",board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
                default:
                    out.writeObject(ServerResponse.createPlay("Opponent has already placed a word at "+ id,board.getPlayBoard(), getClues(match), getScores(match)));
                    break;
            }
        }
        else if (input.equals("EXIT")) {
            //initiate callback to make all players move to SCORE state
            match.endGame("A player left the match");
        } else {
            out.writeObject(ServerResponse.createPlay("Unparsable command", board.getPlayBoard(), getClues(match), getScores(match)));
        }
    }
    
    /**
     * Method to handle the SCORE state. The player can use commands to start a NEW MATCH, which will switch state to 
     * the CHOOSE state, or EXIT which will terminate the connection.
     * 
     * @param playerID   id of player playing the match from this connection
     * @param input      input command of client
     * @param out        output stream that can be used to send response objects to the client
     * @throws IOException   if the out has troubles converting and sending the response object
     */
    private synchronized void handleScore(String playerID, String input, ObjectOutputStream out) throws IOException {
        if (input.equals("NEW MATCH")) {
            playerStates.replace(playerID, State.CHOOSE);
            playerMatches.remove(playerID); 
            inChoose.put(playerID, () -> {
                try {
                    out.writeObject(ServerResponse.createChoose(getGames()));
                } catch (IOException e) {
                    e.printStackTrace();
                }});
            out.writeObject(ServerResponse.createChoose(getGames()));
        } else {
            Match match = playerMatches.get(playerID);
            out.writeObject(ServerResponse.createScore("Unknown command", getScores(match)));
        }
    }
    
    /**
     * method to update all players in the CHOOSE state. Used when new matches are created, or when matches reach 
     * have two players so they can no longer be joined
     */
    private synchronized void updateChoosePlayers() {
        for (ChooseUpdateCallBack c : inChoose.values()) {
            c.call();
        }
    }
    
    /**
     * method to generate a response for CHOOSE. Will return all available loaded boards in format
     * "Board: loadedBoards[i] loadedBoard[i].description()" where loadedBoards[i] is the board's name, and all open matches (matches 
     * with only one player) in the format "Match: matches[i] matches[i].description()" where matches[i] represent the matches 
     * from the values of playerMatches that contain only one player
     * 
     * @return ServerResponse with state = CHOOSE and list of available games & boards
     */
    private synchronized List<String> getGames() {
        List<String> availGames = new ArrayList<String>();
        for (Match tempMatch: playerMatches.values()) {
            if (tempMatch.getNumPlayers() == 1) {
                availGames.add("Match: " + tempMatch.getMatchId() + " \"" + tempMatch.getDescription() + "\"");
            }
        }
        for (String boardID : loadedBoards.keySet()) {
            availGames.add("Board: " + boardID + " \"" + loadedBoards.get(boardID).getDescription() + "\"");
        }
        return availGames;
    }
    
    /**
     * get the scores of player in match in form [player : score]
     * @param match   match you want to get the player scores from
     * @return  list where each element is the id and score (given in the format above)
     *          of a player
     */
    private synchronized List<String> getScores(Match match) {
        List<String> scores = new ArrayList<String>();
        for (String id : playerMatches.keySet()) { 
            if (playerMatches.get(id).getMatchId() == match.getMatchId()) {
                scores.add(id + ": "+ match.getBoard().showScore(id));
            }
        }
        return scores;
    }
    
    /**
     * get all the clues as strings for a match in the format [word_ID : clue] for all word IDs
     * 
     * @param match  match that you want to get all the clues for
     * @return   list of clues where each element is one clue given in format above
     */
    private synchronized List<String> getClues(Match match){
        List<String> clues = new ArrayList<String>();
        for (String id : match.getBoard().getClues().keySet()) {
            clues.add(id + ": " + match.getBoard().getClues().get(id));
        }
        return clues;
    }
    
    
}
