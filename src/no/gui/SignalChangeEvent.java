package no.gui;

import java.util.EventObject;

public class SignalChangeEvent extends EventObject{

	private static final long serialVersionUID = 1L;
	private String message;
	private String sourceName;

	/**
	 * Create an event from a source, with the given message
	 * @param source
	 * @param type
	 */
	public SignalChangeEvent( Object source, String message, String sourceName ) {
		super( source );
		this.message 	= message;
		this.sourceName = sourceName;
	}
	
	/**
	 * The type of event, or other message to carry
	 * @return
	 */
	public String type() {
		return message;
	}
	
	/**
	 * The name of the component that fired the event
	 * @return
	 */
	public String sourceName(){
		return this.sourceName;
	}

}

