package crossword;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.mit.eecs.parserlib.ParseTree;
import edu.mit.eecs.parserlib.Parser;
import edu.mit.eecs.parserlib.UnableToParseException;

public class CrosswordParser {
    /**
     * Main method. Parses and then reprints an example expression.
     * 
     * @param args command line arguments, not used
     * @throws UnableToParseException if example expression can't be parsed
     */
    public static void main(final String[] args) throws UnableToParseException {
        final String input = ">> \"Simple Puzzle\" \"A trivial puzzle designed to show how puzzles work\" \n" + 
                "  (//hello \n cat, \"feline companion\", DOWN, 0,1 )\n" + 
                "  (mat, \"lounging place for feline companion\", ACROSS, 1, 0) //hello \n";
        System.out.println(input);
        final CrosswordFile crossword = CrosswordParser.parse(input);
        System.out.println(crossword);
    }
    
    // the nonterminals of the grammar
    private static enum CrosswordGrammar {
        NAME, DESCRIPTION, WORDNAME, CLUE, DIRECTION, ROW, COL, WHITESPACE, ENTRY, FILE, COMMENT
    }

    private static Parser<CrosswordGrammar> parser = makeParser();
    
    /**
     * Compile the grammar into a parser.
     * 
     * @return parser for the grammar
     * @throws RuntimeException if grammar file can't be read or has syntax errors
     */
    private static Parser<CrosswordGrammar> makeParser() {
        try {
            // read the grammar as a file, relative to the project root.
            final File grammarFile = new File("src/crossword/Crossword.g");
            return Parser.compile(grammarFile, CrosswordGrammar.FILE);
            
        // Parser.compile() throws two checked exceptions.
        // Translate these checked exceptions into unchecked RuntimeExceptions,
        // because these failures indicate internal bugs rather than client errors
        } catch (IOException e) {
            throw new RuntimeException("can't read the grammar file", e);
        } catch (UnableToParseException e) {
            throw new RuntimeException("the grammar has a syntax error", e);
            //throw e;
        }
    }

    /**
     * Parse a string into an expression.
     * 
     * @param string string to parse
     * @return Expression parsed from the string
     * @throws UnableToParseException if the string doesn't match the Expression grammar
     */
    public static CrosswordFile parse(final String string) throws UnableToParseException {
        // parse the example into a parse tree
        try {
            final ParseTree<CrosswordGrammar> parseTree = parser.parse(string);
    
            // display the parse tree in various ways, for debugging only
            //System.out.println("parse tree " + parseTree);
            //Visualizer.showInBrowser(parseTree);
    
            // make an AST from the parse tree
            final CrosswordFile crosswordFile = (CrosswordFile) makeAbstractSyntaxTree(parseTree);
            // System.out.println("AST " + expression);
            
            return crosswordFile;}
        catch(Exception e) {
            //throw new RuntimeException("the grammar has a syntax error", e);
            throw e;
        }
    }
    
    /**
     * Convert a parse tree into an abstract syntax tree.
     * 
     * @param parseTree constructed according to the grammar in Exression.g
     * @return abstract syntax tree corresponding to parseTree
     */
    private static Object makeAbstractSyntaxTree(final ParseTree<CrosswordGrammar> parseTree) {
        switch (parseTree.name()) {
            case FILE:
                {
                    final List<ParseTree<CrosswordGrammar>> children = parseTree.children();
                    String name = children.get(0).text();
                    String description = children.get(1).text();
                    List<Entry> entries = new ArrayList<Entry>();
                    for (int i = 2; i < children.size(); i++) { //skip over the topToBottom operator
                        Entry tempEntry = (Entry) makeAbstractSyntaxTree(children.get(i));
                        entries.add(tempEntry);
                    }
                    return new CrosswordFile(name, description, entries);
                }
            case ENTRY:
                {
                    final List<ParseTree<CrosswordGrammar>> children = parseTree.children();
                    String word = children.get(0).text();
                    String clue = children.get(1).text();
                    String direction = children.get(2).text();
                    int row = Integer.parseInt(children.get(3).text());
                    int col = Integer.parseInt(children.get(4).text());
                    if (direction.equals("ACROSS")) {
                        return new Entry(word, clue, Entry.Direction.ACROSS, row, col);
                    }
                    //if the direction is not across, it must be down
                    return new Entry(word, clue, Entry.Direction.DOWN, row, col);
                }
        default:
            throw new AssertionError("should never get here");
        }

    }
}
