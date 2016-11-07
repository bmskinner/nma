package components.active.generic;

/**
 * Thrown when the requested border tag is not present in a Taggable object
 * @author bms41
 *
 */
public class UnavailableBorderTagException extends Exception {
	private static final long serialVersionUID = 1L;
	public UnavailableBorderTagException() { super(); }
	public UnavailableBorderTagException(String message) { super(message); }
	public UnavailableBorderTagException(String message, Throwable cause) { super(message, cause); }
	public UnavailableBorderTagException(Throwable cause) { super(cause); }
}
