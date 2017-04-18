package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

/**
 * A default implementation of the IMutableLobeDetectionOptions interface.
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultLobeDetectionOptions
	extends DefaultNucleusDetectionOptions 
	implements IMutableLobeDetectionOptions {

	private static final long serialVersionUID = 1L;
	
	private double lobeFraction = 0.5;;
	
	public DefaultLobeDetectionOptions(File folder) {
		super(folder);
	}
	
	public DefaultLobeDetectionOptions(ILobeDetectionOptions template) {
		super(template);
		lobeFraction = template.getLobeDiameter();
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new DefaultLobeDetectionOptions(this);
	}

	@Override
	public void setLobeDiameter(double d) {
		lobeFraction = d;
		
	}

	@Override
	public double getLobeDiameter() {
		return lobeFraction;
	}

}
