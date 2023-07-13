package interpreter;

import java.util.List;
import java.util.Map;

public class NadClass implements Callable {
	final String name;
	final Map<String, Function> methods;
	
	NadClass(String name, Map<String, Function> methods) {
		this.name = name;
		this.methods = methods;
	}
	
	public String toString() {
		return "<class '" + name + "'>";
	}
	
	public Object call(Interpreter interpreter, List<Object> args) {
		Instance instance = new Instance(this);
		Function constructor = findMethod("init");
		if (constructor != null) {
			constructor.bind(instance).call(interpreter, args);
		}
		return instance;
	}
	
	public int arity() {
		Function constructor = findMethod("init");
		return (constructor == null) ? 0 : constructor.arity();
	}

	Function findMethod(String name) {
		return methods.get(name);
	}
}
