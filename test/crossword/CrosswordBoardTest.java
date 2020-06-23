/*
 * Copyright (c) 2018 MIT 6.031 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course
 * staff.
 */
package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import crossword.CrosswordBoard.Outcome;
import crossword.Entry.Direction;

public class CrosswordBoardTest {

    /*
     * Partitions -
     * CrosswordBoard(String filename)-
     * -filename contains a puzzle with all starting at different positions, two
     * words going in different directions both starting at the same position
     * -filename throw an throws IOException for a board that is invalid because:
     * --two words are overlapping in the same direction
     * --more than one word is going in the same direction and starting at the same
     * location
     * --two words going in different directions are overlapping and have different
     * characters
     * 
     * Outcome tryWord(String wordID, String wordEntered, String playerID)-
     * - wordID is/is not in the CrosswordBoard
     * - wordEntered is valid/invalid; invalid where:
     * -- word was already confirmed
     * -- same as word already there
     * -- word not of correct length
     * - return CONFIRMED, WORD_OWNED, CONFLICT, SUCCESS, WRONG_LENGTH, NONEXISTENT,
     * FINISHED
     * 
     * Outcome tryChallenge(String wordID, String newWord, String playerID)-
     * - wordID is/is not in the CrosswordBoard
     * - original word was correct/newWord is correct/neither correct
     * - wordEntered is valid/invalid; invalid where:
     * -- challenged word your own or no one's
     * -- challenged word was already confirmed
     * -- same as word already there
     * -- word not of correct length
     * - return CONFIRMED, SUCCESS, FAILED, WRONG_LENGTH, NONEXISTENT, SAME_WORD,
     * FINISHED, CANT_CHALLENGE
     * 
     * int showScore(String playerID)-
     * - return <0,0,>0
     * 
     * String toString()-
     * - 0,1,1+ spaces filled
     * 
     * List<List<CrosswordCharacter>> getPlayBoard()-
     * - test after 0, 1, 1+ turns
     * - makes sure that getPlayBoard is a deep copy of original rep
     * 
     * Map<String, String> getClues()- (never changes, so only need to test once)
     * - makes sure that getClues is a deep copy of original rep
     * 
     * String getName() - (never changes, so only need to test once)
     * - return the name of the puzzle from the file
     * 
     * String getDescription() - (never changes, so only need to test once)
     * - return the description of the puzzle from the file
     */

    @Test public void testAssertionsEnabled() {
        assertThrows(AssertionError.class, () -> {
            assert false;
        }, "make sure assertions are enabled with VM argument '-ea'");
    }

    /*
     * throws IOException for a board that is invalid because two
     * words going in different directions are overlapping and have different
     * characters
     */
    @Test public void testInconsistentBoard() {
        assertThrows(IOException.class, () -> {
            new CrosswordBoard("puzzles/inconsistent.puzzle");
        }, "should fail because two letters that overlap are not the same");
    }

    /*
     * throws IOException for a board that is invalid because more than one word
     * is going in the same direction and starting at the same location
     */
    @Test public void testSameStartSameDirection() {
        assertThrows(IOException.class, () -> {
            new CrosswordBoard("puzzles/sameStartSameDirection.puzzle");
        }, "should fail because two entries start at the same location with the same direction");
    }

    /*
     * List<List<CrosswordCharacter>> getPlayBoard() -
     * - test after 0, 1, 1+ turns
     * - makes sure that getPlayBoard is a deep copy of original rep
     * 
     * Map<String, String> getClues() -
     * - makes sure that getClues is a deep copy of original rep
     * 
     * String getName() -
     * - return the name of the puzzle from the file
     * 
     * String getDescription() -
     * - return the description of the puzzle from the file
     */
    @Test public void testPlayBoardCluesRep() throws IOException {
        CrosswordBoard okBoard = new CrosswordBoard("puzzles/test.puzzle");
        // check name and description of puzzle
        assertEquals("ANIMALS", okBoard.getName());
        assertEquals("One particular animal", okBoard.getDescription());
        // check that clues gets the right clues
        Map<String, String> getClues = Map.of("1DOWN", "winged mammal", "2ACROSS", "feline companion");
        for (String key : getClues.keySet()) {
            assertEquals(getClues.get(key), okBoard.getClues().get(key));
        }
        // makes sure that a change in the copy doesn't change rep in clues
        getClues = okBoard.getClues();
        getClues.replace("1DOWN", "animal of the night");
        assertEquals(getClues.keySet().size(), okBoard.getClues().size());
        assertEquals("winged mammal", okBoard.getClues().get("1DOWN"));

        // test play board initially
        List<List<CrosswordCharacter>> expectedPlayBoard = new ArrayList<>();
        expectedPlayBoard.add(List.of(new CrosswordCharacter(), new CrosswordCharacter('_', 1, Direction.DOWN, true),
                new CrosswordCharacter()));
        expectedPlayBoard.add(List.of(new CrosswordCharacter('_', 2, Direction.ACROSS, true),
                new CrosswordCharacter('_', 2, Direction.ACROSS, false),
                new CrosswordCharacter('_', 2, Direction.ACROSS, false)));
        expectedPlayBoard.add(List.of(new CrosswordCharacter(), new CrosswordCharacter('_', 1, Direction.DOWN, false),
                new CrosswordCharacter()));
        List<List<CrosswordCharacter>> getPlayBoard = okBoard.getPlayBoard();
        for (int i = 0; i < expectedPlayBoard.size(); i++) {
            for (int j = 0; j < expectedPlayBoard.get(0).size(); j++) {
                assertEquals(expectedPlayBoard.get(i).get(j).getChar(), getPlayBoard.get(i).get(j).getChar());
            }
        }
        // test play board after one turn
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1DOWN", "Cat", "p1"));
        expectedPlayBoard.clear();
        expectedPlayBoard.add(List.of(new CrosswordCharacter(), new CrosswordCharacter('c', 1, Direction.DOWN, true),
                new CrosswordCharacter()));
        expectedPlayBoard.add(List.of(new CrosswordCharacter('_', 2, Direction.ACROSS, true),
                new CrosswordCharacter('a', 2, Direction.ACROSS, false),
                new CrosswordCharacter('_', 2, Direction.ACROSS, false)));
        expectedPlayBoard.add(List.of(new CrosswordCharacter(), new CrosswordCharacter('t', 1, Direction.DOWN, false),
                new CrosswordCharacter()));
        getPlayBoard = okBoard.getPlayBoard();
        for (int i = 0; i < expectedPlayBoard.size(); i++) {
            for (int j = 0; j < expectedPlayBoard.get(0).size(); j++) {
                assertEquals(expectedPlayBoard.get(i).get(j).getChar(), getPlayBoard.get(i).get(j).getChar());
            }
        }
        // test play board after two turns
        assertEquals(Outcome.SUCCESS, okBoard.tryChallenge("1DOWN", "bat", "p2"));
        expectedPlayBoard.clear();
        expectedPlayBoard.add(List.of(new CrosswordCharacter(), new CrosswordCharacter('b', 1, Direction.DOWN, true),
                new CrosswordCharacter()));
        expectedPlayBoard.add(List.of(new CrosswordCharacter('_', 2, Direction.ACROSS, true),
                new CrosswordCharacter('a', 2, Direction.ACROSS, false),
                new CrosswordCharacter('_', 2, Direction.ACROSS, false)));
        expectedPlayBoard.add(List.of(new CrosswordCharacter(), new CrosswordCharacter('t', 1, Direction.DOWN, false),
                new CrosswordCharacter()));
        getPlayBoard = okBoard.getPlayBoard();
        for (int i = 0; i < expectedPlayBoard.size(); i++) {
            for (int j = 0; j < expectedPlayBoard.get(0).size(); j++) {
                assertEquals(expectedPlayBoard.get(i).get(j).getChar(), getPlayBoard.get(i).get(j).getChar());
            }
        }
        // makes sure that a change in the copy doesn't change rep in playBoard
        expectedPlayBoard = okBoard.getPlayBoard();
        expectedPlayBoard.get(0).set(1, new CrosswordCharacter('a', 1, Direction.DOWN, true));
        for (int i = 0; i < expectedPlayBoard.size(); i++) {
            for (int j = 0; j < expectedPlayBoard.get(0).size(); j++) {
                if (i == 0 && j == 1) {
                    assertFalse(expectedPlayBoard.get(i).get(j).getChar() == getPlayBoard.get(i).get(j).getChar());
                    continue;
                }
                assertEquals(expectedPlayBoard.get(i).get(j).getChar(), getPlayBoard.get(i).get(j).getChar());
            }
        }
    }

    /*
     * throws IOException for a board that is invalid because two words are
     * overlapping in the same direction
     */
    @Test public void testConflictOverlapping() {
        assertThrows(IOException.class, () -> {
            new CrosswordBoard("puzzles/badOverlap.puzzle");
        }, "should fail because two entries at a location with the same direction");
    }

    /*
     * tryWord-
     * - wordID is/is not in the CrosswordBoard
     * - wordEntered is valid/invalid
     * - return CONFLICT, SUCCESS, NONEXISTENT, WRONG_LENGTH, FINISHED
     * 
     * tryChallenge-
     * - wordID is in the CrosswordBoard
     * - newWord is valid
     * - return FAILED, FINISHED
     * 
     * showScore-
     * - return <0,0,>0
     * 
     * toString()-
     * - 0 places filled
     */
    @Test public void testSameStartingLocation() throws IOException {
        CrosswordBoard okBoard = new CrosswordBoard("puzzles/sameStartingLocation.puzzle");
        assertEquals(okBoard.toString(), "_ _ _ \n_     \n_     \n");
        // should not do anything
        assertEquals(Outcome.WRONG_LENGTH, okBoard.tryWord("1DOWN", "cater", "p1"));
        assertEquals(Outcome.NONEXISTENT, okBoard.tryWord("2DOWN", "cater", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1ACROSS", "Cat", "p1"));
        // conflicts with p1's cat
        assertEquals(Outcome.CONFLICT, okBoard.tryWord("1DOWN", "mOb", "p2"));
        // gets rid of cat
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1DOWN", "MaT", "p1"));
        // should get rid of mat
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1DOWN", "CaB", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1ACROSS", "Cab", "p1"));
        // lost challenge
        assertEquals(Outcome.FAILED, okBoard.tryChallenge("1ACROSS", "caT", "p2"));
        assertEquals(0, okBoard.showScore("p1"));
        assertEquals(-1, okBoard.showScore("p2"));
        // win challenge
        assertEquals(Outcome.FINISHED, okBoard.tryChallenge("1DOWN", "CAT", "p2"));
        assertEquals(1, okBoard.showScore("p1"));
        assertEquals(2, okBoard.showScore("p2"));
        assertEquals(Outcome.FINISHED, okBoard.tryChallenge("1DOWN", "CAT", "p2"));
        // finished game already so no more changes
        assertEquals(Outcome.FINISHED, okBoard.tryWord("1DOWN", "lot", "p1"));
        assertEquals(1, okBoard.showScore("p1"));
        assertEquals(2, okBoard.showScore("p2"));
    }

    /*
     * tryWord-
     * - wordID is in the CrosswordBoard
     * - wordEntered is valid/invalid
     * - return CONFIRMED, SUCCESS, FINISHED
     * 
     * tryChallenge-
     * - newWord is correct
     * - wordID is/is not in the CrosswordBoard
     * - newWord is valid/invalid
     * -- challenged word is no one's/player's
     * -- wrong length
     * -- nonexistent wordID
     * - return SUCCESS, CANT_CHALLENGE, WRONG_LENGTH, NONEXISTENT,FINISHED
     * 
     * showScore-
     * - return 0,>0
     * 
     * toString()-
     * - 0,1 places filled
     */
    @Test public void testSimple() throws IOException {
        CrosswordBoard okBoard = new CrosswordBoard("puzzles/test_duplicate.puzzle");
        assertEquals(okBoard.toString(), "  _   \n_ _ _ \n  _   \n");
        // no one inputted anything at "1down" yet
        assertEquals(Outcome.CANT_CHALLENGE, okBoard.tryChallenge("1DOwn", "cat", "p2"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1down", "cat", "p1"));
        assertEquals(okBoard.toString(), "  c   \n_ a _ \n  t   \n");
        // should get rid of "cat"
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("2Across", "for", "p1"));
        // should be fine
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1dowN", "lot", "p1"));
        // p1 is challenging own word
        assertEquals(Outcome.CANT_CHALLENGE, okBoard.tryChallenge("1down", "cat", "p1"));
        // wrong length
        assertEquals(Outcome.WRONG_LENGTH, okBoard.tryChallenge("1DOwn", "cater", "p2"));
        // word id doesn't exist
        assertEquals(Outcome.NONEXISTENT, okBoard.tryChallenge("10DOwn", "cat", "p2"));
        // should win challenge
        assertEquals(Outcome.SUCCESS, okBoard.tryChallenge("1DOwn", "cat", "p2"));
        assertEquals(0, okBoard.showScore("p1"));
        assertEquals(2, okBoard.showScore("p2"));
        // should not do anything because already confirmed
        assertEquals(Outcome.CONFIRMED, okBoard.tryWord("1down", "cat", "p1"));
        // should finish the game
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("2across", "fat", "p2"));
        assertEquals(Outcome.FINISHED, okBoard.tryChallenge("2ACroSS", "mat", "p1"));
        // both players won one challenge and each have one word under their playerID
        assertEquals(3, okBoard.showScore("p1"));
        assertEquals(3, okBoard.showScore("p2"));
    }

    /*
     * tryWord-
     * - wordID is in the CrosswordBoard
     * - wordEntered is valid/invalid
     * - return CONFIRMED, WORD_OWNED, SUCCESS, WRONG_LENGTH, CONFLICT
     * 
     * tryChallenge-
     * - newWord is correct/neither correct
     * - wordID is in the CrosswordBoard
     * - newWord is valid/invalid
     * -- already confirmed word
     * - return CONFIRMED, SUCCESS, FAILED
     * 
     * showScore-
     * - return 0,>0
     */
    @Test public void testBigBoard() throws IOException {
        CrosswordBoard okBoard = new CrosswordBoard("puzzles/simple.puzzle");
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("2down", "market", "p1"));
        // p1 already owns the word
        assertEquals(Outcome.WORD_OWNED, okBoard.tryWord("2down", "market", "p2"));
        // should clear market
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1across", "pore", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("5across", "sea", "p2"));
        // both words are wrong, so clear 5 across
        assertEquals(Outcome.FAILED, okBoard.tryChallenge("5across", "see", "p1"));
        assertEquals(-1, okBoard.showScore("p1"));
        assertEquals(0, okBoard.showScore("p2"));
        // longer than what is there
        assertEquals(Outcome.WRONG_LENGTH, okBoard.tryWord("6across", "pressures", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("6across", "pressure", "p2"));
        // cannot enter because conflicts with p2's words pressure and sea
        assertEquals(Outcome.CONFLICT, okBoard.tryWord("2down", "market", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("2down", "artamp", "p2"));
        // should clear all other words
        assertEquals(Outcome.SUCCESS, okBoard.tryChallenge("2down", "market", "p1"));
        assertEquals(1, okBoard.showScore("p1"));
        assertEquals(0, okBoard.showScore("p2"));
        // already confirmed
        assertEquals(Outcome.CONFIRMED, okBoard.tryChallenge("2down", "market", "p1"));
    }

    /*
     * tryWord-
     * - wordID is in the CrosswordBoard
     * - wordEntered is valid
     * - return WORD_OWNED, SUCCESS, FINISHED
     * 
     * tryChallenge-
     * - original word was correct
     * - wordID is in the CrosswordBoard
     * - newWord is valid
     * -- already confirmed word
     * -- same as word already there
     * - return SUCCESS, FAILED, CONFIRMED, SAME_WORD
     * 
     * showScore-
     * - return <0,>0
     * 
     * toString()-
     * - 0, 1, 1+ places filled
     */
    @Test public void testMultipleConfirmed() throws IOException {
        CrosswordBoard okBoard = new CrosswordBoard("puzzles/multipleConfirmed.puzzle");
        assertEquals("  _ _ _ \n_ _ _   \n  _ _ _ \n      _ \n      _ \n      _ \n", okBoard.toString());
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1across", "car", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("3across", "tax", "p1"));
        assertEquals("  c a r \n_ _ _   \n  t a x \n      _ \n      _ \n      _ \n", okBoard.toString());
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1down", "bob", "p1"));
        assertEquals("  b _ _ \n_ o _   \n  b _ _ \n      _ \n      _ \n      _ \n", okBoard.toString());

        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1across", "car", "p1"));
        assertEquals("  c a r \n_ _ _   \n  _ _ _ \n      _ \n      _ \n      _ \n", okBoard.toString());
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("2across", "mat", "p1"));
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("3across", "tax", "p1"));
        // same as word already there
        assertEquals(Outcome.SAME_WORD, okBoard.tryChallenge("1across", "car", "p2"));
        // originally correct
        assertEquals(Outcome.FAILED, okBoard.tryChallenge("1across", "cat", "p2"));
        // confirmed since was originally correct
        assertEquals(Outcome.CONFIRMED, okBoard.tryChallenge("1across", "cat", "p2"));
        assertEquals(Outcome.FAILED, okBoard.tryChallenge("2across", "sat", "p2"));
        assertEquals(Outcome.FAILED, okBoard.tryChallenge("3across", "tap", "p2"));
        // no one has claimed the id "1down" yet
        assertEquals(Outcome.SUCCESS, okBoard.tryWord("1down", "cat", "p2"));
        // p2 already claimed "1down"
        assertEquals(Outcome.WORD_OWNED, okBoard.tryWord("1down", "cat", "p1"));
        // p2 was originally correct
        assertEquals(Outcome.FAILED, okBoard.tryChallenge("1down", "car", "p1"));
        assertEquals(Outcome.FINISHED, okBoard.tryWord("4down", "xray", "p2"));
        // finished because of the previous move
        assertEquals(Outcome.FINISHED, okBoard.tryWord("4down", "xray", "p2"));
        assertEquals(2, okBoard.showScore("p1"));
        assertEquals(-1, okBoard.showScore("p2"));
    }
}
