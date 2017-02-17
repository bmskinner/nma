package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Rectangle;
import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;

import ij.gui.Roi;

/**
 * The default pipline for detecting single nuclei in a fluorescence image.
 * @author ben
 * @since 1.13.4
 *
 */
public class FluoresentNucleusDetectionPipeline extends DetectionPipeline<ICell> {
	
	private final NucleusFactory factory;
	
	public FluoresentNucleusDetectionPipeline(IDetectionOptions op, File imageFile, NucleusType t, double prop) throws ImageImportException {
		super(op, imageFile, prop);
		factory = new NucleusFactory(imageFile, t);
	}
		
	/**
	  * Save the region of the input image containing the nucleus
	  * Create a Nucleus from the Roi and add it to a new Cell 
	  *
	  * @param roi the ROI within the image
	  * @param nucleusNumber the count of the nuclei in the image
	  * @throws ComponentCreationException 
	  */
	protected ICell makeComponent(Roi roi, int nucleusNumber) 
			throws ComponentCreationException{

		ICell result = null;
		
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

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );

//		fine("Offsetting CoM to point "+offsetCoM.toString());

		currentNucleus.moveCentreOfMass(offsetCoM);

//		fine("Setting basic stats");
		currentNucleus.setStatistic(PlottableStatistic.AREA,      values.get("Area"));
		currentNucleus.setStatistic(PlottableStatistic.MAX_FERET, values.get("Feret"));
		currentNucleus.setStatistic(PlottableStatistic.PERIMETER, values.get("Perim"));
//		finer("Setting scale");
		currentNucleus.setScale(options.getScale());

		currentNucleus.initialise(proportion);

		currentNucleus.findPointsAroundBorder();

		// if everything checks out, add the measured parameters to the global pool
		result = new DefaultCell(currentNucleus);

		return result;
	}  
	
}
