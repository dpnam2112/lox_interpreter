package interpreter;

import java.util.List;
import java.util.Map;

public class LoxClass implements Callable {
	final String name;
	final Map<String, Function> methods;
	final LoxClass superclass;
	
	LoxClass(String name, Map<String, Function> methods, LoxClass superclass) {
		this.name = name;
		this.methods = methods;
		this.superclass = superclass;
	}
	
	public String toString() {
		return "<class '" + name + "'>";
	}
	
	public Object call(Interpreter interpreter, List<Object> args) {
		Instance instance = new Instance(this);
		
		// Call the constructor
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
		if (!methods.containsKey(name) && superclass != null)
			return superclass.findMethod(name);
		return methods.get(name);
	}
}
