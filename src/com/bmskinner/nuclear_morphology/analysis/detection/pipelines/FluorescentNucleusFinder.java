/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;
import ij.process.ImageProcessor;

public class FluorescentNucleusFinder extends CellFinder {
	
	private static final Logger LOGGER = Logger.getLogger(FluorescentNucleusFinder.class.getName());

    private final ComponentFactory<Nucleus> nuclFactory;
    private final HashOptions nuclOptions;
    
    public FluorescentNucleusFinder(@NonNull final IAnalysisOptions op) {
        super(op);
        nuclFactory = new NucleusFactory(op.getRuleSetCollection());
        Optional<HashOptions> n = options.getDetectionOptions(CellularComponent.NUCLEUS);
        if(!n.isPresent())
        	throw new IllegalArgumentException("No nucleus options");
        nuclOptions = n.get();
    }

    @Override
    public List<ICell> findInImage(@NonNull final File imageFile) throws ImageImportException {
        List<ICell> list = new ArrayList<>();

        try {
            // Get all objects that could be nuclei
            List<Nucleus> nuclei = detectNucleus(imageFile);

            // Display passing and failing size nuclei
            if (hasDetectionListeners()) {

                ImageProcessor original = new ImageImporter(imageFile).importImageAndInvert(nuclOptions.getInt(HashOptions.CHANNEL))
                        .convertToRGB();
                ImageAnnotator ann = new ImageAnnotator(original);

                for (Nucleus n : nuclei) {
                    Color colour = isValid(nuclOptions,n) ? Color.ORANGE : Color.RED;
                    ann.annotateBorder(n, colour);
                }
                fireDetectionEvent(ann.toProcessor().duplicate(), "Detected objects");

                for (Nucleus n : nuclei) {
                    ann.annotateStats(n, Color.ORANGE, Color.BLUE);
                }
                fireDetectionEvent(ann.toProcessor().duplicate(), "Annotated objects");
            }

            for (Nucleus n : nuclei) {
                if (isValid(nuclOptions,n)) {
                	ICell c = new DefaultCell(n);
                	if(c!=null)
                		list.add(c);
                }
            }
        } finally {
        	fireProgressEvent();
        }        
        return list;
    }

    private List<Nucleus> detectNucleus(@NonNull final File imageFile)
            throws ImageImportException {

        List<Nucleus> list = new ArrayList<>();

        ImageImporter importer = new ImageImporter(imageFile);
        
        ImageProcessor original = importer.toConverter()
        		.convertToRGBGreyscale()
                .invert()
                .toProcessor();

        ImageProcessor ip = importer.importImage(nuclOptions.getInt(HashOptions.CHANNEL));

        ImageFilterer filt = new ImageFilterer(ip.duplicate());
        if (nuclOptions.getBoolean(HashOptions.IS_USE_KUWAHARA)) {
            filt.kuwaharaFilter(nuclOptions.getInt(HashOptions.KUWAHARA_RADIUS_INT));
            if (hasDetectionListeners()) {
            	ip = filt.toProcessor().duplicate();
            	ip.invert();
            	fireDetectionEvent(ip.duplicate(), "Kuwahara filter");
            }
        }

        if (nuclOptions.getBoolean(HashOptions.IS_USE_FLATTENING)) {
            filt.setMaximumPixelValue(nuclOptions.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
            if (hasDetectionListeners()) {
            	ip = filt.toProcessor().duplicate();
            	ip.invert();
            	fireDetectionEvent(ip.duplicate(), "Chromocentre flattening");
            }
        }

        if (nuclOptions.getBoolean(HashOptions.IS_USE_CANNY)) {
            filt.cannyEdgeDetection(nuclOptions);
            if (hasDetectionListeners()) {
            	ip = filt.toProcessor().duplicate();
            	ip.invert();
            	fireDetectionEvent(ip.duplicate(), "Edge detection");
            }

            filt.close(nuclOptions.getInt(HashOptions.CANNY_CLOSING_RADIUS_INT));
            if (hasDetectionListeners()) {
            	ip = filt.toProcessor().duplicate();
            	ip.invert();
            	fireDetectionEvent(ip.duplicate(), "Gap closing");
            }
        } else {
            filt.threshold(nuclOptions.getInt(HashOptions.THRESHOLD));
            if (hasDetectionListeners()) {
            	ip = filt.toProcessor().duplicate();
            	ip.invert();
            	fireDetectionEvent(ip.duplicate(), "Thresholded");
            }
        }

        GenericDetector gd = new GenericDetector();
        gd.setSize(MIN_PROFILABLE_OBJECT_SIZE, original.getWidth() * original.getHeight()); // do not use the minimum nucleus size - we want the roi outlined in red

        ImageProcessor img = filt.toProcessor();

        
        Map<Roi, StatsMap> rois = gd.getRois(img.duplicate());
        LOGGER.finer("Image: "+imageFile.getName()+": "+rois.size()+" rois");
        
        for(Roi roi : rois.keySet()) {
            StatsMap s = rois.get(roi);

            try {
            	Nucleus n = makeNucleus(roi, imageFile, s);
            	list.add(n);
            } catch(ComponentCreationException e) {
            	LOGGER.log(Loggable.STACK, "Unable to create nucleus from roi: "+e.getMessage()+"; skipping", e);
            }
        }
        LOGGER.finer("Detected nuclei in "+imageFile.getName());
        return list;
    }

    private synchronized Nucleus makeNucleus(final Roi roi, final File f,
            final StatsMap values) throws ComponentCreationException {
        
        LOGGER.fine("Creating nucleus from roi "+f.getName()+" area: "+values.get(StatsMap.AREA));

        // save the position of the roi, for later use
        int xbase = (int) roi.getXBase();
        int ybase = (int) roi.getYBase();

        Rectangle bounds = roi.getBounds();

        int[] originalPosition = { xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

        // create a Nucleus from the roi
        IPoint centreOfMass = IPoint.makeNew(values.get(StatsMap.COM_X), values.get(StatsMap.COM_Y));

        Nucleus result = nuclFactory.buildInstance(roi, f, nuclOptions.getInt(HashOptions.CHANNEL), originalPosition, centreOfMass);

        // Move the nucleus xbase and ybase to 0,0 coordinates for charting
        IPoint offsetCoM = IPoint.makeNew(centreOfMass.getX() - xbase, centreOfMass.getY() - ybase);

        result.moveCentreOfMass(offsetCoM);


        result.setStatistic(Measurement.AREA, values.get(StatsMap.AREA));
        result.setStatistic(Measurement.MAX_FERET, values.get(StatsMap.FERET));
        result.setStatistic(Measurement.PERIMETER, values.get(StatsMap.PERIM));


        result.setScale(nuclOptions.getDouble(HashOptions.SCALE));

        double prop = options.getProfileWindowProportion();
        result.initialise(prop);
        result.findLandmarks(options.getRuleSetCollection());
        LOGGER.finer("Created nucleus from roi "+f.getName());
        return result;
    }

}
