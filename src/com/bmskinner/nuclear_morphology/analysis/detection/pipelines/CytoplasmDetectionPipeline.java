package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Rectangle;
import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.components.CytoplasmFactory;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.gui.Roi;

public class CytoplasmDetectionPipeline extends DetectionPipeline<ICytoplasm> {

	public CytoplasmDetectionPipeline(IDetectionOptions op, File imageFile, double prop) throws ImageImportException {
		super(op, imageFile, prop);
	}

	@Override
	protected ICytoplasm makeComponent(Roi roi, int objectNumber) throws ComponentCreationException {
		
		  // measure the area, density etc within the nucleus
		StatsMap values   = measure(roi, ip);

		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		Rectangle bounds = roi.getBounds();

		int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		// create a Nucleus from the roi
		IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));

		ICytoplasm result = new CytoplasmFactory(file).buildInstance(roi, options.getChannel(), originalPosition, centreOfMass); 

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );

		fine("Offsetting CoM to point "+offsetCoM.toString());

		result.moveCentreOfMass(offsetCoM);

		result.setStatistic(PlottableStatistic.AREA,      values.get("Area"));
		result.setStatistic(PlottableStatistic.MAX_FERET, values.get("Feret"));
		result.setStatistic(PlottableStatistic.PERIMETER, values.get("Perim"));

		result.setScale(options.getScale());

		return result;
	}
	
	

}
