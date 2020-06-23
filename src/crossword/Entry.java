package crossword;

/**
 * Entry is an immutable class that describes a word in a crossword puzzle.
 * This includes the word's location, direction, and clue.
 *
 */
public class Entry {
    /*
     * AF(wordname, clue, direction, row, col) -> represents an entry in a crossword puzzle that begins at spot (row,col) 
     *                                            and is in direction direction and is the word wordname. The clue is the hint
     *                                            that will be displayed to help users guess the word
     *                                            
     * RI:
     *  - wordname is not null or empty
     *  - clue is not null or empty
     *  - row and col >=0
     *  - direction is not null
     *  
     * SRE:
     *  - all fields are private and final
     *  - all parameters are immutable
     *  - all returned values from methods are immutable
     */
    
    public enum Direction{
        ACROSS, DOWN,
    }
    
    private final String wordname;
    private final String clue;
    private final Direction direction;
    private final int row;
    private final int col;
    
    /**
     * creates an entry in a crossword puzzle
     * @param wordname  word of entry
     * @param clue  clue for entry
     * @param direction  direction of entry
     * @param row  row of first element in the entry
     * @param col  column of first element in the entry
     */
    public Entry(String wordname, String clue, Direction direction, int row, int col) {
        this.wordname = wordname;
        this.clue = trimQuotationMarks(clue);
        this.direction = direction;
        this.row = row;
        this.col = col;
        checkRep();
    }
    
    /**
     * asserts the RI written above is followed
     */
    public void checkRep() {
        assert row >=0;
        assert col >=0;
        assert wordname != null;
        assert !wordname.equals("");
        assert clue != null;
        assert !clue.equals("");
        assert direction != null;
    }
    
    /**
     * @return word name for entry 
     */
    public String getWord() {
        return wordname;
    }
    /**
     * @return clue for entry's word 
     */
    public String getClue() {
        return clue;
    }
    
    /**
     * @return entry's direction 
     */
    public Direction getDirection() {
        return direction;
    }
    
    /**
     * @return row of first element of entry 
     */
    public int getRow() {
        return row;
    }
    
    /**
     * @return column of first element of entry 
     */
    public int getCol() {
        return col;
    }
    
    /**
     * @param input  string you are trying to remove quotation marks from beginning and end (if it has any)
     * @return  the substring in between the first and second quotation marks (if there is only one quotation mark, 
     * the substring is after the first quotation mark to the end, if there aere no quotation marks returns 
     * entire input string
     */
    private static String trimQuotationMarks(String input) {
        String output = input;
        if (output.indexOf("\"") >= 0){
            output = output.substring(output.indexOf("\"")+1);
        }
        if (output.indexOf("\"") >= 0){
            output = output.substring(0,output.indexOf("\""));
        }
        return output;
    }
    
    /**
     * @param that  object to compare this to
     * @return      true is both objects are Entry objects that start on the same spot (row and column), have the same word and the same hint
     */
    @Override
    public boolean equals(Object that) {
        checkRep();
        if (that instanceof Entry) {
            Entry entryThat = (Entry) that;
            return (entryThat.row == this.row && entryThat.col == this.col &&
                    entryThat.wordname.equals(this.wordname) && entryThat.clue.equals(this.clue) &&
                    entryThat.direction == this.direction);
        }
        return false;
    }

}
