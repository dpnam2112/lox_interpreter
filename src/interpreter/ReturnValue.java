package interpreter;

public class ReturnValue extends RuntimeException {
	Object value;
	
	public ReturnValue(Object value) {
		super(null, null, false, false);
		this.value = value;
	}
}
