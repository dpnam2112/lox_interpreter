package interpreter;

public enum TokenType {
    /* Single-character tokens */

    DOT, SLASH, COMMA, SEMICOLON, 
    LEFT_PAREN, RIGHT_PAREN,		/* Parentheses, aka round braces */
    LEFT_BRACKET, RIGHT_BRACKET,
    PLUS, MINUS, STAR, NOT, COLON, QUESTION,
    MOD,

    /* Logical, comparison and assignment operators */
    AND, OR, 
    LT, GT, EQ, LT_EQ, GT_EQ, DIFF,
    ASGN, INC_ASGN, DEC_ASGN,

    /* Values and Identifiers */
    STR_LIT, NUM, IDENTIFIER,

    /* Keywords */
    FUNC, FOR, CLASS, VAR, WHILE,
    TRUE, FALSE, RETURN, SUPER, IF, ELSE, NIL, PRINT, THIS,
    BREAK, CONTINUE, STATIC,

    EOF,
}
