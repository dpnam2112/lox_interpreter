package interpreter;
import java.util.List;

interface Callable {
	Object call(Interpreter interpreter, List<Object> args);
	int arity();
	String toString();
}
