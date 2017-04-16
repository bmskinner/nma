package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * The default implementation of the lobe interface.
 * @author ben
 * @since 1.13.5
 *
 */
public class DefaultLobe extends DefaultCellularComponent implements Lobe {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image.
	 * It sets the immutable original centre of mass, and the mutable current centre of mass. 
	 * It also assigns a random ID to the component.
	 * @param roi the roi of the object
	 * @param centerOfMass the original centre of mass of the component
	 * @param source the image file the component was found in
	 * @param channel the RGB channel the component was found in 
	 * @param position the bounding position of the component in the original image
	 */
	public DefaultLobe(Roi roi, IPoint centreOfMass, File source, int channel, int[] position) {
		super(roi, centreOfMass, source, channel, position);
	}

	protected DefaultLobe(Lobe l) {
		super(l);

	}

	@Override
	public CellularComponent duplicate() {
		return new DefaultLobe(this);
	}

	@Override
	public void alignVertically() {
		// TODO Auto-generated method stub
		
	}

}
