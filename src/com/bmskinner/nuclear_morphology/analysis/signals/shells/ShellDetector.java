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


/*
  -----------------------
  SHELL ANALYSIS
  -----------------------
  Signal positions in round nuclei.
*/
package com.bmskinner.nuclear_morphology.analysis.signals.shells;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.ImageProcessor;

/**
 * The shell detector carries out the task of dividing components into shells of
 * equal area, and calculating the proportion of signal intensity within each
 * shell
 * 
 * @author bms41
 * @since 1.13.1
 *
 */
public class ShellDetector extends Detector {

    public static final int DEFAULT_SHELL_COUNT = 5;

    // private final ImageStack st;

    /**
     * The shell ROIs within the template object. This list begins with the
     * largest shell (index 0) and ends with the smallest shell. The larger
     * shells include the area contained within smaller shells.
     */
    private List<Shell> shells = new ArrayList<Shell>(0);

    /**
     * Create shells in the given component, using the default shell count
     * 
     * @param n
     *            the component to analyse
     */
    public ShellDetector(CellularComponent n) throws ShellAnalysisException {

        this(n, ShellDetector.DEFAULT_SHELL_COUNT);
    }

    /**
     * Create shells in the given nucleus, using the given shell count
     *
     * @param n
     *            the component to analyse
     * @param shellCount
     *            the number of shells to create
     * @throws ShellAnalysisException
     */
    public ShellDetector(CellularComponent n, int shellCount) throws ShellAnalysisException {

        createShells(n, shellCount);
    }

    /**
     * Get the shells created
     * 
     * @return
     */
    public List<Shell> getShells() {
        return this.shells;
    }

    /**
     * Find the shell in the template object that the given point lies within,
     * or -1 if the point is not found
     * 
     * @param p
     * @return
     */
    int findShell(IPoint p) {

        int shell = -1;
        for (Shell r : shells) {
            if (r.contains(p.getXAsInt(), p.getYAsInt())) {
                shell++;
            }
        }
        return shell;
    }

    /*
     * 
     * METHODS FOR COUNTING THE NUMBER OF PIXELS WITHIN A SHELL, REGARDLESS OF
     * INTENSITY
     * 
     */

    /**
     * Find the number pixels of the signal within each shell
     * 
     * @param signal
     * @return
     */
    long[] findPixelCountPerShell(CellularComponent signal) {

//        fine("Calculating the number of signal pixels per shell");

        long[] counts = makeZeroArray();

        for (int i = 0; i < shells.size(); i++) {

            Shell shell = shells.get(i);
            long count = shell.getCount(signal);
            counts[i] = count;

        }

        counts = correctNestedIntensities(counts);
        return counts;
    }

    /**
     * Count the total pixel intensity in each shell for the given image
     * 
     * @param st the image stack to analyse
     * @param channel the RGB channel in the stack
     * @return
     */
    long[] findPixelIntensityPerShell(ImageStack st, int channel) {
        return getChannelIntensities(st, channel);
    }
    
    /**
     * Find the number of pixels within the area of each shell. Formally,
     * calculates the area of each shell in pixel units via the Area() polygon
     * method.
     * 
     * @return
     */
    private long[] findPixelCountPerShell() {
        long[] counts = makeZeroArray();

        for (int i = 0; i < shells.size(); i++) {

            Shell shell = shells.get(i);
            int count = (int) Stats.area(shell.shellRoi);
            counts[i] = count;

        }

        counts = correctNestedIntensities(counts);
        return counts;
    }

    /*
     * 
     * METHODS FOR COUNTING THE INTENSITY OF PIXELS WITHIN A SHELL
     * 
     */

    /**
     * Find the total pixel intensity per shell contained within the component
     * 
     * @param signal the component to measure.
     * @return
     */
    private long[] findPixelIntensityPerShell(CellularComponent signal) {
        return getSignalIntensities(signal);
    }

    /*
     * 
     * METHODS FOR COUNTING THE PROPORTION OF PIXELS WITHIN A SHELL
     * 
     */

    /**
     * Find the proportions of signal within each shell. Note that this will
     * return values for the entire cell.
     *
     * @param signal
     *            the signal to analyse
     * @return an array of signal proportions in each shell
     * @throws Exception
     */
    private double[] findProportionPerShell(ImageStack st, int channel) {

        // Get the pixel intensities per shell for signal channel
        long[] signalDensities = getChannelIntensities(st, channel);

        // find the proportion of the total signal within each shell
        return getProportions(signalDensities);
    }

    /**
     * Find the proportions of signal pixels within each shell. Only consider
     * pixels within the given signal. Does not consider intensities, and does
     * not DAPI normalise.
     *
     * @param signal
     *            the signal to analyse
     * @return an array of signal proportions in each shell
     * @throws Exception
     */
    private double[] findProportionPerShell(CellularComponent signal) throws ShellAnalysisException {

        // Get the pixel intensities per shell for signal channel
        long[] signalDensities = getSignalIntensities(signal);

        // find the proportion of the total signal within each shell
        return getProportions(signalDensities);
    }

    /*
     * 
     * METHODS FOR NORMALISING SIGNALS
     * 
     */

    /**
     * Find the DAPI-normalised signal density per shell
     *
     * @param signals
     *            the proportion of signal per shell
     * @param counterstain
     *            the pixel intensity counts per shell
     * @return a double[] with the normalised signal density per shell, outer to
     *         inner
     */
    private double[] normalise(double[] signals, int[] counterstain) {

        fine("DAPI normalising signals");

        if (signals.length != counterstain.length) {
            throw new IllegalArgumentException("Array lengths are not equal");
        }

        if (signals.length == 0) {
            throw new IllegalArgumentException("Array length is zero");
        }

        for (int i = 0; i < signals.length; i++) {

            if (new Double(signals[i]).isNaN()) {
                warn("Signal is NaN: setting to zero");
                signals[i] = 0;
            }
            if (new Double(counterstain[i]).isNaN()) {
                warn("DAPI is NaN: setting to zero");
                counterstain[i] = 0;
            }
        }

        double[] norm = makeZeroDoubleArray();
        double total = 0;

        // perform the dapi normalisation, and get the signal total
        for (int i = 0; i < signals.length; i++) {
            if (counterstain[i] == 0) {
                norm[i] = 0;
            } else {
                norm[i] = signals[i] / counterstain[i];
            }
            total += norm[i];
        }

        // re-express the normalised signal as a fraction of the total

        double[] result = new double[shells.size()];
        for (int i = 0; i < shells.size(); i++) {

            result[i] = total == 0 // if the total is 0
                    ? 0 // don't try dividing by 0
                    : norm[i] / total; // otherwise get the fraction of the
                                       // total signal
        }

        return result;
    }

    /*
     * PROTECTED AND PRIVATE METHODS
     * 
     */

    /**
     * Get the intensities in each shell for the given channel. Correct for
     * nested shells by removing the total for inner shells from outer shells
     *
     * @return Intensity per shell, outer to inner
     */
    private long[] getChannelIntensities(ImageStack st, int channel) {
        long[] result = makeZeroArray();

        // find the total signal in the signal channel
        for (int i = 0; i < shells.size(); i++) {

            Shell shell = shells.get(i);
            int density = shell.getDensity(st, channel);
            result[i] = density;

        }

        // Correct for nested shells
        result = correctNestedIntensities(result);

        return result;
    }

    /**
     * Get the intensities in each shell for the given channel. Correct for
     * nested shells by removing the total for inner shells from outer shells
     *
     * @return Intensity per shell, outer to inner
     */
    private long[] getSignalIntensities(CellularComponent signal) {
        long[] result = makeZeroArray();

        // find the total signal in the signal channel
        for (int i = 0; i < shells.size(); i++) {

            Shell shell = shells.get(i);
            long density;
            try {
                density = shell.getDensity(signal);
                result[i] = density;
            } catch (UnloadableImageException e) {
                warn("Unable to load image for signal");
                fine("Error loading image", e);
            }
        }

        // Correct for nested shells
        result = correctNestedIntensities(result);

        return result;
    }

    private long[] correctNestedIntensities(long[] array) {

        if (array.length == 0)
            throw new IllegalArgumentException("Array length is zero");

        long innerShellTotal = 0;

        for (int i = shells.size() - 1; i >= 0; i--) {

            long shellTotal = array[i];
            long corrected = shellTotal - innerShellTotal;
            array[i] = corrected;
            innerShellTotal = shellTotal;
        }
        return array;
    }

    /**
     * Create an array with shellCount entries, each set to 0
     * 
     * @return
     */
    private long[] makeZeroArray() {
        long[] result = new long[shells.size()];
        for (int i = 0; i < shells.size(); i++) {
            result[i] = 0;
        }
        return result;
    }

    private double[] makeZeroDoubleArray() {
        double[] result = new double[shells.size()];
        for (int i = 0; i < shells.size(); i++) {
            result[i] = 0;
        }
        return result;
    }

    /**
     * Divide the nucleus into shells of equal area. Since this works by eroding
     * the component shape by 1 pixel, the areas created will not be perfectly
     * equal, but will be at the closest area above or below the target.
     * 
     * @param c
     *            the component to divide
     * @param shellCount
     *            the number of shells to create
     */
    private void createShells(CellularComponent c, int shellCount) {

        // Position of the shells is with respect to the source image
        Roi nucleusRoi = new PolygonRoi(c.toOriginalPolygon(), Roi.POLYGON);
//        fine("Creating shells");

        double initialArea = Stats.area(nucleusRoi);

//        fine("Nucleus area: " + initialArea);
        double target = initialArea / shellCount;
//        fine("Target area per shell: " + target);

        double[] areas = new double[shellCount];

        areas[0] = initialArea;
        shells.add(new Shell(nucleusRoi, c));

        // start with the next shell in nucleus, and shrink shell by shell
        for (int i = 1; i < shellCount; i++) {

            // take the original roi
            Roi shrinkingRoi = (Roi) nucleusRoi.clone();

            // get the maximum faction of the total area this
            // shell should occupy
            // i.e shell 1 of 4 is 1 (all area)
            double maxFraction = (double) (shellCount - i) / (double) shellCount;

            // the max area for this shell
            double maxArea = initialArea * maxFraction;

            double area = initialArea;

            // Converge on the best shrinking factor

            // find the shrnking factor closest to the target area
            double prevDiffToTarget = initialArea - maxArea;

            double diffToTarget = initialArea - maxArea;

            while (diffToTarget > 0) { // allow leeway - it won't be clean

                shrinkingRoi = RoiEnlarger.enlarge(shrinkingRoi, -1);

                area = Stats.area(shrinkingRoi);

                prevDiffToTarget = diffToTarget;
                diffToTarget = area - maxArea;

//                fine("\tShrunk by 1 pixel to " + area + ": Diff to target of "+maxArea+": " + diffToTarget);
            }

            // Correct overspills by enlarging the roi again
            if (Math.abs(prevDiffToTarget) < Math.abs(diffToTarget)) {
                shrinkingRoi = RoiEnlarger.enlarge(shrinkingRoi, 1);
                area = Stats.area(shrinkingRoi);
//                fine("\tIncreasing area if shell by 1 pixel");
            }

            // Make the shell
//            fine("\tFinal shell area: " + area);
            areas[i] = area;
            shells.add(new Shell((Roi) shrinkingRoi.clone(), c));
        }

    }

    /**
     * Find the proportion of the total pixels within each shell.
     *
     * @param counts
     *            the number of pixels for each shell inwards
     * @return an array with the fractions of signal per shell, outer to inner
     */
    private double[] getProportions(long[] counts) {

        if (counts.length == 0)
            throw new IllegalArgumentException("Array length is zero");

        long total = LongStream.of(counts).sum();

        double[] result = makeZeroDoubleArray();

        if (total == 0)
            return result;

        for (int i = 0; i < counts.length; i++) {
            result[i] = (double) counts[i] / (double) total;
        }

        return result;
    }

    private String print(int[] arr) {
        String s = "";
        for (int i : arr) {
            s += i + "\t";
        }
        return s;
    }

    private String print(double[] arr) {
        String s = "";
        for (double i : arr) {
            s += i + "\t";
        }
        return s;
    }

    public class Shell implements Imageable {

        /**
         * The roi of the shell at the original position in the source image of
         * the component from which it was made. Things that are to be compared
         * to it must be offset to the same coordinate space
         */
        private Roi shellRoi;
        private Imageable source;

        public Shell(Roi r, Imageable source) {
            this.shellRoi = r;
            this.source = source;
        }
        
        public Imageable getSource(){
        	return source;
        }

        /**
         * Test if this shell contains the given pixel
         * 
         * @param x
         * @param y
         * @return
         */
        public boolean contains(int x, int y) {
            return shellRoi.contains(x, y);
        }

        /**
         * Count the number of pixels within the signal that are also within
         * this shell.
         * 
         * @param s
         * @param channel
         * @return
         */
        public int getCount(CellularComponent s) {

            Area signalArea = new Area(s.toOriginalShape());
            Area shellArea = this.toArea();

            // Keep pixels that are in both shapes
            signalArea.intersect(shellArea);

            int count = (int) Stats.area(signalArea);

            return count;
        }

        /**
         * Find the sum of all pixel intensities within this shell from the
         * given channel.
         *
         * @param s
         *            the signal with the image to measure
         * @param channel
         *            the channel to measure
         * @return the sum of intensities in the shell
         */
        public int getDensity(ImageStack st, int channel) {

            int stackNumber = ImageImporter.rgbToStack(channel);
            ImageProcessor ip = st.getProcessor(stackNumber);

            int result = getDensity(ip, this.toShape());

            return result;
        }

        /**
         * Find the sum of pixel intensities in the signal channel within this
         * shell which also lie within the given component area.
         *
         * @param s
         *            the signal
         * @return the sum of signal intensities in the signal
         * @throws UnloadableImageException
         */
        public int getDensity(CellularComponent s) throws UnloadableImageException {

            Area componentArea = new Area(s.toOriginalShape());
            Area shellArea = this.toArea();

            ImageStack st;
            try {

                st = new ImageImporter(s.getSourceFile()).importToStack();

            } catch (ImageImportException e) {
                stack("Error importing component image", e);
                throw new UnloadableImageException(
                        "Error importing image source file " + s.getSourceFile().getAbsolutePath(), e);
            }

            int stackNumber = ImageImporter.rgbToStack(s.getChannel());
            ImageProcessor ip = st.getProcessor(stackNumber);

            // Keep pixels that are in both shapes
            componentArea.intersect(shellArea);

            int overlappingArea = (int) Stats.area(componentArea);

            if (overlappingArea == 0)
                return 0;

            int result = getDensity(ip, componentArea);

            return result;
        }

        /**
         * Get the total intensity of pixels within the given shape.
         * 
         * @param ip
         *            the image processor to test
         * @param mask
         *            the shape to test
         * @return
         */
        private int getDensity(ImageProcessor ip, Shape mask) {

            int result = 0;

            Rectangle roiBounds = mask.getBounds();

            int minX = roiBounds.x;
            int maxX = minX + roiBounds.width;

            int minY = roiBounds.y;
            int maxY = minY + roiBounds.height;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {

                    if (mask.contains(x, y)) {
                        if (shellRoi.contains(x, y)) {
                            result += ip.getPixel(x, y);
                        }
                    }

                }
            }

            return result;
        }

        /**
         * Get the position of the shell as described in the CellularComponent
         * interface
         * 
         * @return
         */
        public int[] getPosition() {
            int[] result = { (int) shellRoi.getBounds().getX(), (int) shellRoi.getBounds().getY(),
                    (int) shellRoi.getBounds().getWidth(), (int) shellRoi.getBounds().getHeight() };
            return result;
        }

        /**
         * Get the bounds of the shell
         * 
         * @return
         */
        public Rectangle getBounds() {
            return shellRoi.getBounds();
        }

        public Shape toShape() {
            return shellRoi.getPolygon();
        }

        public Polygon toPolygon() {
            return shellRoi.getPolygon();
        }

        public Area toArea() {
            return new Area(this.toShape());
        }
        
        public Roi toRoi(){
        	return shellRoi;
        }

        public String toString() {

            return this.getBounds().toString();

        }

		@Override
		public IPoint getOriginalBase() {
			return IPoint.makeNew(shellRoi.getXBase(), shellRoi.getYBase());
		}

		@Override
		public int getChannel() {
			return source.getChannel();
		}

		@Override
		public ImageProcessor getImage() throws UnloadableImageException {
			return source.getImage();
		}

		@Override
		public ImageProcessor getRGBImage() throws UnloadableImageException {
			return source.getRGBImage();
		}

		@Override
		public ImageProcessor getComponentImage() throws UnloadableImageException {
			return source.getComponentImage();
		}

		@Override
		public ImageProcessor getComponentRGBImage() throws UnloadableImageException {
			return source.getComponentRGBImage();
		}

		@Override
		public File getSourceFolder() {
			return source.getSourceFolder();
		}

		@Override
		public File getSourceFile() {
			return source.getSourceFile();
		}

		@Override
		public String getSourceFileName() {
			return source.getSourceFileName();
		}

		@Override
		public String getSourceFileNameWithoutExtension() {
			return source.getSourceFileNameWithoutExtension();
		}

		@Override
		public void updateSourceFolder(File newFolder) {		
		}

		@Override
		public void setSourceFile(File sourceFile) {			
		}

		@Override
		public void setChannel(int channel) {
			
		}

		@Override
		public void setSourceFolder(File sourceFolder) {
		}

		@Override
		public IPoint getBase() {
			return IPoint.makeNew(shellRoi.getXBase(), shellRoi.getYBase());
		}

    }

}
