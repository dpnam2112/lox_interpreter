package interpreter;

public class LoopException extends RuntimeException {
	Token token;
	
	LoopException(Token token) {
		super(null, null, false, false);
		this.token = token;
	}
}
