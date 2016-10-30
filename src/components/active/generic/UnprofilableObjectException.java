package components.active.generic;

public class UnprofilableObjectException extends Exception {
	private static final long serialVersionUID = 1L;
	public UnprofilableObjectException() { super(); }
	public UnprofilableObjectException(String message) { super(message); }
	public UnprofilableObjectException(String message, Throwable cause) { super(message, cause); }
	public UnprofilableObjectException(Throwable cause) { super(cause); }
}
