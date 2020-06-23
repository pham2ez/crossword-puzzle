package crossword;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import crossword.Entry.Direction;

/**
 * CrosswordCharacter is a threadsafe, mutable class that represents a block
 * containing a character in a crossword puzzle.
 */
public class CrosswordCharacter implements Serializable {

    private char character;
    private boolean confirmed = false;
    private int startingBlock;
    private final Map<String, String> wordIDs;
    private final Map<String, Boolean> confirmedWords;

    /*
     * AF(character,confirmed,startingBlock,wordIDs,confirmedWords)-
     * character is the current character that was inputed in this tile.
     * 
     * confirmed represents if this crossword block has been confirmed in the game
     * and if it is confirmed, character can no longer be changed.
     * 
     * startingBlock is 0 if it is not a starting block, otherwise it is the
     * starting block of the wordID with idNum = startingBlock.
     * 
     * wordIDs maps which two wordIDs this tile belongs to and if which player has
     * control to which wordID, "" if no player has control to a wordID.
     * 
     * confirmedWords maps which two wordIDs this tile belongs to and if that word
     * specifically has been confirmed, else it is possible for players to confirm.
     * 
     * RI - wordIDs.keys.size <= 2
     * confirmedWords.keys.size <= 2
     * 
     * RE -
     * -all of our rep is private. wordID and startingBlock is final.
     * confirmed, character, startingBlock can only be changed with the functions in
     * the class.
     * -we only take in a mutable CrosswordCharacter, however, we still make a deep,
     * defensive copy to the new instance of CrosswordCharacter.
     * -everything else takes in and feeds back only void or immutable objects, such
     * as enums, String, and boolean
     * -we never release a direct reference to wordIDs and confirmedWords.
     * 
     * TSE -
     * -we wrap wordID and confirmedWords with Collections.SynchronizedMap, and we
     * only put new pairs in setOwnerOf, removeOwner, and addWordID, which we wrap
     * with synchronized, making them threadsafe
     * -startingBlock, character, and confirmed are all primitive values and can
     * only be changed through changeChar, setConfirmed, and addWordID, which are
     * all synchronized
     */

    private void checkRep() {
        assert wordIDs.keySet().size() <= 2;
        assert confirmedWords.keySet().size() <= 2;
    }

    /**
     * black tile
     */
    public CrosswordCharacter() {
        this.character = ' ';
        this.startingBlock = 0;
        this.wordIDs = Collections.synchronizedMap(new HashMap<>());
        this.confirmedWords = Collections.synchronizedMap(new HashMap<>());
        checkRep();
    }

    /**
     * initializes a block in the crossword puzzle that is a starting block.
     * 
     * @param character currently associated to this crossword block
     * @param id        that this tile is associated with
     * @param direction that this tile is associated with
     * @param starting  If this tile is the start of a word labeled id
     */
    public CrosswordCharacter(char character, int id, Direction direction, boolean starting) {
        this.character = character;
        if (starting) {
            this.startingBlock = id;
        } else {
            this.startingBlock = 0;
        }
        this.wordIDs = Collections.synchronizedMap(new HashMap<>());
        this.wordIDs.put(id + direction.toString(), "");
        this.confirmedWords = Collections.synchronizedMap(new HashMap<>());
        this.confirmedWords.put(id + direction.toString(), false);
        checkRep();
    }

    /**
     * make a copy of another CrosswordCharacter
     * 
     * @param cc block to be made a copy of
     */
    public CrosswordCharacter(CrosswordCharacter cc) {
        this.character = cc.character;
        this.startingBlock = cc.startingBlock;
        this.confirmed = cc.confirmed;
        this.wordIDs = Collections.synchronizedMap(new HashMap<>(cc.wordIDs));
        this.confirmedWords = Collections.synchronizedMap(new HashMap<>(cc.confirmedWords));
        checkRep();
    }

    /* ------------------ CONFIRMATION FUNCTIONS ----------------- */

    /**
     * @return true iff this block has been confirmed
     */
    public boolean isConfirmed() {
        checkRep();
        return confirmed;
    }

    /**
     * confirm the wordID at this block
     * 
     * @param wordID to confirm
     */
    public synchronized void setConfirmed(String wordID) {
        confirmed = true;
        confirmedWords.replace(wordID, true);
        checkRep();
    }

    /**
     * @param wordID to check
     * @return true iff the wordID at this block is confirmed
     */
    public boolean isWordConfirmed(String wordID) {
        checkRep();
        return confirmedWords.get(wordID);
    }

    /* ------------------ STARTING BLOCK FUNCTIONS ------------------ */

    /**
     * @return if this letter is the start of a word
     */
    public boolean startOfWord() {
        checkRep();
        return startingBlock != 0;
    }

    /**
     * @return id of word
     * @throws NoSuchFieldError if not start of word
     */
    public String getID() {
        if (!startOfWord()) {
            throw new NoSuchFieldError("Not a starting block");
        }
        checkRep();
        return ((Integer) startingBlock).toString();
    }

    /* ------------------ CHARACTER FUNCTIONS ------------------ */

    /**
     * @return the character associated with this block, turned into a string
     */
    public char getChar() {
        checkRep();
        return character;
    }

    /**
     * Overwrite what is currently written at this tile.
     * 
     * @param character to change this tile to
     */
    public synchronized void changeChar(char character) {
        this.character = character;
        checkRep();
    }

    /**
     * @return true if this tile is an empty tile
     */
    public boolean isEmpty() {
        checkRep();
        return character == '_';
    }

    /**
     * @return true if this tile is a black tile
     */
    public boolean isBlack() {
        checkRep();
        return character == ' ';
    }

    /* ------------------ OWNER/WORDID FUNCTIONS ------------------ */

    /**
     * Check to see that the owner of this tile is not empty and not the player's.
     * 
     * @param playerID see if this player can try to challenge this tile
     * @param wordID   to check for
     * @return true iff player can try to challenge the word at this tile
     */
    public boolean canChallenge(String playerID, String wordID) {
        checkRep();
        return !getOwnerOf(wordID).equals("") && !getOwnerOf(wordID).equals(playerID);
    }

    /**
     * Check to see that the owner of this tile is empty or the player's.
     * 
     * @param playerID see if this player can try to input at this tile
     * @param wordID   to check for
     * @return true iff player can try to input their word in this tile
     */
    public boolean canTry(String playerID, String wordID) {
        checkRep();
        return getOwnerOf(wordID).equals("") || getOwnerOf(wordID).equals(playerID);
    }

    /**
     * This tile is associated with more than one wordID, so add this wordID to this
     * tile.
     * 
     * @param id        number associated with this word
     * @param direction direction associated with this word
     * @param starting  if this tile is a starting number
     * @throws IOException when there are two words overlapping in the same
     *                     direction
     */
    public synchronized void addWordID(int id, Direction direction, boolean starting) throws IOException {
        String wordID = id + direction.toString();
        if (other(wordID).contains(direction.toString())) {
            throw new IOException("overlapping words in the same direction");
        }
        if (starting) {
            this.startingBlock = id;
        }
        wordIDs.put(wordID, "");
        confirmedWords.put(wordID, false);
        checkRep();
    }

    /**
     * @param wordID given
     * @return the other wordID that this tile is associated with that is not the
     *         one given, but return wordID if it is the only wordID here.
     */
    public String other(String wordID) {
        for (String key : wordIDs.keySet()) {
            if (!key.equals(wordID)) {
                return key;
            }
        }
        checkRep();
        return wordID;
    }

    /**
     * @param playerID checking to see if playerID currently owns this entire tile
     * @return true iff playerID is the current owner of all wordIDs that this tile
     *         is associated with
     */
    public boolean isOwner(String playerID) {
        for (String wordID : wordIDs.keySet()) {
            if (!canTry(playerID, wordID)) {
                return false;
            }
        }
        checkRep();
        return true;
    }

    /**
     * @param playerID  the owner of a certain wordID associated with this tile
     * @param setWordID wordID to set the new owner to
     */
    public synchronized void setOwnerOf(String playerID, String setWordID) {
        if (wordIDs.containsKey(setWordID)) {
            wordIDs.replace(setWordID, playerID);
        } else {
            throw new IllegalArgumentException("is not a part of this word id:" + setWordID);
        }
        checkRep();
    }

    /**
     * Remove the owner that is associated with wordID as well as turn the tile back
     * into an empty one if the owner owns both wordIDs associated with this tile.
     * 
     * @param wordID the wordID's owner will be removed
     */
    public synchronized void removeOwner(String wordID) {
        wordIDs.put(wordID, "");
        for (String key : wordIDs.keySet()) {
            if (isConfirmed() || !getOwnerOf(key).equals("")) {
                return;
            }
        }
        changeChar('_');
        checkRep();
    }

    /**
     * get the current owner of a certain word at this tile
     * 
     * @param wordID we want the owner of
     * @return the owner of wordID
     */
    public String getOwnerOf(String wordID) {
        if (wordIDs.containsKey(wordID)) {
            return wordIDs.get(wordID);
        } else {
            throw new IllegalArgumentException("is not a part of this word id:" + wordID);
        }
    }
}
