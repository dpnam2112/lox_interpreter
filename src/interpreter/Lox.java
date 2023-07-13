package interpreter;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.io.Console;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Lox {
	static boolean hadRuntimeError = false;
	static boolean hadSyntaxError = false;
	private static boolean console = false;

	static Interpreter interpreter = new Interpreter();
	static Resolver resolver = new Resolver(interpreter);
	
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			runFile(args[0]);	
		}
		else if (args.length == 0) {
			runPrompt();
		}
		else {
			System.out.println("Usage: java <source to the main class> <path to source code>");
			System.exit(1);
		}
		
	}
	
	public static void runPrompt() throws IOException {
		console = true;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.print(">> ");
			String input = in.readLine();
			
			try {
				run(input);	
			}
			finally {
				hadSyntaxError = false;
				hadRuntimeError = false;
			}
		}
	}

	public static boolean consoleMode() {
		return console;
	}
	
	
	public static void runFile(String path) throws IOException {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(path));
			String source = new String(bytes);
			run(source);
		}
		catch(IOException exp) {
			System.out.println("Cannot open the source code.");
		}
		
		if (hadSyntaxError) {
			System.exit(10);
		}
		else if (hadRuntimeError) {
			System.exit(11);
		}
	}

	
	public static void run(String source) {
		Scanner sc = new Scanner(source);
		sc.scanTokens();

		List<Token> tokens = sc.getTokens();
		if (hadSyntaxError) {
			return;
		}
		
		Parser parser = new Parser(tokens);
		List<Stmt> stmts = parser.parse();
		
		if (stmts == null || hadSyntaxError) {
			return;
		}
		
		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(stmts);
		
		if (stmts == null || hadSyntaxError) {
			return;
		}

		interpreter.interpret(stmts);
	}

	/* reportError: print syntax error to the console
	 * @message:	message of the error
	 * @posMsg:		message that points out the the token which is mistakenly typed 
	 * @lineNumber: line number where the error is encountered */
	public static void reportError(String message, String posMsg, int lineNumber) {
	    /* Print an error message when a mistake is encountered */
	    System.out.println("On line " +	 lineNumber + ", " + posMsg + ": " + message);
	}
	
	static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			reportError(message, "at end", token.line);
		} else {
			reportError(message, "at '" + token.lexeme + "'", token.line);		
		}
		
		hadSyntaxError = true;
	}
	
	static void runtimeError(RuntimeError error) {
		int lineNumber = error.token.line;
		System.out.println("On line " + lineNumber +
							", token '" + error.token.lexeme + 
							"': " + error.getMessage());
		hadRuntimeError = true;
	}
}
