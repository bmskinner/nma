package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
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
	private LobedNucleusDetectionPipeline nucl;
	
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
		
		

		
		for(ICytoplasm cy : cytoplasms){
			
			ICell cell = new DefaultCell(cy);
			
			result.add(cell);
			
		}
		
//		search for nuclei and lobes within cytoplasm
		try {
			nucl = new LobedNucleusDetectionPipeline(options, file, proportion, result);
		} catch (ImageImportException e) {
			warn("Cannot detect nuclei");
			stack("Cannot detect nuclei", e);
		}
		
		List<Nucleus> nuclei = nucl.kuwaharaFilter()
				.flatten()
				.edgeDetect()
				.gapClose()
				.findInImage();
		
		// Add nuclei to cells
		for(ICell cell : result){
	
			for(Nucleus n : nuclei){
				if( cell.getCytoplasm().containsOriginalPoint(n.getOriginalCentreOfMass())){
					cell.setNucleus(n);
				}
			}
			
		}
		
		return result;
	}

	@Override
	protected ICell makeComponent(Roi roi, int objectNumber) throws ComponentCreationException {
		// TODO Auto-generated method stub
		return null;
	}
	

}
