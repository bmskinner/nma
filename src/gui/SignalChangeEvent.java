/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui;

import java.util.EventObject;

public class SignalChangeEvent extends EventObject {

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
	
	public SignalChangeEvent( SignalChangeEvent event ) {
		super( event.getSource() );
		this.message 	= event.type();
		this.sourceName = event.sourceName();
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

