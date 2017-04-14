package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

public class LobeFactory implements ComponentFactory<Lobe> {
	
	private final File file;
	
	/**
	 * Create a factory for nuclei of the given type
	 * @param imageFile
	 * @param nucleusType
	 */
	public LobeFactory(File imageFile){
		
		if(imageFile==null){
			throw new IllegalArgumentException("File cannot be null in factory");
		}
		file = imageFile;

	}
	
	
	@Override
	public Lobe buildInstance(Roi roi, int channel,
			int[] originalPosition, IPoint centreOfMass)
			throws ComponentCreationException {
		
		Lobe result = new DefaultLobe(roi, centreOfMass, file, channel, originalPosition);
		return result;
	}

}
