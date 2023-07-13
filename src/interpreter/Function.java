package interpreter;
import java.util.List;

public class Function implements Callable {
	final Stmt.FuncStmt declaration;
	final Environment closure;
	final boolean isInit; // is a constructor/ an initializer? 

	public Function(Stmt.FuncStmt declaration, Environment closure) {
		this.closure = closure;
		this.declaration = declaration;
		isInit = false;
	}
	
	public Function(Stmt.FuncStmt declaration, Environment closure, boolean isInit) {
		this.closure = closure;
		this.declaration = declaration;
		this.isInit = isInit;
	}
	
	public int arity() {
		return declaration.argNames.size();
	}
	
	public Object call(Interpreter interpreter, List<Object> args) {
		/* Save the environment before calling */
		Environment beforeCall = interpreter.environment;
		
		/* Create a new stack frame for the function call */
		Environment frame = new Environment(closure);
		
		for (int i = 0; i < args.size(); i++) {
			Token argName = declaration.argNames.get(i);
			Object value = args.get(i);
			frame.define(argName, value);
		}
		
		Object returnValue = null;
		
		try {
			interpreter.executeBlock(declaration.body, frame);	
		}
		catch (ReturnValue valueWrapper) {
			returnValue = valueWrapper.value;
		}
		finally {
			interpreter.environment = beforeCall;
		}

		
		return returnValue;
	}

	/* @bind: return a new function that incorporates an instance's states to its closure */
	Function bind(Instance instance) {
		Environment surround = new Environment(closure);
		surround.define("this", instance);
		return new Function(declaration, surround, isInit);
	}
	
	public String toString() {
		return "<function " + declaration.name.lexeme + ">";
	}
}
