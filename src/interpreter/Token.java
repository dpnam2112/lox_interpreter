package interpreter;

public class Token {
	final TokenType type;
	final String lexeme;
	final Object literal;	    /* Literal value of the token (if it exists). E.g: 123, "abc",... */
	final int line;

	public Token(TokenType type, String lexeme, Object literal, int line) {
	    this.type = type;
	    this.lexeme = lexeme;
	    this.literal = literal;
	    this.line = line;
	}

	public String toString() {
	    return type + " " + lexeme + " " + literal;
	}
}
