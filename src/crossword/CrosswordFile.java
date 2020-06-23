package crossword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable class representing a crossword board loaded from a file
 */
public class CrosswordFile {
    /*
     * AF(name, description, entries) -> a crossword puzzle with name name and description description. entries contains all the words 
     *                                   and clues that make up the puzzle (i.e. entries[i].getWord() is the word at location 
     *                                   (entries[i].getRow(), entries[i].getColumn()) with direction entries[i].getDirection(), and 
     *                                   has hint entries[i].getClue() for all i in [0,entries.size()-1])
     *                                            
     * RI:
     *  - name is not null or empty
     *  - description is not null or empty
     *  - entries is not null
     *  
     * SRE:
     *  - all fields are private and final 
     *  - only method that take parameters is the constructor. The parameters are String which 
     *    are immutable and a list which is deep copied and then wrapped in an unmodifiable 
     *    list so the client can not change the Entry list
     *  - all returned values from methods are immutable
     *  
     *  Thread Safety:
     *   - immutable class
     */
    
    private final String name;
    private final String description;
    private final List<Entry> entries;
    
    /**
     * constructor to create a crossword puzzle
     * 
     * @param name  name of crossword puzzle
     * @param description  description of crossword puzzle
     * @param entries  list of entries in the crossword
     */
    public CrosswordFile(String name, String description, List<Entry> entries) {
        this.name = trimQuotationMarks(name);
        this.description = trimQuotationMarks(description);
        List<Entry> temp = new ArrayList<Entry>(entries);
        this.entries = Collections.unmodifiableList(temp);
        checkRep();
    }
    
    /**
     * asserts the RI written above is followed
     */
    public void checkRep() {
        assert name != null;
        assert !name.equals("");
        assert description != null;
        assert !description.equals("");
        assert entries != null;
    }
    
    /**
     * @return name of the crossword
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return description of the crossword
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @return list of Entry objects consisting of all entries in a crossword
     */
    public List<Entry> getEntries(){
        return entries;
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

}
