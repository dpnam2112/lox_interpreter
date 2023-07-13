package interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Scanner {
	private String source;					    /* Source code represented as a String */
	private final List<Token> tokens = new ArrayList<>();	    /* Scanned tokens */
	private int line = 1;					    /* Current line number */
	private int start = 0;					    /* Starting index of the lexeme */
	private int current = 0;				    /* Index of the current character */
	static final HashMap<String, TokenType> reservedKeywords;
	
	static {
	    reservedKeywords = new HashMap<>();

	    reservedKeywords.put("var", TokenType.VAR);
	    reservedKeywords.put("func", TokenType.FUNC);
	    reservedKeywords.put("for", TokenType.FOR);
	    reservedKeywords.put("while", TokenType.WHILE);
	    reservedKeywords.put("class", TokenType.CLASS);
	    reservedKeywords.put("super", TokenType.SUPER);
	    reservedKeywords.put("return", TokenType.RETURN);
	    reservedKeywords.put("false", TokenType.FALSE);
	    reservedKeywords.put("true", TokenType.TRUE);
	    reservedKeywords.put("nil", TokenType.NIL);
	    reservedKeywords.put("if", TokenType.IF);
	    reservedKeywords.put("else", TokenType.ELSE);
	    reservedKeywords.put("this", TokenType.THIS);
	    reservedKeywords.put("print", TokenType.PRINT);	
	    reservedKeywords.put("break", TokenType.BREAK);
	    reservedKeywords.put("continue", TokenType.CONTINUE);
	}

	public Scanner(String source) {
	    this.source = source;
	}

	public void scanTokens() {
	    while (!endOfSource()) {
		start = current;
		scanToken();
	    }
	    
	    tokens.add(new Token(TokenType.EOF, "", null, line));
	}

	/* @scanToken(): Scan a token per time in the source
	 * Precondition: start == current
	 * Postcondition, `start` and `current` are at the starting and ending positions of the
	 * token, respectively. For example, token `for` is encountered, start = 0, then after
	 * scanning, start = 0, current = 3. 
	 * If a multiple-line or a single-line comment is encountered, we treat a comment as a token.
	 * */
	private void scanToken() {
	    char c = scanChar();

	    switch (c) {
	    case '?':
	    	addToken(TokenType.QUESTION);
	    	break;
	    case '%':
	    	addToken(TokenType.MOD);
	    	break;
	    case ':':
	    	addToken(TokenType.COLON);
	    	break;
		case '(':
		    addToken(TokenType.LEFT_PAREN);
		    break;
		case ')':
		    addToken(TokenType.RIGHT_PAREN);
		    break;
		case '{':
		    addToken(TokenType.LEFT_BRACKET);
		    break;
		case '}':
		    addToken(TokenType.RIGHT_BRACKET);
		    break;
		case '+':
		    addToken(scanCharIfMatch('=') ? TokenType.INC_ASGN : TokenType.PLUS);
		    break;
		case '-':
		    addToken(scanCharIfMatch('=') ? TokenType.DEC_ASGN : TokenType.MINUS);
		    break;
		case '*':
		    addToken(TokenType.STAR);
		    break;
		case '.':
		    addToken(TokenType.DOT);
		    break;
		case ';':
		    addToken(TokenType.SEMICOLON);
		    break;
		case ',':
		    addToken(TokenType.COMMA);
		    break;
		case '!':
		    addToken(scanCharIfMatch('=') ? TokenType.DIFF : TokenType.NOT);
		    break;
		case '=':
		    addToken(scanCharIfMatch('=') ? TokenType.EQ : TokenType.ASGN);
		    break;
		case '<':
		    addToken(scanCharIfMatch('=') ? TokenType.LT_EQ : TokenType.LT);
		    break;
		case '>':
		    addToken(scanCharIfMatch('=') ? TokenType.GT_EQ : TokenType.GT);
		    break;
		case '/':
		    if (scanCharIfMatch('/')) {
			/* Handle single-line comments */
			while (getCurrentChar() != '\0' && getCurrentChar() != '\n')
			    scanChar();
		    }
		    else if (scanCharIfMatch('*')) {
			/* Handle multiple-line comments */
			while (getNextChar() != '\0') {
			    if (getCurrentChar() == '*' && getNextChar() == '/') {
				break;
			    }

			    if (getCurrentChar() == '\n')
				line++;

			    scanChar();
			}

			if (getNextChar() == '\0') {
			    Lox.reportError("Unclosed multiple-line comment", "at EOF", current);
			}
			else {
			    /* Scan star and forward-slash */
			    scanChar();
			    scanChar();
			}
		    }
		    else {
			addToken(TokenType.SLASH);
			break;
		    }
		/* Insignificant characters */
		case '\r':
		case ' ':
		case '\t':
		    break;
		case '\n':
		    line++;
		    break;
		case '\"':
		    scanStringLit();
		    break;
		case '&':
		    if (scanCharIfMatch('&'))
			addToken(TokenType.AND);
		    break;
		case '|':
		    if (scanCharIfMatch('|'))
			addToken(TokenType.OR);
		    break;
		default:
		    /* Handle cases where the current character is a number */
		    if (isDigit(c)) {
			scanNumber();
		    }
		    else if (isAlpha(c)) {
			/* We also check whether a token iis a reserved keyword in
			 * this method (e.g: func, var, .etc) */
			scanIdentifier();
		    }
		    else {
		    	Lox.reportError("Invalid character", "", line);
		    }
	    }

	}

	private boolean endOfSource() {
	    return current >= source.length();
	}

	
	public List<Token> getTokens() {
	    return tokens;
	}

	private void addToken(TokenType type) {
	    addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) {
	    String lexeme = source.substring(start, current);
	    tokens.add(new Token(type, lexeme, literal, line));
	}

	private char getCurrentChar() {
	    if (endOfSource())
		return '\0';
	    return source.charAt(current);
	}

	private char getNextChar() {
	    if (current + 1 >= source.length())
		return '\0';
	    return source.charAt(current + 1);
	}

	/* @scanStringLit(): Scan string literal
	 * Precondition: the starting character is a double quote
	 * */
	private void scanStringLit() {
	    while (getCurrentChar() != '\"' && !endOfSource()) {
		if (getCurrentChar() == '\n')
		    line++;
		scanChar();
	    }

	    if (endOfSource()) {
		Lox.reportError("Unterminated string", "", line);
	    }

	    String strLit = source.substring(start + 1, current);

	    scanChar();
	    addToken(TokenType.STR_LIT, strLit);
	}

	/* @scanIdentifier(): Scan identifiers or reserved tokens
	 * We check whether a token is an identifier or a reserved token in this method
	 * Precondition: the starting character is an underscore or a valid letter (a, A, b, B,...)
	 * */
	private void scanIdentifier() {
	    while (isDigit(getCurrentChar()) || isAlpha(getCurrentChar())) {
		scanChar();
	    }

	    TokenType type = reservedKeywords.get(source.substring(start, current));
	    
	    if (type == null)
		type = TokenType.IDENTIFIER;
	    
	    if (type == TokenType.TRUE) {
	    	addToken(type, true);
	    	return;
	    }
	    else if (type == TokenType.FALSE) {
	    	addToken(type, false);
	    	return;
	    }

	    addToken(type);
	}
	/* @scanChar(): scan the current character */
	private char scanChar() {
	    return source.charAt(current++);
	}

	/* scanCharIfMatch(char): Scan the current character if it matches `c` */
	private boolean scanCharIfMatch(char c) {
	    if (getCurrentChar() != c)
		return false;
	    current++;
	    return true;
	}

	/* @isDigit(): Check whether the character is a digit: 0, 1, 2,..., 9 */
	private static boolean isDigit(char c) {
	    return (c >= '0' && c <= '9');
	}

	/* @isAlpha(): Check whether @c is an alphabetical letter
	 * For simplicity, we consider underscore as a valid alphabetical letter
	 * */
	private static boolean isAlpha(char c) {
	    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}

	/* @scanNumber(): Scan a number: integer, double, float
	 * Precondition: The character at index 'start' is a digit
	 * */
	private void scanNumber() {
	    while (isDigit(getCurrentChar()))
		scanChar();

	    if (getCurrentChar() == '.' && isDigit(getNextChar())) {
		scanChar();	//Scan 'dot' character

		/* Scan fractional part */
		while (isDigit(getCurrentChar()))
		    scanChar();
	    }

	    Double value = Double.parseDouble(source.substring(start, current));
	    addToken(TokenType.NUM, value);
	}
}
