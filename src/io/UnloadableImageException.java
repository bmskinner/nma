package io;

/**
 * This exception gets thrown by components that try to access a source image file
 * that has become unavailable, and signals to other modules that they should not try
 * to process the source image
 * @author ben
 *
 */
public class UnloadableImageException extends Exception {
	private static final long serialVersionUID = 1L;
	public UnloadableImageException() { super(); }
	public UnloadableImageException(String message) { super(message); }
	public UnloadableImageException(String message, Throwable cause) { super(message, cause); }
	public UnloadableImageException(Throwable cause) { super(cause); }
}
