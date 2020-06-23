package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import crossword.ServerResponse.State;

public class ServerResponseTest {
    /**
     * Partitions
     *      Each of the states: START, CHOOSE, WAIT, PLAY, SCORE
     *      Has message: Y/N
     */
    
    //Start tests
    @Test
    public void testSTART() {
        ServerResponse noMess = ServerResponse.createStart();
        assertFalse(noMess.hasMessage());
        assertEquals(State.START, noMess.state());
        assertThrows(NoSuchFieldError.class, () -> {noMess.message();});
        assertThrows(NoSuchFieldError.class, () -> {noMess.availGames();});
        ServerResponse mess = ServerResponse.createStart("message");
        assertEquals("message", mess.message());
        assertEquals(State.START, mess.state());
    }
    
    //CHOOSE tests
    @Test
    public void testCHOOSE() {
        ServerResponse noMess = ServerResponse.createChoose(List.of("g1", "g2", "g3"));
        assertEquals(State.CHOOSE, noMess.state());
        assertFalse(noMess.hasMessage());
        assertThrows(NoSuchFieldError.class, () -> {noMess.message();});
        assertThrows(NoSuchFieldError.class, () -> {noMess.charBoard();});
        assertEquals(List.of("g1", "g2", "g3"), noMess.availGames());
        
        ServerResponse mess = ServerResponse.createChoose("t", List.of("g1", "g2", "g3"));
        assertEquals("t", mess.message());
        assertEquals(List.of("g1", "g2", "g3"), mess.availGames());
        assertEquals(State.CHOOSE, mess.state());
    }
    
    //WAIT tests
    @Test
    public void testWAIT() {
        ServerResponse noMess = ServerResponse.createWait();
        assertEquals(State.WAIT, noMess.state());
        assertFalse(noMess.hasMessage());
        assertThrows(NoSuchFieldError.class, () -> {noMess.message();});
        assertThrows(NoSuchFieldError.class, () -> {noMess.clues();});
        
        ServerResponse mess = ServerResponse.createWait("mess");
        assertEquals(State.WAIT, noMess.state());
        assertEquals("mess", mess.message());
    }
    
    //SCORE tests
    @Test
    public void testSCORE() {
        ServerResponse noMess = ServerResponse.createScore(List.of("s1", "s2"));
        assertEquals(State.SCORE, noMess.state());
        assertEquals(List.of("s1", "s2"), noMess.scores());
        assertFalse(noMess.hasMessage());
        assertThrows(NoSuchFieldError.class, () -> {noMess.message();});
        
        ServerResponse mess = ServerResponse.createScore("mess", List.of("s1", "s2"));
        assertEquals(State.SCORE, mess.state());
        assertEquals(List.of("s1", "s2"), mess.scores());
        assertEquals("mess", mess.message());
    }
}
