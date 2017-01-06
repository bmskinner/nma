package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.CytoplasmDetectionPipeline;
import com.bmskinner.nuclear_morphology.analysis.detection.DetectionPipeline;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory.NucleusCreationException;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.gui.Roi;

/**
 * Detects neutrophils in H&E images by identifying the cytoplasm,
 * then searching for nuclei and lobes within the cytoplasm area.
 * @author ben
 * @aince 1.13.4
 *
 */
public class NeutrophilDetectionPipeline extends DetectionPipeline<ICell> {

	private final CytoplasmDetectionPipeline cyto;
	
	public NeutrophilDetectionPipeline(IDetectionOptions cytoOptions, 
			IDetectionOptions nucOptions, 
			File imageFile, 
			double prop) throws ImageImportException {
		
		super(nucOptions, imageFile, prop);
		
		cyto = new CytoplasmDetectionPipeline(cytoOptions, imageFile, prop);
	}

	@Override
	public List<ICell> findInImage() {
		
		List<ICell> result = new ArrayList<ICell>(0);
		List<ICytoplasm> cytoplasms = cyto.kuwaharaFilter()
				.flatten()
				.edgeDetect()
				.gapClose()
				.findInImage();
		
		
//		search for nuclei and lobes within cytoplasm
		
		for(ICytoplasm cy : cytoplasms){
			
			ICell cell = new DefaultCell(cy);
			
			result.add(cell);
			
		}
		
		return result;
	}

	@Override
	protected ICell makeComponent(Roi roi, int objectNumber) throws NucleusCreationException {
		// TODO Auto-generated method stub
		return null;
	}
	

}
