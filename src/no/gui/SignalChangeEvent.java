package no.gui;

import java.util.EventObject;

public class SignalChangeEvent extends EventObject{

	private static final long serialVersionUID = 1L;
	private String eventType;

	/**
	 * Create an event from a source, with the given message
	 * @param source
	 * @param type
	 */
	public SignalChangeEvent( Object source, String type ) {
		super( source );
		eventType = type;
	}
	
	/**
	 * The type of event, or other message to carry
	 * @return
	 */
	public String type() {
		return eventType;
	}

}

