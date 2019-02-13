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
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclear.LobeFactory;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus.IncorrectNucleusTypeException;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.DiskStrel;
import inra.ijpb.watershed.Watershed;

/**
 * This method finds lobes within nuclei. It is designed to work on neutrophils.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class LobeDetectionMethod extends SingleDatasetAnalysisMethod {

    private IHoughDetectionOptions options;

    private static final double DEFAULT_MIN_LOBE_AREA = 10;

    private final LobeFactory factory = new LobeFactory();

    public LobeDetectionMethod(IAnalysisDataset dataset, IHoughDetectionOptions op) {
        super(dataset);
        options = op;
    }

    @Override
    public IAnalysisResult call() throws Exception {

        fine("Running lobe detection method");

        
        if (NucleusType.NEUTROPHIL.equals(dataset.getAnalysisOptions().get().getNucleusType())) {
            run();
        } else {
            throw new IncorrectNucleusTypeException("Not a lobed nucleus type");
        }

        IAnalysisResult r = new DefaultAnalysisResult(dataset);
        return r;
    }

    private void run() {

        // Clear existing lobes
        dataset.getCollection().getNuclei().stream().forEach(n -> {
            if (n instanceof LobedNucleus) {
                ((LobedNucleus) n).removeAllLobes();
                n.setStatistic(PlottableStatistic.LOBE_COUNT, Statistical.STAT_NOT_CALCULATED);
            }
        });

        // Remove existing cached stats
        dataset.getCollection().clear(PlottableStatistic.LOBE_COUNT, CellularComponent.NUCLEUS);
        dataset.getCollection().clear(PlottableStatistic.LOBE_COUNT, CellularComponent.WHOLE_CELL);
        
        dataset.getCollection().getCells().forEach(c->{
            detectLobes(c);
            fireProgressEvent();
        });
    }

    /**
     * Identify lobes within the nuclei of the cell
     * 
     * @param cell
     */
    private void detectLobes(ICell cell) {

        try {

            detectLobesViaWatershed(cell);
            // detectLobesViaHough(cell);

        } catch (UnloadableImageException e) {
            warn("Unable to load cell image");
            stack(e);
        } catch (Exception e) {
            warn("Error in lobe detection");
            stack(e.getMessage(), e);
        }
    }

    /**
     * Detect lobes using the hough transform
     * 
     * @param cell
     * @throws UnloadableImageException
     * @throws MissingOptionException
     */
//    private void detectLobesViaHough(ICell cell) throws UnloadableImageException, MissingOptionException {
//
//    	Optional<IAnalysisOptions> an = dataset.getAnalysisOptions();
//        if(!an.isPresent())
//        	throw new MissingOptionException("Options not present in dataset "+dataset.getName());
//
//        Optional<IDetectionOptions> no = an.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
//        
//        if(!no.isPresent())
//        	throw new MissingOptionException("Nucleus options not present in dataset "+dataset.getName());
//        
//        IDetectionOptions nucleusOptions = no.get();
//        
//        IPreprocessingOptions op = (IPreprocessingOptions) nucleusOptions
//                .getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
//
//        ImageProcessor ip = cell.getCytoplasm().getComponentRGBImage();
//        if (op.isUseColourThreshold()) {
//
//            int minHue = op.getMinHue();
//            int maxHue = op.getMaxHue();
//            int minSat = op.getMinSaturation();
//            int maxSat = op.getMaxSaturation();
//            int minBri = 73;// op.getMinBrightness();
//            int maxBri = 255;// op.getMaxBrightness();
//
//            ImageProcessor test = new ImageFilterer(ip).colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri)
//                    .convertToByteProcessor().toProcessor();
//
//            //
//            // ICannyOptions canny = OptionsFactory.makeCannyOptions();
//
//            ImageFilterer imf = new ImageFilterer(test);
//            // .runEdgeDetector(canny);
//
//            // new ImagePlus(cell.getNucleus().getNameAndNumber()+": Canny",
//            // imf.toProcessor()).show();
//            List<IPoint> lobes = imf.houghCircleDetection(options);
//            addPointsToNuclei(cell, lobes);
//        }
//
//    }

    /**
     * Identify lobes based on watershed segmentation of nuclei within cytoplasm
     * 
     * @param cell
     * @throws UnloadableImageException
     * @throws ComponentCreationException
     */
    private void detectLobesViaWatershed(ICell cell) throws UnloadableImageException, ComponentCreationException {

        int topHatRadius = 20;
        boolean calculateDams = true;
        int gradientRadius = 2;
        int connectivity = 6;
        int bitDepth = 32;
        int dynamic = 10; // the minimal difference between a minima and its
                          // boundary

        int thresholdMin = 20;
        int thresholdMax = 255;

        Strel strel = DiskStrel.fromRadius(topHatRadius); // the structuring
                                                          // element used for
                                                          // black top-hat

        // Since we're using the component image, we need to offset coordinates
        // back to the
        // source image when testing if a lobe is within the nucleus
        ImageProcessor ip = cell.getCytoplasm().getComponentRGBImage();

        ImageProcessor test = new ImageConverter(ip).convertToByteProcessor().toProcessor();

        /*
         * Top hat filtering removes most cytoplasm background Computes black
         * top hat (or "bottom hat") of the original image. The black top hat is
         * obtained by subtracting the original image from the result of a
         * closing. The black top hat enhances dark structures smaller than the
         * structuring element.
         */

        ImageProcessor th = Morphology.blackTopHat(test, strel);

        // Most remaining cytoplasm is weak, can can be thresholded away
        th.setMinAndMax(thresholdMin, thresholdMax);

        ImagePlus imp = new ImagePlus("Min max", th);
        // imp.show();

        // Copy the process used in the MorphoLbJ MorphologicalSegmentation
        // plugin

        ImageStack image = new ImageStack(th.getWidth(), th.getHeight());
        image.addSlice(th);

        strel = Strel.Shape.SQUARE.fromRadius(gradientRadius);

        /*
         * Computes the morphological gradient of the input image. The
         * morphological gradient is obtained from the difference of image
         * dilation and image erosion computed with the same structuring
         * element.
         */
        ImageProcessor gradient = Morphology.gradient(image.getProcessor(1), strel);
        image = new ImageStack(image.getWidth(), image.getHeight());
        image.addSlice(gradient);

        ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima(image, dynamic, connectivity);

        /*
         * Computes the labels in the binary 2D or 3D image contained in the
         * given ImagePlus, and computes the maximum label to set up the display
         * range of the resulting ImagePlus.
         */
        ImageStack labeledMinima = BinaryImages.componentsLabeling(regionalMinima, connectivity, bitDepth);
        ImagePlus min = new ImagePlus("", labeledMinima.getProcessor(1));

        /*
         * Compute watershed with markers with a binary mask to restrict the
         * regions of application
         */
        ImagePlus resultStack = Watershed.computeWatershed(imp, min, connectivity, calculateDams);
        // resultStack.show();
        final ImagePlus lines = BinaryImages.binarize(resultStack);
        // lines.show();

        ImageProcessor lp = lines.getProcessor();
        lp.invert();

        // Now take the watershed image, and detect the distinct lobes

        ImageFilterer ft = new ImageFilterer(lp);
        ImageProcessor ws = ft.dilate(1).toProcessor();

        GenericDetector gd = new GenericDetector();
        gd.setIncludeHoles(false);
        gd.setSize(DEFAULT_MIN_LOBE_AREA, cell.getStatistic(PlottableStatistic.CELL_NUCLEAR_AREA));
//        List<Roi> rois = gd.getRois(ws);
        
        Map<Roi, StatsMap> rois = gd.getRois(ws);

        addLobesToNuclei(cell, rois);
    }

    private void addLobesToNuclei(ICell cell, Map<Roi, StatsMap> rois)
            throws UnloadableImageException, ComponentCreationException {

//        GenericDetector gd = new GenericDetector();

        List<Nucleus> nuclei = cell.getNuclei();

        for (Nucleus n : nuclei) {

            if (n instanceof LobedNucleus) {
                LobedNucleus l = (LobedNucleus) n;
//                ImageProcessor ip = l.getImage();

                // Add this to the roi centre of mass

                for (Roi roi : rois.keySet()) {
                    StatsMap m = rois.get(roi);
                    int x = m.get(GenericDetector.COM_X).intValue();
                    int y = m.get(GenericDetector.COM_Y).intValue();
                    IPoint com = IPoint.makeNew(x, y);
                    IPoint adj = Imageable.translateCoordinateToSourceImage(com, cell.getCytoplasm());

                    // log("\tTesting "+adj.toString());
                    if (l.containsOriginalPoint(adj)) {
                        // log("\t\tMatch ");

                        // Now adjust the roi base to match the source image
                        IPoint base = IPoint.makeNew(roi.getXBase(), roi.getYBase());
                        IPoint adjBase = Imageable.translateCoordinateToSourceImage(base, cell.getCytoplasm());

                        Rectangle bounds = roi.getBounds();

                        roi.setLocation(adjBase.getXAsInt(), adjBase.getYAsInt());

                        int[] originalPosition = { adjBase.getXAsInt(), adjBase.getYAsInt(), (int) bounds.getWidth(),
                                (int) bounds.getHeight() };

                        Lobe lobe = factory.buildInstance(roi, l.getSourceFile(), 0, originalPosition, adj);
                        // lobe.moveCentreOfMass(adj);

                        l.addLobe(lobe); // TODO makethe channel useful
                    }

                }

                l.setStatistic(PlottableStatistic.LOBE_COUNT, l.getLobeCount());
            }

        }
        // Update stats
        double lobes = cell.getNuclei().stream().mapToDouble(n -> n.getStatistic(PlottableStatistic.LOBE_COUNT)).sum();
        cell.setStatistic(PlottableStatistic.LOBE_COUNT, lobes);
    }

    private void addPointsToNuclei(ICell cell, List<IPoint> points) {
        // log("Adding "+points.size()+" points to nuclei");

        IPoint base = cell.getCytoplasm().getOriginalBase();

        List<Nucleus> nuclei = cell.getNuclei();

        for (Nucleus n : nuclei) {

            if (n instanceof LobedNucleus) {
                LobedNucleus l = (LobedNucleus) n;

                // Trim the points so that the centre of a point cluster is
                // chosen,
                // rather than the whole cloud

                // make a binary mask over the points
                // get the ROIs encompassing them
                // Add the CoM of each ROI

                try {
                    // // This is just to get the dimensions of the original
                    // image
                    // // TODO - use the component size and apply an offset
                    ImageProcessor ip = l.getImage();

                    int w = ip.getWidth();
                    int h = ip.getHeight();
                    ByteProcessor bp = new ByteProcessor(w, h);

                    for (int i = 0; i < w * h; i++) {
                        bp.set(i, 0);
                    }

                    for (IPoint p : points) {

                        int oX = p.getXAsInt() + base.getXAsInt() - CellularComponent.COMPONENT_BUFFER;
                        int oY = p.getYAsInt() + base.getYAsInt() - CellularComponent.COMPONENT_BUFFER;

//                        IPoint oP = IPoint.makeNew(oX, oY);

                        bp.set(oX, oY, 255);
                    }

                    // Now look for ROIs in the byte processor
                    GenericDetector dt = new GenericDetector();
                    
                    Map<Roi, StatsMap> rois = dt.getRois(bp);
                    for (Roi roi : rois.keySet()) {
                        StatsMap m = rois.get(roi);
                        int x = m.get(GenericDetector.COM_X).intValue();
                        int y = m.get(GenericDetector.COM_Y).intValue();

                        IPoint com = IPoint.makeNew(x, y);
                        if (l.containsOriginalPoint(com)) {
                            Rectangle bounds = roi.getBounds();
                            int xbase = (int) roi.getXBase();
                            int ybase = (int) roi.getYBase();
                            int[] originalPosition = { xbase, ybase, (int) bounds.getWidth(),
                                    (int) bounds.getHeight() };
                            l.addLobe(factory.buildInstance(roi, l.getSourceFile(), 0, originalPosition, com)); // TODO
                                                                                                                // makethe
                                                                                                                // channel
                                                                                                                // useful
                            // l.addLobeCentre(com);
                        }

                    }

                    // log(l.getLobeCount()+" lobes");
                    // Copy stat for charting
                    l.setStatistic(PlottableStatistic.LOBE_COUNT, l.getLobeCount());
                } catch (UnloadableImageException | ComponentCreationException e) {
                    stack(e);
                }
            }

        }

    }

}
