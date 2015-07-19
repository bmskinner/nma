package no.gui;

import java.util.EventObject;

public class SignalChangeEvent extends EventObject{

	private static final long serialVersionUID = 1L;
	private String eventType;

	public SignalChangeEvent( Object source, String type ) {
		super( source );
		eventType = type;
	}
	public String type() {
		return eventType;
	}

}

