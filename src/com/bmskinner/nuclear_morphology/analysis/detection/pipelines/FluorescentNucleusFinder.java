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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.profiles.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
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
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;
import ij.process.ImageProcessor;

public class FluorescentNucleusFinder extends CellFinder {
	
	private static final Logger LOGGER = Logger.getLogger(FluorescentNucleusFinder.class.getName());

    private final NucleusFactory nuclFactory;
    private final HashOptions nuclOptions;
    private final FinderDisplayType displayType;
    
    public FluorescentNucleusFinder(@NonNull final IAnalysisOptions op, FinderDisplayType t) {
        super(op);
        displayType = t;
        
        if(op.getRuleSetCollection()==null)
        	throw new IllegalArgumentException("No ruleset provided");
        
        Optional<HashOptions> n = options.getDetectionOptions(CellularComponent.NUCLEUS);
        if(!n.isPresent())
        	throw new IllegalArgumentException("No nucleus options");
        nuclOptions = n.get();
        
        nuclFactory = new NucleusFactory(op.getRuleSetCollection(), 
        		options.getProfileWindowProportion(),
        		nuclOptions.getDouble(HashOptions.SCALE));
    }

    @Override
    public List<ICell> findInImage(@NonNull final File imageFile) throws ImageImportException {
    	List<ICell> list = new ArrayList<>();

    	try {
    		if(FinderDisplayType.PREVIEW.equals(displayType)) {
    			// Get all objects and annotate if passing filters
    			list = detectNucleusPreview(imageFile);
    		}
    		
    		if(FinderDisplayType.PIPELINE.equals(displayType)) {
    			// Get only objects matching filters
    			list = detectNucleusPipeline(imageFile);
    		}
    	} finally {
    		fireProgressEvent();
    	}        
    	return list;
    }
    
    private List<ICell> detectNucleusPipeline(@NonNull final File imageFile)
    		throws ImageImportException {
    	List<Nucleus> list = new ArrayList<>();
    	
        ImageImporter importer = new ImageImporter(imageFile);

        ImageProcessor ip = importer.importImage(nuclOptions.getInt(HashOptions.CHANNEL));

        ImageFilterer filt = new ImageFilterer(ip.duplicate());
        if (nuclOptions.getBoolean(HashOptions.IS_USE_KUWAHARA)) {
            filt.kuwaharaFilter(nuclOptions.getInt(HashOptions.KUWAHARA_RADIUS_INT));
        }

        if (nuclOptions.getBoolean(HashOptions.IS_USE_FLATTENING)) {
            filt.setMaximumPixelValue(nuclOptions.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
        }

        
        if (nuclOptions.getBoolean(HashOptions.IS_USE_CANNY)) {
            filt.cannyEdgeDetection(nuclOptions);
            filt.close(nuclOptions.getInt(HashOptions.CANNY_CLOSING_RADIUS_INT));

        } else {
            filt.threshold(nuclOptions.getInt(HashOptions.THRESHOLD));
        }

        LOGGER.finer("Detecting ROIs in "+imageFile.getName());
        GenericDetector gd = new GenericDetector();

        gd.setSize(nuclOptions.getInt(HashOptions.MIN_SIZE_PIXELS), nuclOptions.getInt(HashOptions.MAX_SIZE_PIXELS)); 

        ImageProcessor img = filt.toProcessor();

        Map<Roi, StatsMap> rois = gd.getRois(img.duplicate());
        LOGGER.finer(()->"Image: "+imageFile.getName()+": "+rois.size()+" rois");
        
        for(Entry<Roi, StatsMap> entry : rois.entrySet()) {
            try {
            	Nucleus n = makeNucleus(entry.getKey(), imageFile, entry.getValue());
            	list.add(n);
            } catch(ComponentCreationException e) {
            	LOGGER.log(Loggable.STACK, "Unable to create nucleus from roi: "+e.getMessage()+"; skipping", e);
            }
        }
        LOGGER.fine(()->"Detected nuclei in "+imageFile.getName());
        
		List<ICell> result = new ArrayList<>();
		for (Nucleus n : list) {
			if (isValid(nuclOptions,n)) {
				result.add(new DefaultCell(n));
			}
		}
        return result;
    }

    private List<ICell> detectNucleusPreview(@NonNull final File imageFile)
            throws ImageImportException {

        List<Nucleus> list = new ArrayList<>();

        ImageImporter importer = new ImageImporter(imageFile);
        
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
        
        // Display passing and failing size nuclei
   		ImageProcessor original = importer
   				.importImageAndInvert(nuclOptions.getInt(HashOptions.CHANNEL))
   				.convertToRGB();
        
        // do not use the minimum nucleus size - we want the roi outlined in red
        gd.setSize(MIN_PROFILABLE_OBJECT_SIZE, original.getWidth() * original.getHeight()); 

        ImageProcessor img = filt.toProcessor();

        
        Map<Roi, StatsMap> rois = gd.getRois(img.duplicate());
        LOGGER.finer(()->"Image: "+imageFile.getName()+": "+rois.size()+" rois");
        
        for(Entry<Roi, StatsMap> entry : rois.entrySet()) {
            try {
            	Nucleus n = makeNucleus(entry.getKey(), imageFile, entry.getValue());
            	list.add(n);
            } catch(ComponentCreationException e) {
            	LOGGER.log(Loggable.STACK, "Unable to create nucleus from roi: "+e.getMessage()+"; skipping", e);
            }
        }
        LOGGER.finer(()->"Detected nuclei in "+imageFile.getName());
        

		ImageAnnotator ann = new ImageAnnotator(original);

		for (Nucleus n : list) {
			Color colour = isValid(nuclOptions,n) ? Color.ORANGE : Color.RED;
			ann.annotateBorder(n, colour);
		}
		fireDetectionEvent(ann.toProcessor().duplicate(), "Detected objects");

		for (Nucleus n : list) {
			ann.annotateStats(n, Color.ORANGE, Color.BLUE);
		}
		fireDetectionEvent(ann.toProcessor().duplicate(), "Annotated objects");
		
		List<ICell> result = new ArrayList<>();
		for (Nucleus n : list) {
			if (isValid(nuclOptions,n)) {
				result.add(new DefaultCell(n));
			}
		}
        return result;
    }

    private synchronized Nucleus makeNucleus(final Roi roi, final File f,
            final StatsMap values) throws ComponentCreationException {
        
        LOGGER.fine(()->"Creating nucleus from roi "+f.getName()+" area: "+values.get(StatsMap.AREA));

        // save the position of the roi, for later use
//        int xbase = (int) roi.getXBase();
//        int ybase = (int) roi.getYBase();

//        Rectangle bounds = roi.getBounds();

//        int[] originalPosition = { xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

        // create a Nucleus from the roi
        IPoint centreOfMass = IPoint.makeNew(values.get(StatsMap.COM_X), values.get(StatsMap.COM_Y));

        Nucleus result = nuclFactory.new NucleusBuilder()
        	.fromRoi(roi)
        	.withFile(f).withChannel(nuclOptions.getInt(HashOptions.CHANNEL))
        	.withCoM(centreOfMass)
        	.withMeasurement(Measurement.AREA, values.get(StatsMap.AREA))
        	.withMeasurement(Measurement.MAX_FERET, values.get(StatsMap.FERET))
        	.withMeasurement(Measurement.PERIMETER, values.get(StatsMap.PERIM))
        	.build();
        
//        Nucleus result = nuclFactory.buildInstance(roi, f, nuclOptions.getInt(HashOptions.CHANNEL),
//        		originalPosition, centreOfMass);

        // Move the nucleus xbase and ybase to 0,0 coordinates for charting
//        IPoint offsetCoM = IPoint.makeNew(centreOfMass.getX() - xbase, centreOfMass.getY() - ybase);
//
//        result.moveCentreOfMass(offsetCoM);


//        result.setStatistic(Measurement.AREA, values.get(StatsMap.AREA));
//        result.setStatistic(Measurement.MAX_FERET, values.get(StatsMap.FERET));
//        result.setStatistic(Measurement.PERIMETER, values.get(StatsMap.PERIM));


//        result.setScale(nuclOptions.getDouble(HashOptions.SCALE));

//        double prop = options.getProfileWindowProportion();
//        result.initialise(prop);
        ProfileIndexFinder.assignLandmarks(result, options.getRuleSetCollection());
//        result.findLandmarks(options.getRuleSetCollection());
        LOGGER.finer(()->"Created nucleus from roi "+f.getName());
        return result;
    }
    
    /**
     * Finds the key points of interest around the border of the object.
     * Specify the rulesets that should be used for landmark detection and
     * subsequent orientation.
     * @throws ComponentCreationException
     */
//    public void findLandmarks(Nucleus n) throws ComponentCreationException {
//    	
//    	for(Landmark lm : options.getRuleSetCollection().getTags()) {
//    		LOGGER.finer(()->"Locating "+lm);
//    		
//    		try {
//    			for(RuleSet rule : options.getRuleSetCollection().getRuleSets(lm)) {
//    				IProfile p = n.getProfile(rule.getType());
//    				int index = ProfileIndexFinder.identifyIndex(p, rule);
//    				n.setLandmark(lm, index);
//    			}
//    		} catch (MissingProfileException e) {
//    			LOGGER.log(Loggable.STACK, "Error getting profile type", e);
//    		} catch (NoDetectedIndexException e) {
//    			LOGGER.fine("Unable to detect "+lm+" in nucleus");
////    			try {
////					setLandmark(lm, 0);
////				} catch (MissingProfileException | ProfileException e1) {
////					LOGGER.log(Loggable.STACK, "Error getting profile type", e);
////				}
//    		} catch (ProfileException e) {
//    			LOGGER.log(Loggable.STACK, "Error getting profile type", e);
//			} catch (IndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (MissingLandmarkException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    }

}
