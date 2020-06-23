package crossword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.mit.eecs.parserlib.UnableToParseException;

public class ParserTest {
    /*
     * Testing Partition:
     *  - only title and description (no entries)
     *  - single entry
     *  - multiple entries
     *  - title includes quotation mark, newline, or tab
     *  - incorrect commas in entry (incorrect format)
     *  - row or column is letter not number
     *  - varied spacing
     */
    
    // only contains title and description
    @Test
    public void testTitleAndDescription() {
        try {
            CrosswordFile file = CrosswordParser.parse(">> \"Test Puzzle\" \"A blank puzzle\"\n");
            assertEquals("Test Puzzle", file.getName(), "parser set incorrect name");
            assertEquals("A blank puzzle", file.getDescription(), "parser set incorrect description");
            assertEquals(0, file.getEntries().size(), "parser set incorrect entries");
        } catch (UnableToParseException e) {
            assertTrue(true, "threw error when parsing valid string");
        }
    }
    
    // single entry
    @Test
    public void testSingleEntry() {
        try {
            CrosswordFile file = CrosswordParser.parse(">> \"Test Puzzle\" \"A blank puzzle\"\n (max, \"6.031 Instructor\", DOWN, 0, 2)");
            assertEquals("Test Puzzle", file.getName(), "parser set incorrect name");
            assertEquals("A blank puzzle", file.getDescription(), "parser set incorrect description");
            assertEquals(1, file.getEntries().size(), "parser set incorrect entries");
            Entry tempEntry = new Entry("max", "6.031 Instructor", Entry.Direction.DOWN, 0, 2);
            assertTrue(tempEntry.equals(file.getEntries().get(0)), "incorrect entries");
        } catch (UnableToParseException e) {
            assertTrue(true, "threw error when parsing valid string");
        }
    }
    
    // multiple entries
    @Test
    public void testMultipleEntrie1() {
        try {
            CrosswordFile file = CrosswordParser.parse(">> \"Test Puzzle\" \"A puzzle with the 6.031 instructors\"\n (max, \"6.031 Instructor\", DOWN, 0, 2) "
                    + "(armando, \"other 6.031 Instructor\", ACROSS, 1, 2)");
            assertEquals("Test Puzzle", file.getName(), "parser set incorrect name");
            assertEquals("A puzzle with the 6.031 instructors", file.getDescription(), "parser set incorrect description");
            assertEquals(2, file.getEntries().size(), "parser set incorrect entries");
            Entry tempEntry1 = new Entry("max", "6.031 Instructor", Entry.Direction.DOWN, 0, 2);
            Entry tempEntry2 = new Entry("armando", "other 6.031 Instructor", Entry.Direction.ACROSS, 1, 2);
            assertTrue(tempEntry1.equals(file.getEntries().get(0)), "incorrect entries");
            assertTrue(tempEntry2.equals(file.getEntries().get(1)), "incorrect entries");
        } catch (UnableToParseException e) {
            assertTrue(true, "threw error when parsing valid string");
        }
    }
    
    // multiple entries
    @Test
    public void testMultipleEntrie2() {
        try {
            CrosswordFile file = CrosswordParser.parse(">> \"Test Puzzle\" \"A puzzle with the 6.031 instructors\"\n (max, \"6.031 Instructor\", ACROSS, 3, 2) "
                    + "(armando, \"other 6.031 Instructor\", DOWN, 3, 3)"
                    + "(tim, \"a beaver\", ACROSS, 5, 1)");
            assertEquals("Test Puzzle", file.getName(), "parser set incorrect name");
            assertEquals("A puzzle with the 6.031 instructors", file.getDescription(), "parser set incorrect description");
            assertEquals(3, file.getEntries().size(), "parser set incorrect entries");
            Entry tempEntry1 = new Entry("max", "6.031 Instructor", Entry.Direction.ACROSS, 3, 2);
            Entry tempEntry2 = new Entry("armando", "other 6.031 Instructor", Entry.Direction.DOWN, 3, 3);
            Entry tempEntry3 = new Entry("tim", "a beaver", Entry.Direction.ACROSS, 5, 1);
            assertTrue(tempEntry1.equals(file.getEntries().get(0)), "incorrect entries");
            assertTrue(tempEntry2.equals(file.getEntries().get(1)), "incorrect entries");
            assertTrue(tempEntry3.equals(file.getEntries().get(2)), "incorrect entries");
        } catch (UnableToParseException e) {
            assertTrue(true, "threw error when parsing valid string");
        }
    }
    
    // title contains quotation marks
    @Test
    public void testIncorrectTitleQuotationMark() {
        boolean threwError = false;
        try {
            CrosswordParser.parse(">> \"Bad\" Puzzle\" \"A puzzle with the 6.031 instructors\"\n (max, \"6.031 Instructor\", ACROSS, 3, 2) ");
        } catch (UnableToParseException e) {
            threwError = true;
        }
        assertTrue(threwError, "did not throw exception when input string couldnt be parsed");
    }
    
    // title contains tab marks
    @Test
    public void testIncorrectTitleTab() {
        boolean threwError = false;
        try {
            CrosswordParser.parse(">> \"Bad\t Puzzle\" \"A puzzle with the 6.031 instructors\"\n (max, \"6.031 Instructor\", ACROSS, 3, 2) ");
        } catch (UnableToParseException e) {
            threwError = true;
        }
        assertTrue(threwError, "did not throw exception when input string couldnt be parsed");
    }
    
    // entry contains incorrect commas
    @Test
    public void testIncorrectEntryFormat() {
        boolean threwError = false;
        try {
            CrosswordParser.parse(">> \"Bad\t Puzzle\" \"A puzzle with the 6.031 instructors\"\n (max \"6.031 Instructor\", ACROSS, 3 2) ");
        } catch (UnableToParseException e) {
            threwError = true;
        }
        assertTrue(threwError, "did not throw exception when input string couldnt be parsed");
    }
    
    // row is not a number
    @Test
    public void testIncorrectRowFormat() {
        boolean threwError = false;
        try {
            CrosswordParser.parse(">> \"Bad\t Puzzle\" \"A puzzle with the 6.031 instructors\"\n (max, \"6.031 Instructor\", ACROSS, T, 2) ");
        } catch (UnableToParseException e) {
            threwError = true;
        }
        assertTrue(threwError, "did not throw exception when input string couldnt be parsed");
    }
    
    // varied spacing
    @Test
    public void testDifferentSpacing() {
        try {
            CrosswordFile file = CrosswordParser.parse(">> \"Test Puzzle\" \"A puzzle with the 6.031 instructors\"\n   (  max  ,    \"6.031 Instructor\",     ACROSS,3,2) "
                    + "    (            armando           ,\"other 6.031 Instructor\",DOWN,3, 3          )"
                    + "(       tim,\"a beaver\",ACROSS,                   5         , 1           )");
            assertEquals("Test Puzzle", file.getName(), "parser set incorrect name");
            assertEquals("A puzzle with the 6.031 instructors", file.getDescription(), "parser set incorrect description");
            assertEquals(3, file.getEntries().size(), "parser set incorrect entries");
            Entry tempEntry1 = new Entry("max", "6.031 Instructor", Entry.Direction.ACROSS, 3, 2);
            Entry tempEntry2 = new Entry("armando", "other 6.031 Instructor", Entry.Direction.DOWN, 3, 3);
            Entry tempEntry3 = new Entry("tim", "a beaver", Entry.Direction.ACROSS, 5, 1);
            assertTrue(tempEntry1.equals(file.getEntries().get(0)), "incorrect entries");
            assertTrue(tempEntry2.equals(file.getEntries().get(1)), "incorrect entries");
            assertTrue(tempEntry3.equals(file.getEntries().get(2)), "incorrect entries");
        } catch (UnableToParseException e) {
            assertTrue(true, "threw error when parsing valid string");
        }
    }

}
