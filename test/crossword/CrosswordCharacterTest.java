/*
 * Copyright (c) 2018 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import crossword.Entry.Direction;

public class CrosswordCharacterTest {

    /*
     * Partitions -
     * void setConfirmed(String wordID) -
     * wordID exists/doesn't exist
     * 
     * boolean isWordConfirmed(String wordID) -
     * return true/false
     * 
     * boolean startOfWord() -
     * is/is not start tile of word
     * 
     * String getID() -
     * return id tile is a part of
     * 
     * char getChar() -
     * return char of the tile
     * 
     * void changeChar(char character) -
     * changes tile to character
     * 
     * boolean isEmpty() -
     * return true/false
     * 
     * boolean isBlack() -
     * return true/false
     * 
     * boolean canChallenge(String playerID, String wordID) -
     * return true/false
     * 
     * boolean canTry(String playerID, String wordID) -
     * return true/false
     * 
     * void addWordID(int id, Direction direction, boolean starting) throws
     * IOException -
     * adds a wordID to wordIDs
     * 
     * String other(String wordID) -
     * return wordID/other wordID
     * 
     * boolean isOwner(String playerID) -
     * return true/false
     * 
     * void setOwnerOf(String playerID, String setWordID) -
     * set the owner from the wordID associate to this tile
     * 
     * void removeOwner(String playerID, String wordID) -
     * remove the owner from the wordID associate to this tile
     * 
     * String getOwnerOf(String wordID) -
     * return no owner/owner
     */

    @Test public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> {
            assert false;
        }, "make sure assertions are enabled with VM argument '-ea'");
    }

    /*
     * boolean canChallenge(String playerID, String wordID) -
     * return true/false
     * 
     * void addWordID(int id, Direction direction, boolean starting) throws
     * IOException -
     * adds a wordID to wordIDs
     * 
     * String other(String wordID) -
     * return wordID/other wordID
     * 
     * boolean isOwner(String playerID) -
     * return true/false
     * 
     * void setOwnerOf(String playerID, String setWordID) -
     * set the owner from the wordID associate to this tile
     * 
     * void removeOwner(String playerID, String wordID) -
     * remove the owner from the wordID associate to this tile
     * 
     * String getOwnerOf(String wordID) -
     * return no owner/owner
     */
    @Test public void testConfirmChallenge() throws IOException {
        CrosswordCharacter cc = new CrosswordCharacter('c', 1, Direction.ACROSS, false);
        assertFalse(cc.startOfWord());
        assertThrows(IOException.class, () -> {
            cc.addWordID(2, Direction.ACROSS, true);
        });
        cc.addWordID(3, Direction.DOWN, true);

        cc.setOwnerOf("p1", "1ACROSS");
        assertTrue(cc.getOwnerOf("1ACROSS").equals("p1"));
        cc.setOwnerOf("p1", "3DOWN");
        assertTrue(cc.canChallenge("p2", "1ACROSS"));
        assertTrue(cc.canChallenge("p2", "3DOWN"));
        assertTrue(cc.getOwnerOf("3DOWN").equals("p1"));
        assertTrue(cc.isOwner("p1"));
        cc.setOwnerOf("p2", "3DOWN");
        assertFalse(cc.canChallenge("p2", "3DOWN"));
        assertTrue(cc.canChallenge("p1", "3DOWN"));
        assertTrue(cc.getOwnerOf("3DOWN").equals("p2"));
        assertFalse(cc.isOwner("p1"));
        assertFalse(cc.isOwner("p2"));
        cc.removeOwner("3DOWN");
        assertFalse(cc.canChallenge("p1", "3DOWN"));
        assertTrue(cc.isOwner("p1"));
        assertTrue(cc.getOwnerOf("3DOWN").equals(""));
        assertTrue(cc.other("3DOWN").equals("1ACROSS"));
        assertTrue(cc.other("1ACROSS").equals("3DOWN"));
        cc.setConfirmed("3DOWN");
        assertFalse(cc.isWordConfirmed("1ACROSS"));
        assertTrue(cc.isWordConfirmed("3DOWN"));
    }

    /*
     * boolean canTry(String playerID, String wordID) -
     * return true/false
     * 
     * void addWordID(int id, Direction direction, boolean starting) throws
     * IOException -
     * adds a wordID to wordIDs
     * 
     * String other(String wordID) -
     * return wordID/other wordID
     * 
     * boolean isOwner(String playerID) -
     * return true/false
     * 
     * void setOwnerOf(String playerID, String setWordID) -
     * set the owner from the wordID associate to this tile
     * 
     * void removeOwner(String playerID, String wordID) -
     * remove the owner from the wordID associate to this tile
     * 
     * String getOwnerOf(String wordID) -
     * return no owner/owner
     */
    @Test public void testConfirmTry() throws IOException {
        CrosswordCharacter cc = new CrosswordCharacter('c', 4, Direction.ACROSS, false);
        assertFalse(cc.startOfWord());
        cc.addWordID(3, Direction.DOWN, true);
        assertTrue(cc.startOfWord());
        assertTrue(cc.getID().equals("3"));
        cc.setOwnerOf("p1", "4ACROSS");
        assertTrue(cc.getOwnerOf("4ACROSS").equals("p1"));
        cc.setOwnerOf("p1", "3DOWN");
        assertTrue(cc.canTry("p1", "4ACROSS"));
        assertTrue(cc.canTry("p1", "3DOWN"));
        assertTrue(cc.getOwnerOf("3DOWN").equals("p1"));
        assertTrue(cc.isOwner("p1"));
        cc.setOwnerOf("p2", "3DOWN");
        assertFalse(cc.canTry("p1", "3DOWN"));
        assertTrue(cc.getOwnerOf("3DOWN").equals("p2"));
        assertFalse(cc.isOwner("p1"));
        assertFalse(cc.isOwner("p2"));
        cc.removeOwner("3DOWN");
        assertTrue(cc.canTry("p1", "4ACROSS"));
        assertTrue(cc.canTry("p1", "3DOWN"));
        cc.setConfirmed("4ACROSS");
        assertTrue(cc.isWordConfirmed("4ACROSS"));
        assertFalse(cc.isWordConfirmed("3DOWN"));
    }

    /*
     * char getChar() -
     * return char of the tile
     * 
     * void changeChar(char character) -
     * changes tile to character
     * 
     * boolean isEmpty() -
     * return true/false
     * 
     * boolean isBlack() -
     * return true/false
     */
    @Test public void testCharacter() {
        CrosswordCharacter cc = new CrosswordCharacter();
        assertTrue(cc.isBlack());
        cc.changeChar('c');
        assertTrue(cc.getChar() == 'c');
        assertFalse(cc.isBlack());
        assertFalse(cc.isEmpty());
        cc.changeChar('_');
        assertTrue(cc.isEmpty());
        cc.changeChar('C');
        assertTrue(cc.getChar() == 'C');
    }

    /*
     * boolean startOfWord() -
     * is/is not start tile of word
     * 
     * String getID() -
     * return id tile is a part of
     */
    @Test public void testStartingTiles() throws IOException {
        CrosswordCharacter cc = new CrosswordCharacter('c', 1, Direction.ACROSS, false);
        assertFalse(cc.startOfWord());
        cc.addWordID(2, Direction.DOWN, true);
        assertTrue(cc.startOfWord());
        assertTrue(cc.getID().equals("2"));

        CrosswordCharacter same = new CrosswordCharacter('c', 1, Direction.ACROSS, true);
        assertTrue(same.startOfWord());
        same.addWordID(1, Direction.DOWN, true);
        assertTrue(same.startOfWord());
        assertTrue(same.getID().equals("1"));
    }

}
