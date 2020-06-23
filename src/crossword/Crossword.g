/*
 * crossword grammar 6.031 final project
 * TODO need to accept comments
 */

@skip whitespace {
    file ::= '>>' name description '\n' entry*; 
    //file ::= '\n' entry*;
    entry ::= '\n'* '('  wordname ','  clue ',' direction ',' row ',' col ')';
}

name ::= '"' [^"\r\n\t\\]* '"';
description ::= '"' ([^"\r\n\\] | '\\' [\\nrt] )* '"'; 
wordname ::= [a-z\-]+;
clue ::= '"' ([^"\r\n\\] | '\\' [\nrt] )* '"';
direction ::= 'DOWN' | 'ACROSS';
row ::= [0-9]+;
col ::= [0-9]+;
whitespace ::= ([ \t\r] | comment)+;
comment ::= '//' ([^\n])* '\n';