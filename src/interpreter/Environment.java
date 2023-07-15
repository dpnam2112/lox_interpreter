package interpreter;
import java.util.Map;
import java.util.HashMap;


public class Environment {
	final Environment outerEnv;
	final Map<String, Object> values = new HashMap<>();
	
	public Environment() {
		outerEnv = null;
	}

	public Environment(Environment outerEnv) {
		this.outerEnv = outerEnv;
	}
	
	public void define(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			Lox.hadRuntimeError = true;
			throw new RuntimeError(name, "Redeclare existing variable: \"" + name.lexeme + "\".");
		}

		values.put(name.lexeme, value);
	}
	
	public void define(String name, Object value) {
		values.put(name, value);
	}
	
	public Object get(Token name) {
		if (!values.containsKey(name.lexeme)) {
			if (outerEnv == null) {
				Lox.hadRuntimeError = true;
				throw new RuntimeError(name, "Dereference an undefined variable.");
			}

			return outerEnv.get(name);
		}
		
		return values.get(name.lexeme);
	}

	public void assign(Token name, Object value) {
		if (!values.containsKey(name.lexeme)) {
			if (outerEnv == null) {
				Lox.hadRuntimeError = true;
				throw new RuntimeError(name, "Assign value to an undefined variable.");
			}

			outerEnv.assign(name, value);
			return;
		}

		values.put(name.lexeme, value);
	}
	
	public Object getAt(int depth, Token name) {
		Object value = this.ancestor(depth).values.get(name.lexeme);
		if (value == null)
			throw new RuntimeError(name, "Undefined identifier.");
		return value;
	}
	
	public Object getAt(int depth, String name) {
		Object value = this.ancestor(depth).values.get(name);
		if (value == null)
			return null;
		return value;
	}
	
	public void assignAt(int depth, Token name, Object value) {
		Environment ancestor = this.ancestor(depth);
		ancestor.assign(name, value);
	}
	
	public Environment ancestor(int distance) {
		Environment ancestorEnv = this;
		for (int i = 0; i < distance; i++) {
			ancestorEnv = ancestorEnv.outerEnv;
		}
		return ancestorEnv;
	}
}
