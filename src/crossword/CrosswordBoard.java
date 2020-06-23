package crossword;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crossword.Entry.Direction;
import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * A threadsafe, mutable crossword puzzle board game that is playable for two
 * players.
 */
public class CrosswordBoard {

    /*
     * Possible outcomes that are returned for TRY word (tryWord()) and CHALLENGE
     * word (tryChallenge())
     * 
     * CONFIRMED is if the word at wordID is already confirmed
     * WORD_OWNED is if the word at wordID is owned by someone else
     * CONFLICT is if the word you are trying to put in conflicts somewhere
     * SUCCESS is if the word is tried/challenged successfully
     * FAILED if if the word is tried/challenged failed
     * NONEXISTENT is if the wordID given does not exist
     * WRONG_LENGTH is if the word being tried/challenged is the wrong length
     * SAME_WORD is when the word being challenged and the word challenging it are
     * the same
     * FINISHED is if the game is already finished
     * CANT_CHALLENGE is if player is trying to challenge an empty word or the
     * player's own word
     */
    public enum Outcome {
        CONFIRMED, WORD_OWNED, CONFLICT, SUCCESS, FAILED, WRONG_LENGTH, NONEXISTENT, SAME_WORD, FINISHED, CANT_CHALLENGE
    }

    // correct board
    private final List<List<CrosswordCharacter>> finalBoard = Collections.synchronizedList(new ArrayList<>());
    // play board
    private final List<List<CrosswordCharacter>> playBoard = Collections.synchronizedList(new ArrayList<>());
    private final List<Dimension> startLocations = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, String> words = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> clues = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Integer> scores = Collections.synchronizedMap(new HashMap<>());
    private boolean finished = false;
    private String name;
    private String desc;

    /*
     * AF(finalBoard, playBoard, startLocations, words, clues,scores, finished)-
     * finalBoard represents what the finished board should look like, and once
     * playBoard reaches this state, then the game will end and points will be
     * accumulated based on which player input which words in the puzzle. finished
     * represents if this crossword game is finished. playBoard can reach finalBoard
     * through moves made by two players.
     * 
     * playBoard shows the player's current progress within the game, including
     * empty tiles, black tiles, and words that may/may not have been confirmed
     * already.
     * 
     * startLocation[i] represents the starting location of a word in this puzzle
     * with id number i.
     * 
     * words["NumberDirection"] represents the word in this puzzle
     * that is going Direction, either ACROSS or DOWN, with id number Number, 0 <
     * Number < startLocations.size.
     * 
     * clues["NumberDirection"] represents what clues
     * are associated with the word going at Direction, either ACROSS or DOWN, with
     * id number Number, 0 < Number < startLocations.size.
     * 
     * scores[playerID] will give the current score of the player with this
     * playerID.
     * 
     * RI - words.size == clues.size
     * scores.size = 2
     * 
     * RE -
     * - we return void or immutable objects in most of our functions, except for
     * our constructors, getPlayBoard, and getClues.
     * - when we do give out playBoard and clues in getPlayBoard and getClues
     * respectively, we make a deep defensive copy and then give out the copy.
     * - canPlace, confirmedWord, and checkValidity are a private methods and only
     * take in Dimension (mutable), but only reads its int values.
     * - during initialization, we use CrosswordParser.parse(), giving an immutable
     * CrosswordFile which we use to create the game.
     * - It is ok when we take in Entry because the Entry class is immutable.
     * - all of our rep, except for finished, is private and final. we never give
     * out any direct references to our rep.
     * - finished is still private can only be changed by our private functions.
     * 
     * TSE -
     * - all of the maps are wrapped in a Collections.synchronizedMap so that the
     * two threads that do access this board can perform concurrently.
     * - all of the lists and lists inside of a list are wrapped in a
     * Collections.synchronizedList so that the two threads that do access this
     * board can perform concurrently.
     * - tryWord and tryChallenge are the only public mutator functions, so we will
     * wrap those functions with synchronized so that if both players try at the
     * same word, there will be no race condition
     * - the rest are observer functions
     * - CrosswordCharacter is also threadsafe
     * - startLocations is created in initialization and only used to access
     * Dimension and read its width and height, never using any of its mutator
     * functions so startLocations is threadsafe.
     * - String, int, boolean is also threadsafe because it is immutable.
     */

    /**
     * checks that the RI of this class is being held.
     */
    private void checkRep() {
        assert words.size() == clues.size();
        assert scores.keySet().size() <= 2;
    }

    /**
     * makes a new copy of the other CrosswordBoard instance.
     * 
     * @param other the other board to copy to this instance
     */
    public CrosswordBoard(CrosswordBoard other) {
        for (int i = 0; i < other.finalBoard.size(); i++) {
            List<CrosswordCharacter> finalrow = Collections.synchronizedList(new ArrayList<>());
            List<CrosswordCharacter> playRow = Collections.synchronizedList(new ArrayList<>());
            for (int j = 0; j < other.finalBoard.get(0).size(); j++) {
                finalrow.add(new CrosswordCharacter(other.finalBoard.get(i).get(j)));
                playRow.add(new CrosswordCharacter(other.playBoard.get(i).get(j)));
            }
            finalBoard.add(finalrow);
            playBoard.add(playRow);
        }
        for (String wordID : other.words.keySet()) {
            words.put(wordID, other.words.get(wordID));
            clues.put(wordID, other.clues.get(wordID));
        }
        for (Dimension d : other.startLocations) {
            startLocations.add(new Dimension(d));
        }
        for (String player : other.scores.keySet()) {
            scores.put(player, other.scores.get(player));
        }
        this.name = other.name;
        this.desc = other.desc;
    }

    /**
     * create a board for the client
     * 
     * @param filename file to read the board from
     * @throws IOException when the file could not be parsed or the file has an
     *                     incorrect crossword format
     */
    public CrosswordBoard(String filename) throws IOException {
        // get the file contents
        String fileContents = "";
        try {
            fileContents = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (FileNotFoundException fnfe) {
            throw new IOException("file does not exist");
        }
        // parse file contents using ParseLib
        CrosswordFile parsedFile;
        try {
            parsedFile = CrosswordParser.parse(fileContents.substring(0, fileContents.length() - 1));
        } catch (UnableToParseException e) {
            throw new IOException("could not be parsed");
        }
        this.name = parsedFile.getName();
        this.desc = parsedFile.getDescription();
        List<Entry> entries = parsedFile.getEntries();
        createEmptyBoard(entries);
        // add the characters to finalBoard iff they are correctly formatted (inserted
        // at a black tile/tile with the same character) and an empty one for playBoard
        for (Entry entry : entries) {
            for (int i = 0; i < entry.getWord().length(); i++) {
                CrosswordCharacter currentChar;
                boolean starting = false;
                if (i == 0) {
                    giveID(entry);
                    starting = true;
                }
                int index = startLocations.indexOf(new Dimension(entry.getRow(), entry.getCol()));
                CrosswordCharacter playChar = new CrosswordCharacter('_', index + 1, entry.getDirection(), starting);
                if (entry.getDirection() == Direction.DOWN) {
                    currentChar = finalBoard.get(entry.getRow() + i).get(entry.getCol());
                    if (currentChar.isBlack()) {
                        currentChar.changeChar(entry.getWord().charAt(i));
                        playBoard.get(entry.getRow() + i).set(entry.getCol(), playChar);
                    } else if (currentChar.getChar() == entry.getWord().charAt(i)) {
                        playBoard.get(entry.getRow() + i).get(entry.getCol()).addWordID(index + 1, entry.getDirection(),
                                starting);
                    } else {
                        throw new IOException("board entries conflict with one another");
                    }
                } else if (entry.getDirection() == Direction.ACROSS) {
                    currentChar = finalBoard.get(entry.getRow()).get(entry.getCol() + i);
                    if (currentChar.isBlack()) {
                        currentChar.changeChar(entry.getWord().charAt(i));
                        playBoard.get(entry.getRow()).set(entry.getCol() + i, playChar);
                    } else if (currentChar.getChar() == entry.getWord().charAt(i)) {
                        playBoard.get(entry.getRow()).get(entry.getCol() + i).addWordID(index + 1, entry.getDirection(),
                                starting);
                    } else {
                        throw new IOException("board entries conflict with one another");
                    }
                }
            }
        }
        checkRep();
    }

    /**
     * 
     * @return the name of the board with non Alphanumeric & _ characters removed
     *         Also cast to uppercase
     */
    public String getName() {
        return name.toUpperCase().replaceAll(" ", "_").replaceAll("[^A-Z0-9_]", "");
    }

    /**
     * 
     * @return the board description
     */
    public String getDescription() {
        return desc;
    }

    /**
     * clients will use this to try putting the word at a number on the board
     * 
     * @param wordID      of the word we are trying to put in that is in format
     *                    "idNum idDirection"
     * @param wordEntered the word the player is trying to put in at wordID
     * @param playerID    the player that made the move
     * @return true iff word was put into the crossword puzzle board
     */
    public synchronized Outcome tryWord(String wordID, String wordEntered, String playerID) {
        // VALIDITY CHECK
        String word = wordEntered.toLowerCase();
        String correctWordID = wordID.toUpperCase();
        Outcome valid = checkValidity(word, correctWordID, playerID, true);
        if (valid != Outcome.SUCCESS) {
            checkRep();
            return valid;
        }

        Dimension startLocation = startLocations.get(getWordNum(correctWordID) - 1);
        Direction direction = getWordDirect(correctWordID);
        if (canPlace(direction, correctWordID, startLocation, word, playerID)) {
            for (int i = 0; i < words.get(correctWordID).length(); i++) {
                CrosswordCharacter currChar = getPlayChar(i, direction, startLocation);
                String other = currChar.other(correctWordID);
                // clear the current word there and place the new one there
                if (other != correctWordID && !currChar.isEmpty() && currChar.getChar() != word.charAt(i)) {
                    clearWord(playerID, other);
                }
                currChar.changeChar(word.charAt(i));
                currChar.setOwnerOf(playerID, correctWordID);
            }
        } else {
            checkRep();
            return Outcome.CONFLICT;
        }

        if (isFinished()) {
            accumulateWhenFinished();
            checkRep();
            return Outcome.FINISHED;
        }
        checkRep();
        return Outcome.SUCCESS;
    }

    /**
     * clients will use this to challenge a word that is already on the board with
     * their own word
     * 
     * @param wordID   of the word we are trying to put in that is in format
     *                 "idNum direction"
     * @param newWord  the newWord the player is trying to put in at wordID
     * @param playerID the player that made the challenge
     * @return true iff the challenge was won
     */
    public synchronized Outcome tryChallenge(String wordID, String newWord, String playerID) {
        // VALIDITY CHECK
        String word = newWord.toLowerCase();
        String correctWordID = wordID.toUpperCase();
        Outcome valid = checkValidity(word, correctWordID, playerID, false);
        if (valid != Outcome.SUCCESS) {
            checkRep();
            return valid;
        }

        Dimension startLocation = startLocations.get(getWordNum(correctWordID) - 1);
        Direction direction = getWordDirect(correctWordID);
        String currentWord = "";
        for (int i = 0; i < word.length(); i++) {
            currentWord += Character.toString(getPlayChar(i, direction, startLocation).getChar());
        }
        if (currentWord.equals(word)) {
            return Outcome.SAME_WORD;
        }
        if (words.get(correctWordID).equals(currentWord)) {
            // confirms entire word since original word was correct
            for (int i = 0; i < words.get(correctWordID).length(); i++) {
                getPlayChar(i, direction, startLocation).setConfirmed(correctWordID);
            }
        } else if (words.get(correctWordID).equals(word)) {
            // clear the current word there, place the new one, and then confirm because the
            // new word is correct
            for (int i = 0; i < words.get(correctWordID).length(); i++) {
                CrosswordCharacter currChar = getPlayChar(i, direction, startLocation);
                String other = currChar.other(correctWordID);
                if (other != correctWordID && currChar.getChar() != word.charAt(i)) {
                    clearWord(playerID, other);
                }
                currChar.changeChar(word.charAt(i));
                currChar.setOwnerOf(playerID, correctWordID);
                currChar.setConfirmed(correctWordID);
            }
            addPoints(playerID, 2);
            if (isFinished()) {
                accumulateWhenFinished();
                checkRep();
                return Outcome.FINISHED;
            }
            checkRep();
            return Outcome.SUCCESS;
        } else {
            // neither are correct, so clear word
            clearWord("", correctWordID);
        }
        addPoints(playerID, -1);
        checkRep();
        return Outcome.FAILED;
    }

    /**
     * 
     * @param playerID the playerID that's trying to access their score.
     * @return the score of playerID
     */
    public int showScore(String playerID) {
        checkRep();
        return scores.getOrDefault(playerID, 0);
    }

    /**
     * prints out each row followed by '\n'
     */
    @Override public String toString() {
        String output = "";
        for (int i = 0; i < playBoard.size(); i++) {
            for (int j = 0; j < playBoard.get(0).size(); j++) {
                output += Character.toString(playBoard.get(i).get(j).getChar()) + ' ';
            }
            output += '\n';
        }
        checkRep();
        return output;
    }

    /**
     * A list of lists of CrosswordCharacter representation of CrosswordBoard.
     * 
     * @return a look at the current play board
     */
    public List<List<CrosswordCharacter>> getPlayBoard() {
        List<List<CrosswordCharacter>> copy = new ArrayList<>();
        for (List<CrosswordCharacter> row : playBoard) {
            List<CrosswordCharacter> currentRow = new ArrayList<>();
            for (CrosswordCharacter element : row) {
                currentRow.add(new CrosswordCharacter(element));
            }
            copy.add(currentRow);
        }
        checkRep();
        return copy;
    }

    /**
     * @return the clues associated with its respective word id = "idNum direction"
     */
    public Map<String, String> getClues() {
        Map<String, String> copy = new HashMap<>(clues);
        checkRep();
        return copy;
    }

    /* --------------------------- PRIVATE METHODS ------------------------------ */

    /**
     * give the current entry an id in the crossword board that doesn't conflict.
     * 
     * @param entry that we are giving an id to
     * @throws IOException when more than one word is going in the same direction
     *                     at the same starting location
     */
    private void giveID(Entry entry) throws IOException {
        // if there are two words that are starting at the same location going in a
        // different direction, then give it a different id with the same id number
        if (startLocations.contains(new Dimension(entry.getRow(), entry.getCol()))) {
            int index = startLocations.indexOf(new Dimension(entry.getRow(), entry.getCol()));
            if (!clues.containsKey((index + 1) + entry.getDirection().toString())) {
                clues.put((index + 1) + entry.getDirection().toString(), entry.getClue());
                words.put((index + 1) + entry.getDirection().toString(), entry.getWord());
                return;
            } else {
                throw new IOException("more than one word in the same direction and starting location");
            }
        } else {
            startLocations.add(new Dimension(entry.getRow(), entry.getCol()));
            clues.put(startLocations.size() + entry.getDirection().toString(), entry.getClue());
            words.put(startLocations.size() + entry.getDirection().toString(), entry.getWord());
        }
    }

    // creates an empty crossword board with the correct width and length
    private void createEmptyBoard(List<Entry> entries) {
        int width = 0;
        int height = 0;
        // find the word that goes the furthest left and furthest down
        for (Entry entry : entries) {
            if (entry.getDirection() == Direction.DOWN && entry.getWord().length() + entry.getRow() > height) {
                height = entry.getWord().length() + entry.getRow();
            } else if (entry.getDirection() == Direction.ACROSS && entry.getWord().length() + entry.getCol() > width) {
                width = entry.getWord().length() + entry.getCol();
            }
        }
        // create our empty board with width and height
        for (int i = 0; i < height; i++) {
            List<CrosswordCharacter> newRow = new ArrayList<>();
            for (int j = 0; j < width; j++) {
                newRow.add(new CrosswordCharacter());
            }
            finalBoard.add(Collections.synchronizedList(newRow));
            playBoard.add(Collections.synchronizedList(new ArrayList<>(newRow)));
        }
        checkRep();
    }

    // parse unparsedWordID and return the id number associated with the word
    private int getWordNum(String unparsedWordID) {
        final int acrossLength = 6;
        final int downLength = 4;
        if (unparsedWordID.endsWith("ACROSS")) {
            return Integer.parseInt(unparsedWordID.substring(0, unparsedWordID.length() - acrossLength));
        } else {
            return Integer.parseInt(unparsedWordID.substring(0, unparsedWordID.length() - downLength));
        }
    }

    // parse unparsedWordID and return the direction associated with the word
    private Direction getWordDirect(String unparsedWordID) {
        if (unparsedWordID.endsWith("ACROSS")) {
            return Direction.ACROSS;
        } else {
            return Direction.DOWN;
        }
    }

    // add points to playerID's current score
    private void addPoints(String playerID, int points) {
        if (!scores.containsKey(playerID)) {
            scores.put(playerID, 0);
        }
        scores.put(playerID, scores.get(playerID) + points);
    }

    // check the validity of the word before trying/challenging
    private Outcome checkValidity(String word, String correctWordID, String playerID, boolean tryWord) {
        if (finished) {
            return Outcome.FINISHED;
        } else if (!words.containsKey(correctWordID)) {
            return Outcome.NONEXISTENT;
        }
        Dimension startLocation = startLocations.get(getWordNum(correctWordID) - 1);
        Direction direction = getWordDirect(correctWordID);
        CrosswordCharacter startingCell = playBoard.get(startLocation.width).get(startLocation.height);
        if (words.get(correctWordID).length() != word.length()) {
            return Outcome.WRONG_LENGTH;
        } else if (confirmedWord(correctWordID, direction, startLocation)) {
            return Outcome.CONFIRMED;
        } else if (tryWord && !startingCell.canTry(playerID, correctWordID)) {
            return Outcome.WORD_OWNED;
        } else if (!tryWord && !startingCell.canChallenge(playerID, correctWordID)) {
            return Outcome.CANT_CHALLENGE;
        } else {
            return Outcome.SUCCESS;
        }
    }

    // if the game is finished, add the number of words they own to their score
    private void accumulateWhenFinished() {
        for (String key : words.keySet()) {
            Dimension location = startLocations.get(getWordNum(key) - 1);
            String thisOwner = playBoard.get(location.width).get(location.height).getOwnerOf(key);
            if (!thisOwner.equals("")) {
                addPoints(thisOwner, 1);
            }
        }
    }

    // checks if the entire word has been confirmed
    private boolean confirmedWord(String wordID, Direction direction, Dimension startLocation) {
        for (int i = 0; i < words.get(wordID).length(); i++) {
            if (!getPlayChar(i, direction, startLocation).isWordConfirmed(wordID)) {
                return false;
            }
        }
        return true;
    }

    // clear the word at wordID that conflicts with what playerID wants to put in
    private void clearWord(String playerID, String wordID) {
        Direction direction = getWordDirect(wordID);
        Dimension location = startLocations.get(getWordNum(wordID) - 1);
        for (int i = 0; i < words.get(wordID).length(); i++) {
            getPlayChar(i, direction, location).removeOwner(wordID);
        }
    }

    // get the character from playBoard
    private CrosswordCharacter getPlayChar(int index, Direction direction, Dimension startLocation) {
        if (direction == Direction.ACROSS) {
            return playBoard.get(startLocation.width).get(startLocation.height + index);
        } else {
            return playBoard.get(startLocation.width + index).get(startLocation.height);
        }
    }

    // check first to see if the player can enter word or not at wordID
    private boolean canPlace(Direction direction, String wordID, Dimension startLocation, String word,
            String playerID) {
        for (int i = 0; i < word.length(); i++) {
            CrosswordCharacter currChar = getPlayChar(i, direction, startLocation);
            // can only place a conflicting word if player owns the entire cell
            if (currChar.isOwner(playerID)) {
                // and if the word is not already confirmed
                if (currChar.isConfirmed() && currChar.getChar() != word.charAt(i)) {
                    return false;
                }
                continue;
            }
            // must not conflict with what is already on the board
            if (!(currChar.isEmpty() || (currChar.getChar() == word.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the game has ended or not based if the current board is the same as
     * the solution.
     * 
     * @return true iff the game has finished
     */
    private boolean isFinished() {
        // check that all cells in playBoard are the same as finalBoard, the solution
        for (int i = 0; i < finalBoard.size(); i++) {
            for (int j = 0; j < finalBoard.get(0).size(); j++) {
                if (finalBoard.get(i).get(j).getChar() != playBoard.get(i).get(j).getChar()) {
                    return false;
                }
            }
        }
        finished = true;
        return true;
    }
}
