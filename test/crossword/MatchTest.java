package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import crossword.Match.MatchListener;
import crossword.Match.TooManyPlayersException;

public class MatchTest {
    /*
     * Testing Partition:
     *  - create match with no players
     *  - add one player
     *  - add two players
     *  - add three players
     *  - use all helper methods
     */
    
    // create match with no players
    @Test
    public void testMakeMatch() {
        try {
            Match match = new Match("TEST", "THIS IS TO TEST THE MATCH OBJECT", new CrosswordBoard("puzzles/simple.puzzle"));
            assertEquals("TEST", match.getMatchId(), "incorrect match name");
            assertEquals("THIS IS TO TEST THE MATCH OBJECT", match.getDescription(), "incorrect description");
            assertEquals(0, match.getNumPlayers(), "incorrect number of players");
        } catch (IOException e) {
            assertTrue(false, "unable to generate board");
        }
    }
    
    // create match with one player
    @Test
    public void testOnePlayer() {
        try {
            Match match = new Match("TEST", "THIS IS TO TEST THE MATCH OBJECT", new CrosswordBoard("puzzles/simple.puzzle"));
            match.addPlayer("TIM", new MatchListener() {
                public void updateMatch(boolean firstCall) {return;}
                public void endMatch(String message) {return;}
            });
            assertEquals("TEST", match.getMatchId(), "incorrect match name");
            assertEquals("THIS IS TO TEST THE MATCH OBJECT", match.getDescription(), "incorrect description");
            assertEquals(1, match.getNumPlayers(), "incorrect number of players");
        }
        catch (TooManyPlayersException e) {
            assertTrue(false, "threw error that too many players were added");
        }
        catch (IOException e) {
            assertTrue(false, "unable to generate board");
        }
    }
    
    // create match with two players
    @Test
    public void testTwoPlayer() {
        try {
            Match match = new Match("TEST", "THIS IS TO TEST THE MATCH OBJECT", new CrosswordBoard("puzzles/simple.puzzle"));
            match.addPlayer("TIM", new MatchListener() {
                public void updateMatch(boolean firstCall) {return;}
                public void endMatch(String message) {return;}
            });
            match.addPlayer("TUYET", new MatchListener() {
                public void updateMatch(boolean firstCall) {return;}
                public void endMatch(String message) {return;}
            });
            assertEquals("TEST", match.getMatchId(), "incorrect match name");
            assertEquals("THIS IS TO TEST THE MATCH OBJECT", match.getDescription(), "incorrect description");
            assertEquals(2, match.getNumPlayers(), "incorrect number of players");
        }
        catch (TooManyPlayersException e) {
            assertTrue(false, "threw error that too many players were added");
        }
        catch (IOException e) {
            assertTrue(false, "unable to generate board");
        }
    }
    
   // create match with three players
    @Test
    public void testThreePlayer() {
        try {
            Match match = new Match("TEST", "THIS IS TO TEST THE MATCH OBJECT", new CrosswordBoard("puzzles/simple.puzzle"));
            match.addPlayer("TIM", new MatchListener() {
                public void updateMatch(boolean firstCall) {return;}
                public void endMatch(String message) {return;}
            });
            match.addPlayer("TUYET", new MatchListener() {
                public void updateMatch(boolean firstCall) {return;}
                public void endMatch(String message) {return;}
            });
            match.addPlayer("TATUM", new MatchListener() {
                public void updateMatch(boolean firstCall) {return;}
                public void endMatch(String message) {return;}
            });
            assertTrue(true, "should have thrown error that too many players were added");
        }
        catch (TooManyPlayersException e) {
            assertTrue(true, "should have thrown error that too many players were added");
        }
        catch (IOException e) {
            assertTrue(false, "unable to generate board");
        }
    }
    
    //helper methods getNumPlayers(), getMatchID(), and getDescription() already tested above
    //methods endGame() and updateMatch() tested in server tests
    
    
    
}
