package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.util.EventObject;

public class OptionsChangeEvent extends EventObject {
	
	public static final int KUWAHARA     = 0;
	public static final int EDGE         = 1;
	public static final int CHROMOCENTRE = 2;
	public static final int GAP_CLOSING  = 3;
	
	
	private static final long serialVersionUID = 1L;
//	private ImageSet set; // the image set to work with
//	private int type; // the image that is affected by the change
	
	/**
	 * Create an event from a source, with the given message
	 * @param source the source of the event
	 * @param set the image set
	 * @param type the image type affected
	 */
	public OptionsChangeEvent( Object source) {
		// , ImageSet set, int type 
		super( source );
//		this.set = set;
//		this.type = type;
	}
	
//	public ImageSet getImageSet() {
//		return set;
//	}
//
//	public int getType() {
//		return type;
//	}
	
	
}
