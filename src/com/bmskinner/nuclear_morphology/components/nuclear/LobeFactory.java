package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * Factory for lobe construction
 * @author ben
 * @since 1.13.5
 *
 */
public class LobeFactory implements ComponentFactory<Lobe> {

	@Override
	public Lobe buildInstance(Roi roi, File imageFile, int channel,
			int[] originalPosition, IPoint centreOfMass)
			throws ComponentCreationException {
		
		return new DefaultLobe(roi, centreOfMass, imageFile, channel, originalPosition);
	}

}
