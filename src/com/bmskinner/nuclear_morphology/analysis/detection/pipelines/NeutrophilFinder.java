/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.CytoplasmFactory;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclear.LobeFactory;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.gui.Roi;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.binary.ChamferWeights;
import inra.ijpb.label.LabelImages;
import inra.ijpb.morphology.GeodesicReconstruction;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.DiskStrel;
import inra.ijpb.watershed.ExtendedMinimaWatershed;

/**
 * Detect neutrophils in H&E stained images
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class NeutrophilFinder extends CellFinder {

    private final ComponentFactory<ICytoplasm> cytoFactory = new CytoplasmFactory();
    private final ComponentFactory<Nucleus>    nuclFactory = new NucleusFactory(NucleusType.NEUTROPHIL);
    private final ComponentFactory<Lobe>       lobeFactory = new LobeFactory();

    private static final int     CONNECTIIVITY          = 4;
    private static final boolean IS_VERBOSE             = false;
    private static final boolean NORMALISE_DISTANCE_MAP = true;
    
    private final IDetectionOptions cytoOptions;
    private final IDetectionOptions nuclOptions;

    /**
     * Construct with an analysis options
     * 
     * @param op
     */
    public NeutrophilFinder(IAnalysisOptions op) {
        super(op);
        
        Optional<? extends IDetectionOptions> c = options.getDetectionOptions(IAnalysisOptions.CYTOPLASM);
        if(!c.isPresent())
        	throw new IllegalArgumentException("No cytoplasm options");

        cytoOptions = c.get();
        
        Optional<? extends IDetectionOptions> n = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
        if(!n.isPresent())
        	throw new IllegalArgumentException("No nucleus options");
        nuclOptions = n.get();
    }

    /*
     * METHODS IMPLEMENTING THE FINDER INTERFACE
     * 
     */

    @Override
    public List<ICell> findInImage(File imageFile) throws ImageImportException {
        List<ICell> list = new ArrayList<>();

        List<ICytoplasm> cyto = detectCytoplasm(imageFile);
        List<Nucleus> nucl = detectNucleus(imageFile, cyto);

        for (ICytoplasm c : cyto) {

            ICell cell = new DefaultCell(c);

            Iterator<Nucleus> it = nucl.iterator();

            while (it.hasNext()) {
                Nucleus n = it.next();
                if (c.containsOriginalPoint(n.getOriginalCentreOfMass())) {
                    cell.addNucleus(n);
                    it.remove();
                }
            }

            if (cell.hasNucleus()) {

                double cytoRatio = cell.getStatistic(PlottableStatistic.CELL_NUCLEAR_RATIO);
                if (cytoRatio < 1) { // if the nuclear area is greater than the
                                     // cytoplasmic area, something went wrong
                                     // in detection of cytoplasm
                    list.add(cell);
                }

            }

        }

        if (hasDetectionListeners()) {
            // Display the final image
            ImageProcessor ip = new ImageImporter(imageFile).importToColorProcessor();
            ImageAnnotator an = new ImageAnnotator(ip);

            for (ICell cell : list) {
                an.annotateCellBorders(cell);
            }
            fireDetectionEvent(an.toProcessor(), "Detected cells");

        }
        fireProgressEvent();
        return list;
    }

    /*
     * PRIVATE METHODS
     */

    /**
     * Find cytoplasms in the given image file
     * 
     * @param imageFile
     * @return
     * @throws ComponentCreationException
     * @throws ImageImportException
     */
    private List<ICytoplasm> detectCytoplasm(File imageFile) throws ImageImportException {
        List<ICytoplasm> result = new ArrayList<>();

        ImageProcessor ip;
        // Do the filtering. Returns binary images.
        if (cytoOptions.getBoolean(IDetectionOptions.IS_USE_WATERSHED)) {
        	ip = detectCytoplasmByWatershed(imageFile);
        	// fireDetectionEvent(ip.duplicate(), "Watershed");
        } else {
        	ip = detectCytoplasmByThreshold(imageFile);
        }
        // fireDetectionEvent(ip.duplicate(), "Cytoplasm mask");

        // Find rois
        GenericDetector gd = new GenericDetector();
        gd.setCirc(cytoOptions.getMinCirc(), cytoOptions.getMaxCirc());
        gd.setSize(cytoOptions.getMinSize(), cytoOptions.getMaxSize());

        Map<Roi, StatsMap> rois = gd.getRois(ip.duplicate());

        List<ICytoplasm> list = new ArrayList<>();
        int i = 0;
        for (Roi r : rois.keySet()) {
        	StatsMap m = rois.get(r);
			try {
				ICytoplasm cyto = makeCytoplasm(r, imageFile, cytoOptions, i, m);
				list.add(cyto);
			} catch (ComponentCreationException e) {
				stack("Error creating cytoplasm", e);
			} finally {
				i++;
			}
        	
        	

        }

        // Filter out the cytoplasms that will not pass detection

        for (ICytoplasm c : list) {
        	if (cytoOptions.isValid(c)) {
        		result.add(c);
        	}
        }

        // Draw the detected ROIs

        if (hasDetectionListeners()) {
        	// Input image for annotation
        	ImageProcessor ann = new ImageImporter(imageFile).importToColorProcessor();
        	ImageAnnotator an = new ImageAnnotator(ann);
        	for (ICytoplasm c : list) {
        		Color colour = cytoOptions.isValid(c) ? Color.ORANGE : Color.RED;
        		an.annotateBorder(c, colour);
        	}
        	fireDetectionEvent(an.toProcessor(), "Detected cytoplasm");

        }
        return result;

    }

    private ImageProcessor detectCytoplasmByWatershed(File imageFile)
            throws ImageImportException {

        ImageProcessor ip = new ImageImporter(imageFile).toConverter().convertToGreyscale(1).toProcessor();
        // fireDetectionEvent(ip.duplicate(), "Input");

        int dilationRadius = 3;
        int erosionDiameter = cytoOptions.getInt(IDetectionOptions.EROSION);
        int dynamic = cytoOptions.getInt(IDetectionOptions.DYNAMIC); // the
        // minimal
        // difference
        // between
        // a
        // minima
        // and
        // its
        // boundary

        ip = GeodesicReconstruction.fillHoles(ip);
        // fireDetectionEvent(ip.duplicate(), "Filled holes");

        ContrastEnhancer ch = new ContrastEnhancer();
        ch.setNormalize(true);
        ch.stretchHistogram(ip, 3); // default saturation value in ImageJ
        // fireDetectionEvent(ip.duplicate(), "Contrast enhanced");

        /*
         * Use a top hat filter to approximate cytoplasm
         */
        {

        	Strel strel = Strel.Shape.DISK.fromRadius(40);
        	ip = Morphology.blackTopHat(ip, strel);
        	// fireDetectionEvent(ip.duplicate(), "Top hat");

        	ip = LabelImages.labelBoundaries(ip);
        	// fireDetectionEvent(ip.duplicate(), "Boundaries");
        	ip.invert();

        	strel = Strel.Shape.DISK.fromRadius(dilationRadius);
        	ip = Morphology.erosion(ip, strel);
        	strel = Strel.Shape.DISK.fromRadius(dilationRadius * 2);
        	ip = Morphology.dilation(ip, strel);
        }

        // Calculate a distance map on the binarised input
        float[] floatWeights = ChamferWeights.CHESSKNIGHT.getFloatWeights();
        ImageProcessor dist = BinaryImages.distanceMap(ip, floatWeights, NORMALISE_DISTANCE_MAP);
        dist.invert();
        // fireDetectionEvent(dist.duplicate(), "Distance map");

        // Watershed the inverted map
        ImageProcessor watersheded = ExtendedMinimaWatershed.extendedMinimaWatershed(dist, ip, dynamic,
        		CONNECTIIVITY, IS_VERBOSE);
        // fireDetectionEvent(watersheded.duplicate(), "Distance transform
        // watershed");

        Strel strel = Strel.Shape.DISK.fromRadius(erosionDiameter);
        watersheded = Morphology.erosion(watersheded, strel);

        // Binarise for object detection
        ImageProcessor lines = BinaryImages.binarize(watersheded);
        // fireDetectionEvent(lines.duplicate(), "Binarized");
        // lines.invert();
        return lines;
    }

    private ImageProcessor detectCytoplasmByThreshold(File imageFile)
            throws ImageImportException {

        ImageProcessor ip = new ImageImporter(imageFile).importToColorProcessor();
        // fireDetectionEvent(ip.duplicate(), "Imported image");
        try {

            IPreprocessingOptions op = (IPreprocessingOptions) cytoOptions
                    .getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);

            int minHue = op.getMinHue();
            int maxHue = op.getMaxHue();
            int minSat = op.getMinSaturation();
            int maxSat = op.getMaxSaturation();
            int minBri = op.getMinBrightness();
            int maxBri = op.getMaxBrightness();

            ImageFilterer filt = new ImageFilterer(ip);

            filt.colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri);

            // the resulting processor has white cells on black. Invert.
            filt.invert();

            ip = filt.convertToByteProcessor().toProcessor();
            // fireDetectionEvent(ip.duplicate(), "To byte");

            return ip;

        } catch (MissingOptionException e) {
            error("Missing option", e);
            return null;
        }

    }

    private ICytoplasm makeCytoplasm(Roi roi, File f, IDetectionOptions options, int objectNumber,
            StatsMap values) throws ComponentCreationException {

        // measure the area, density etc within the nucleus


        // save the position of the roi, for later use
        int xbase = (int) roi.getXBase();
        int ybase = (int) roi.getYBase();

        Rectangle bounds = roi.getBounds();

        int[] originalPosition = { xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

        // create a Nucleus from the roi
        IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));

        ICytoplasm result = cytoFactory.buildInstance(roi, f, options.getChannel(), originalPosition, centreOfMass);

        // Move the nucleus xbase and ybase to 0,0 coordinates for charting
        IPoint offsetCoM = IPoint.makeNew(centreOfMass.getX() - xbase, centreOfMass.getY() - ybase);

        result.moveCentreOfMass(offsetCoM);

        result.setStatistic(PlottableStatistic.AREA, values.get("Area"));
        result.setStatistic(PlottableStatistic.MAX_FERET, values.get("Feret"));
        result.setStatistic(PlottableStatistic.PERIMETER, values.get("Perim"));

        result.setScale(options.getScale());

        return result;
    }

    private List<Nucleus> detectNucleus(File imageFile, List<ICytoplasm> mask)
            throws ImageImportException {
        List<Nucleus> result = new ArrayList<>();
        ImageProcessor ip = new ImageImporter(imageFile).importToColorProcessor();
        ImageProcessor ann = ip.duplicate();

        int topHatRadius = nuclOptions.getInt(IDetectionOptions.TOP_HAT_RADIUS);

        int thresholdMin = nuclOptions.getThreshold();

        ImageProcessor test = new ImageConverter(ip).convertToByteProcessor().toProcessor();
        Strel strel = DiskStrel.fromRadius(topHatRadius); // the structuring
        // element used
        // for black
        // top-hat
        ip = Morphology.blackTopHat(test, strel);
        // fireDetectionEvent(ip.duplicate(), "Nucleus top hat");
        //

        // Most remaining cytoplasm is weak, can can be thresholded away
        ImageProcessor bin = ip.duplicate();
        // ip.setMinAndMax(thresholdMin, thresholdMax);
        bin.threshold(thresholdMin);
        // bin.invert();

        // fireDetectionEvent(bin.duplicate(), "Thresholded top hat");

        List<Nucleus> list = new ArrayList<>();
        GenericDetector gd = new GenericDetector();
        gd.setCirc(nuclOptions.getMinCirc(), nuclOptions.getMaxCirc());
        gd.setSize(nuclOptions.getMinSize(), nuclOptions.getMaxSize());
        gd.setThreshold(thresholdMin);
        Map<Roi, StatsMap> rois = gd.getRois(bin);

        int i=0;
        for (Roi r : rois.keySet()) {
        	StatsMap m = rois.get(r);
        	Nucleus n = makeNucleus(r, imageFile, nuclOptions, i, m);
        	list.add(n);
        	i++;

        }

        for (Nucleus c : list) {
        	if (nuclOptions.isValid(c)) {
        		result.add(c);
        	}
        }

        if (hasDetectionListeners()) {
        	// fireDetectionEvent(ip.duplicate(), "Nucleus");

        	ImageAnnotator an = new ImageAnnotator(ann.duplicate());
        	for (Nucleus c : list) {
        		Color colour = nuclOptions.isValid(c) ? Color.ORANGE : Color.RED;
        		an.annotateBorder(c, colour);
        	}
        	fireDetectionEvent(an.toProcessor(), "Detected nucleus");
        }

        if (options.getNucleusType().equals(NucleusType.NEUTROPHIL)) {
        	detectLobesViaWatershed(ip, result);

        	ImageAnnotator an = new ImageAnnotator(ann.duplicate());
        	for (Nucleus c : list) {
        		if (c instanceof LobedNucleus) {
        			for (Lobe l : ((LobedNucleus) c).getLobes()) {
        				an.annotateBorder(l, Color.YELLOW);
        			}

        		}
        	}
        	fireDetectionEvent(an.toProcessor(), "Detected lobes");

        }

        return result;
    }

    private Nucleus makeNucleus(Roi roi, File f, IDetectionOptions options, int objectNumber,
            StatsMap values) throws ComponentCreationException {


        // save the position of the roi, for later use
        int xbase = (int) roi.getXBase();
        int ybase = (int) roi.getYBase();

        Rectangle bounds = roi.getBounds();

        int[] originalPosition = { xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

        // create a Nucleus from the roi
        IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));

        Nucleus result = nuclFactory.buildInstance(roi, f, options.getChannel(), originalPosition, centreOfMass);

        // Move the nucleus xbase and ybase to 0,0 coordinates for charting
        IPoint offsetCoM = IPoint.makeNew(centreOfMass.getX() - xbase, centreOfMass.getY() - ybase);

        result.moveCentreOfMass(offsetCoM);

        result.setStatistic(PlottableStatistic.AREA, values.get("Area"));
        result.setStatistic(PlottableStatistic.MAX_FERET, values.get("Feret"));
        result.setStatistic(PlottableStatistic.PERIMETER, values.get("Perim"));

        result.setScale(options.getScale());
        result.initialise(this.options.getProfileWindowProportion());
        result.findPointsAroundBorder();
        return result;
    }

    /*
     * Uses the Distance Transform watershed Take the distance map from the
     * input. Invert it, and perform watershed using the binary mask (dynamic of
     * 1 and 4-connectivity).
     * 
     * @param ip
     * 
     * @param list
     * 
     * @throws ComponentCreationException
     */
    private void detectLobesViaWatershed(ImageProcessor ip, List<Nucleus> list) throws ComponentCreationException {

        int erosionDiameter = 1;
        int dynamic = 1; // the minimal difference between a minima and its
                         // boundary

        // fireDetectionEvent(ip.duplicate(), "Lobe detection input");
        ImageProcessor mask = ip.duplicate();

        mask.threshold(20);
        // fireDetectionEvent(mask.duplicate(), "Binarised input");

        // Calculate a distance map on the binarised input
        float[] floatWeights = ChamferWeights.CHESSKNIGHT.getFloatWeights();
        ImageProcessor dist = BinaryImages.distanceMap(mask, floatWeights, NORMALISE_DISTANCE_MAP);
        dist.invert();
        // fireDetectionEvent(dist.duplicate(), "Distance map");

        // Watershed the inverted map
        ImageProcessor watersheded = ExtendedMinimaWatershed.extendedMinimaWatershed(dist, mask, dynamic, CONNECTIIVITY,
                IS_VERBOSE);
        // fireDetectionEvent(watersheded.duplicate(), "Distance transform
        // watershed");

        // Binarise for object detection
        ImageProcessor lines = BinaryImages.binarize(watersheded);
        // fireDetectionEvent(lines.duplicate(), "Binarized");

        // Erode by 1 pixel to better separate lobes
        Strel erosionStrel = Strel.Shape.DISK.fromDiameter(erosionDiameter);
        lines = Morphology.erosion(lines, erosionStrel);
        // fireDetectionEvent(lines.duplicate(), "Eroded");

        // lines.invert();

        // Now take the watershed image, and detect the distinct lobes
        makeLobes(lines, list);

    }

    /**
     * Detect lobes in the given processed image, and assign them to nuclei
     * 
     * @param ip
     *            the binary image with lobe objects
     * @param list
     *            the nuclei to which lobes in this image belong
     * @throws ComponentCreationException
     */
    private void makeLobes(ImageProcessor ip, List<Nucleus> list) throws ComponentCreationException {

        int minArea = 5;
        int maxArea = 3000;

        GenericDetector gd = new GenericDetector();
        gd.setIncludeHoles(false);
        gd.setSize(minArea, maxArea);
        Map<Roi, StatsMap> rois = gd.getRois(ip);
        
        
        for (Roi roi : rois.keySet()) {
            for (Nucleus n : list) {
                LobedNucleus l = (LobedNucleus) n;
                StatsMap m = rois.get(roi);
                int x = m.get(GenericDetector.COM_X).intValue();
                int y = m.get(GenericDetector.COM_Y).intValue();
                IPoint com = IPoint.makeNew(x, y);
                if (n.containsOriginalPoint(com)) {
                    // Now adjust the roi base to match the source image
                    IPoint base = IPoint.makeNew(roi.getXBase(), roi.getYBase());

                    Rectangle bounds = roi.getBounds();

                    roi.setLocation(base.getXAsInt(), base.getYAsInt());

                    int[] originalPosition = { base.getXAsInt(), base.getYAsInt(), (int) bounds.getWidth(),
                            (int) bounds.getHeight() };

                    Lobe lobe = lobeFactory.buildInstance(roi, l.getSourceFile(), 0, originalPosition, com);

                    l.addLobe(lobe); // TODO makethe channel useful
                }
            }
        }
    }

}
