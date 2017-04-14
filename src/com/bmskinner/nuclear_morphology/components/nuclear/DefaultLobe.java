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
