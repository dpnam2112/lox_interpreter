package interpreter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.RuntimeException;

public class Parser {
	private static class ParseError extends RuntimeException {
		
	}

	private List<Token> tokens;
	private int current = 0;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}
	
	private Token scanToken() {
		if (endOfTokens()) {
			return tokens.get(current);
		}

		return tokens.get(current++);
	}
	
	private boolean endOfTokens() {
		return tokens.get(current).type == TokenType.EOF;
	}
	
	private Token getCurrToken() {
		return tokens.get(current);
	}
	
	private Token previous() {
		if (current == 0)
			return null;
		
		return tokens.get(current - 1);
	}
	
	private boolean scanTokenIfMatch(TokenType... tokenTypes) {
		for (TokenType type : tokenTypes) {
			if (getCurrToken().type == type) {
				if (type != TokenType.EOF)
					current++;
				return true;
			}
		}
		
		return false;
	}
	
	public List<Stmt> parse() {
		List<Stmt> stmts = new ArrayList<>();

		try {
			while (!endOfTokens()) {
				skipNop();
				Stmt stmt = declaration();
				
				if (stmt != null)
					stmts.add(stmt);
			}
		}
		catch (ParseError error) {
			/* Move to the next statements or expressions if any syntax error
			 * is encountered. */
			synchronize();
			return null;
		}
		
		return stmts;
	}

	/* skipNop: Skip 'No operation' statements
	 * E.g: <Statement 1>;;<statement 2>;
	 * In the above example, because there is no operation between two semicolons so we can skip
	 * them while parsing. */
	private void skipNop() {
		while (scanTokenIfMatch(TokenType.SEMICOLON));
	}

	private Stmt.Block block() {
		List<Stmt> statements = new ArrayList<>();

		while (getCurrToken().type != TokenType.RIGHT_BRACKET && !endOfTokens()) {
			skipNop();			/* Skip semicolons (no operation) */
			Stmt statement = declaration();
			statements.add(statement);
		}

		consume(TokenType.RIGHT_BRACKET, "Unclosed block.");
		Stmt.Block block = new Stmt.Block(statements);
		return block;
	}

	private Stmt statement() {
		if (scanTokenIfMatch(TokenType.SEMICOLON)) {
			return null;
		}

		if (scanTokenIfMatch(TokenType.PRINT)) {
			return printStmt();
		}
		
		if (scanTokenIfMatch(TokenType.IF)) {
			return conditional();
		}
		
		if (scanTokenIfMatch(TokenType.LEFT_BRACKET)) {
			return block();
		}
		
		if (scanTokenIfMatch(TokenType.WHILE)) {
			return whileStmt();
		}
		
		if (scanTokenIfMatch(TokenType.FOR)) {
			return forStmt();
		}
		
		if (scanTokenIfMatch(TokenType.RETURN)) {
			return returnStmt();
		}
		
		if (scanTokenIfMatch(TokenType.BREAK, TokenType.CONTINUE)) {
			return loopControl();
		}
		
		return expressionStmt();
	}
	
	private Stmt declaration() {
		try {
			if (scanTokenIfMatch(TokenType.VAR)) {
				return varDeclaration();
			}
			
			if (scanTokenIfMatch(TokenType.FUNC)) {
				return funcDeclaration();
			}
			
			if (scanTokenIfMatch(TokenType.CLASS)) {
				return classDeclaration();
			}

			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}
	
	/* varDeclaration: Parse the variable declaration statement
	 * The statement has the following syntax: var x = <expression>; */
	private Stmt varDeclaration() {
		Token identifier = consume(TokenType.IDENTIFIER, "Missing identifier after the keyword 'var'.");
		
		Expr initExpr = (scanTokenIfMatch(TokenType.ASGN)) ? scanExpr() : new Expr.Literal(null);
		consume(TokenType.SEMICOLON, "Missing a ';' after the operand.");
		
		return new Stmt.VarStmt(identifier, initExpr);
	}
	
	private Stmt.FuncStmt funcDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect function's name.");
		consume(TokenType.LEFT_PAREN, "Expect '(' after function's name.");
		
		List<Token> argNames = new ArrayList<>();
		
		if (getCurrToken().type != TokenType.RIGHT_PAREN) {
			do {
				if (argNames.size() >= 20) {
					throw error(getCurrToken(), "Cannot accept more than 20 arguments.");
				}
				
				Token argName = consume(TokenType.IDENTIFIER, "Expect an identifier.");
				argNames.add(argName);
			}
			while (scanTokenIfMatch(TokenType.COMMA));
		}
		
		consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
		consume(TokenType.LEFT_BRACKET, "Expect a function definition.");
		
		Stmt.Block body = block();
		
		return new Stmt.FuncStmt(name, body, argNames);
	}
	
	private Stmt classDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect class's name.");
		
		Expr.Variable spClass = null;
		if (scanTokenIfMatch(TokenType.LT)) {
			Token spClassName = consume(TokenType.IDENTIFIER, "Expect an identifier after '<'.");
			spClass = new Expr.Variable(spClassName);
		}
		
		consume(TokenType.LEFT_BRACKET, "Expect '{' after class's name.");
		
		List<Stmt.FuncStmt> methods = new ArrayList<>();
		
		while (getCurrToken().type != TokenType.RIGHT_BRACKET && !endOfTokens()) {
			methods.add(funcDeclaration());
		}
		
		consume(TokenType.RIGHT_BRACKET, "Expect '}' after class's definition.");
		return new Stmt.Class(name, methods, spClass);
	}
	
	/* expressionStmt: parse the expression statement 
	 * Statement's syntax: <expression> */
	private Stmt.Expression expressionStmt() {
		Expr expr = scanExpr();
		consume(TokenType.SEMICOLON, "Missing a ';' after the statement.");
		return new Stmt.Expression(expr);
	}
	
	private Stmt.Print printStmt() {
		Expr expr = scanExpr();
		consume(TokenType.SEMICOLON, "Missing a ';' after the statement.");
		return new Stmt.Print(expr);
	}
	
	private Stmt.Conditional conditional() {
		consume(TokenType.LEFT_PAREN, "Expect a '(' after 'if' keyword.");
		Expr expr = scanExpr();
		consume(TokenType.RIGHT_PAREN, "Expect a ') after the expression.");
		
		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		
		if (scanTokenIfMatch(TokenType.ELSE)) {
			elseBranch = statement();
		}
		
		return new Stmt.Conditional(expr, thenBranch, elseBranch);
	}
	
	private Stmt.While whileStmt() {
		consume(TokenType.LEFT_PAREN, "Expect a '(' after 'if' keyword.");
		Expr expr = scanExpr();
		consume(TokenType.RIGHT_PAREN, "Expect a ')' after the expression.");
		
		Stmt body = statement();
		
		return new Stmt.While(expr, body, null);
	}
	
	private Stmt.Block forStmt() {
		consume(TokenType.LEFT_PAREN, "Expect a '(' after 'for' keyword.");
		
		Stmt initializer = null;
		
		if (scanTokenIfMatch(TokenType.SEMICOLON)) {
			
		}
		else if (scanTokenIfMatch(TokenType.VAR)) {
			initializer = varDeclaration();
		}
		else {
			initializer = expressionStmt();
		}
		
		Expr condition = null;
		
		if (getCurrToken().type != TokenType.SEMICOLON) {
			condition = scanExpr();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after the loop condition.");
		
		Expr updateExpr = scanExpr();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after the loop initialization.");
		
		Stmt body = statement();
		
		return forLoopToWhile(initializer, condition, updateExpr, body);
	}
	
	
	/* forLoopToWhile: 'convert' a for-loop statement to while-loop style
	 * 
	 * a for-loop statement:
	 * for (initializer; condition; increment)
	 * 	body
	 * 
	 * Can be interpreted like this:
	 * {
	 * 		initializer;
	 * 		while (condition) {
	 * 			body
	 * 			increment expression (if exist)
	 * 		}
	 * }
	 *  */
	private Stmt.Block forLoopToWhile(Stmt init, Expr condition, Expr increment, Stmt body) {
		if (condition == null)
			condition = new Expr.Literal(true);

		Stmt.While whileLoop = new Stmt.While(condition, body, increment);
		return new Stmt.Block(Arrays.asList(init, whileLoop));
	}
	
	private Stmt.Return returnStmt() {
		Token keyword = previous();
		Expr expr;
		
		try {
			expr = scanExpr();
		}
		catch(ParseError error) {
			expr = null;
		}
		
		consume(TokenType.SEMICOLON, "Expect a ';' after the statement.");
		return new Stmt.Return(keyword, expr);
	}
	
	private Stmt loopControl() {
		Token keyword = previous();
		consume(TokenType.SEMICOLON, "Expect ';' after the statement.");
		return new Stmt.Jump(keyword);
	}
	
	private Expr scanExpr() {
		return comma();
	}
	
	/* comma: parse the comma expression 
	 * Syntax: <expression>, <expression>; */
	private Expr comma() {
		Expr commaExpr = assignment();
		
		while (scanTokenIfMatch(TokenType.COMMA)) {
			Token op = previous();
			Expr nextExpr = assignment();
			commaExpr = new Expr.Binary(commaExpr, nextExpr, op);
		}
		
		return commaExpr;
	}
	
	
	/* assignment: parse the assignment expression
	 * Assignment's syntax: (Identifier | object's field) = <Expresssion>
	 * This expression is right-associated. */
	private Expr assignment() {
		Expr left = ternary();

		if (scanTokenIfMatch(TokenType.ASGN, TokenType.INC_ASGN, TokenType.DEC_ASGN)) {
			if (!(left instanceof Expr.Variable
					|| left instanceof Expr.Get)) {
				throw error(previous(), "Invalid left-hand side in the assignment expression.");
			}

			Token op = previous();
			Expr asgnExpr = assignment();
			
			if (left instanceof Expr.Variable) {
				left = new Expr.Assign(((Expr.Variable)left).name, asgnExpr, op);				
			}
			else {
				left = constructSetExpr((Expr.Get) left, asgnExpr);
			}
		}

		return left;
	}
	
	private Expr constructSetExpr(Expr.Get left, Expr right) {
		Token field = left.field;
		Expr object = left.object;
		return new Expr.Set(object, field, right);
	}
	
	/* ternary: parse the ternary expression
	 * Syntax: <Expression> ? <Expression> : <Expression>
	 * */
	private Expr ternary() {
		Expr ternary = equality();

		while (scanTokenIfMatch(TokenType.QUESTION)) {
			Expr trueStmt = equality();
			consume(TokenType.COLON, "Missing a ':' after the operand.");
			Expr falseStmt = equality();
			ternary = new Expr.Ternary(ternary, trueStmt, falseStmt);
		}
		
		return ternary;
	}
	
	
	/* equality: parse the 'equality' expression
	 * Syntax: <Expression> (= | !=) <Expression> */
	private Expr equality() {
		Expr eqExpr = disjunction();
		
		while (scanTokenIfMatch(TokenType.EQ, TokenType.DIFF)) {
			Token op = previous();
			Expr nextComp = disjunction();
			eqExpr = new Expr.Binary(eqExpr, nextComp, op);
		}
		
		return eqExpr;
	}
	
	/* disjunction: parse the 'disjunction' expression
	 * Syntax: <Expression> || <Expression> */
	private Expr disjunction() {
		Expr orExpr = conjunction();
		
		while (scanTokenIfMatch(TokenType.OR)) {
			Token op = previous();
			Expr nextOperand = conjunction();
			orExpr = new Expr.Binary(orExpr, nextOperand, op);
		}
		
		return orExpr;
	}
	
	private Expr conjunction() {
		Expr andExpr = comparison();
		
		while (scanTokenIfMatch(TokenType.AND)) {
			Token op = previous();
			Expr nextOperand = comparison();
			andExpr = new Expr.Binary(andExpr, nextOperand, op);
		}
		
		return andExpr;
	}
	
	private Expr comparison() {
		Expr compExpr = term();
		
		while (scanTokenIfMatch(TokenType.LT, TokenType.GT, TokenType.LT_EQ, TokenType.GT_EQ)) {
			Token op = previous();
			Expr nextTerm = term();
			compExpr = new Expr.Binary(compExpr, nextTerm, op);
		}
		
		return compExpr;
	}
	
	private Expr term() {
		Expr term = factor();
		
		while (scanTokenIfMatch(TokenType.PLUS, TokenType.MINUS)) {
			Token op = previous();
			Expr nextFactor = factor();
			term = new Expr.Binary(term, nextFactor, op);
		}
		
		return term;
	}
	
	private Expr factor() {
		Expr factor = unary();
		
		while (scanTokenIfMatch(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
			Token op = previous();
			Expr nextUnary = unary();
			factor = new Expr.Binary(factor, nextUnary, op);
		}
		
		return factor;
	}
	
	private Expr unary() {
		if (!scanTokenIfMatch(TokenType.MINUS, TokenType.NOT)) {
			return call();
		}

		Token op = previous();
		Expr primaryExpr = call();

		return new Expr.Unary(op, primaryExpr);
	}
	
	private Expr call() {
		Expr callee = primary();
		
		while (true) {
			if (scanTokenIfMatch(TokenType.LEFT_PAREN)) {
				List<Expr> args = parseCallArgs();
				Token rightParen = consume(TokenType.RIGHT_PAREN, "Expect ')' after expression arguments.");
				callee = new Expr.Call(callee, rightParen, args);	
			}
			else if (scanTokenIfMatch(TokenType.DOT)) {
				Token property = consume(TokenType.IDENTIFIER, "Expect an identifier after '.'.");
				callee = new Expr.Get(callee, property);
			}
			else {
				break;
			}
		}
		
		return callee;
	}
	
	private List<Expr> parseCallArgs() {
		List<Expr> args = new ArrayList<>();
		
		/* Handle cases when there are no arguments */
		if (getCurrToken().type == TokenType.RIGHT_PAREN) {
			return args;
		}
		
		do
		{
			if (args.size() > 20) {
				error(getCurrToken(), "Function call can accept no more than 20 arguments.");
			}

			Expr arg = assignment();
			args.add(arg);
		}
		while (scanTokenIfMatch(TokenType.COMMA));
		
		return args;
	}
	
	private Expr primary() {
		if (scanTokenIfMatch(TokenType.TRUE))
			return new Expr.Literal(true);
		if (scanTokenIfMatch(TokenType.FALSE))
			return new Expr.Literal(false);
		if (scanTokenIfMatch(TokenType.NIL))
			return new Expr.Literal(null);
		if (scanTokenIfMatch(TokenType.NUM, TokenType.STR_LIT))
			return new Expr.Literal(previous().literal);
		if (scanTokenIfMatch(TokenType.LEFT_PAREN)) {
			Expr innerExpr = scanExpr();
			consume(TokenType.RIGHT_PAREN, "Expect token ')'.");
			return new Expr.Grouping(innerExpr);
		}
		if (scanTokenIfMatch(TokenType.THIS))
			return new Expr.This(previous());
		if (scanTokenIfMatch(TokenType.SUPER)) {
			Token superTk = previous();
			consume(TokenType.DOT, "Expect '.' after 'super' keyword.");
			Token method = consume(TokenType.IDENTIFIER, "Expect an identifier after '.'.");
			return new Expr.Super(superTk, method);
		}
		if (scanTokenIfMatch(TokenType.IDENTIFIER)) {
			Token identifier = previous();
			if (Scanner.reservedKeywords.containsKey(identifier.lexeme)) {
				error(identifier, "'" + identifier + "' is a reserved keyword.");
			}
			return new Expr.Variable(identifier);	
		}
		if (scanTokenIfMatch(TokenType.FUNC))
			return funcExpr();
		throw error(previous(), "Expect an expression.");
	}

	private Expr.Function funcExpr() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'func' keyword.");
		// Function arguments 
		List<Token> args = new ArrayList<>();
		if (getCurrToken().type != TokenType.RIGHT_PAREN) {
			do {
				if (scanTokenIfMatch(TokenType.IDENTIFIER)) {
					args.add(previous());
				}
				else {
					error(getCurrToken(), "Expect an identifier.");
				}
			}
			while (scanTokenIfMatch(TokenType.COMMA) && !endOfTokens());
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after parameter list.");
		consume(TokenType.LEFT_BRACKET, "Expect '{' after parameter declaration.");
		Stmt.Block body = block();
		return new Expr.Function(body, args);
	}

	/* consume: Check if the next token belongs to 'type', if it does
	 * not, throw an error.
	 *  */
	private Token consume(TokenType type, String msg) {
		if (getCurrToken().type == type) {
			return scanToken();
		}
		
		throw error(getCurrToken(), msg);
	}

	private ParseError error(Token token, String message) {
		/* Report error to the user */
		Lox.error(token, message);

		return new ParseError();
	}
	
	/* synchronize: move the scan pointer (current) to the next statement when
	 * encountering a syntax error */
	private void synchronize() {
	    scanToken();
	    
	    while (!endOfTokens()) {
	    	if (previous().type == TokenType.SEMICOLON) {
	    		return;
	    	}

		    switch (getCurrToken().type) {
			case CLASS:
			case WHILE:
			case FOR:
			case VAR:
			case IF:
			case RETURN:
			case PRINT:
			case FUNC:
				return;
		    }
		    
		    scanToken();
	    }
	}
}
