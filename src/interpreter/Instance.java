package interpreter;

import java.util.Map;
import java.util.HashMap;

public class Instance {
	private final NadClass nadClass;
	private final Map<String, Object> fields = new HashMap<>();

	public Instance(NadClass nadClass) {
		this.nadClass = nadClass;
	}
	
	public Object get(Token field) {
		if (fields.containsKey(field.lexeme)) {
			return fields.get(field.lexeme);
		}

		Function method = nadClass.findMethod(field.lexeme);
		if (method != null) {
			return method.bind(this);
		}

		throw new RuntimeError(field, "property'" + field.lexeme +"' does not exist.");
	}

	public void set(Token field, Object value) {
		fields.put(field.lexeme, value);
	}
}
