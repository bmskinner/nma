package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;

import ij.gui.Roi;

public class LobedNucleusDetectionPipeline  extends DetectionPipeline<Nucleus> {

	private final List<ICell> cells;
	private final NucleusFactory factory;
	
	public LobedNucleusDetectionPipeline(IDetectionOptions op, File imageFile, double prop, List<ICell> cells) throws ImageImportException {
		super(op, imageFile, prop);
		this.cells = cells;
		factory = new NucleusFactory(imageFile, NucleusType.NEUTROPHIL);
	}
	
	/**
	  * Save the region of the input image containing the nucleus
	  * Create a Nucleus from the Roi and add it to a new Cell 
	  *
	  * @param roi the ROI within the image
	  * @param nucleusNumber the count of the nuclei in the image
	  * @throws ComponentCreationException 
	  */
	protected Nucleus makeComponent(Roi roi, int nucleusNumber) 
			throws ComponentCreationException{
		
		  // measure the area, density etc within the nucleus
		StatsMap values   = measure(roi, ip);

		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		Rectangle bounds = roi.getBounds();

		int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		// create a Nucleus from the roi
		IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));

		
		
		Nucleus currentNucleus = factory.buildInstance(roi, 
				options.getChannel(),
				originalPosition, 
				centreOfMass);
		
		boolean present = false;
		for(ICell cell : cells){
			if (cell.getCytoplasm().containsOriginalPoint(centreOfMass)){
				present = true;
			}
		}
		
		if( ! present){
			throw new ComponentCreationException("Roi is not within a cell");
		}

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );

		fine("Offsetting CoM to point "+offsetCoM.toString());

		currentNucleus.moveCentreOfMass(offsetCoM);

		currentNucleus.setStatistic(NucleusStatistic.AREA,      values.get("Area"));
		currentNucleus.setStatistic(NucleusStatistic.MAX_FERET, values.get("Feret"));
		currentNucleus.setStatistic(NucleusStatistic.PERIMETER, values.get("Perim"));

		currentNucleus.setScale(options.getScale());

		currentNucleus.initialise(proportion);

		currentNucleus.findPointsAroundBorder();

		return currentNucleus;
	}  
}
