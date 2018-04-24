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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.ImageStack;
import ij.Prefs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.RoiScaler;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
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
    
    private final int nShells;
    private final ShrinkType type;
    private static final int DEFAULT_SCALE_FACTOR = 4;
    private boolean isScale = false;

    /**
     * The shell ROIs within the template object. This list begins with the
     * largest shell (index 0) and ends with the smallest shell. The larger
     * shells include the area contained within smaller shells.
     */
    private List<Shell> shells = new ArrayList<Shell>(0);


    /**
     * @param component the component to analyse
     * @param type the method used to generate the shells
     * @param isScale should the component be scaled up for more precise shell creation
     * @throws ShellAnalysisException
     */
    public ShellDetector(@NonNull CellularComponent component, @NonNull ShrinkType type, boolean isScale) throws ShellAnalysisException {
        this(component, ShellDetector.DEFAULT_SHELL_COUNT, type, isScale);
    }

    /**
     * Create shells in the given nucleus, using the given shell count
     *
     * @param component the component to analyse
     * @param shellCount the number of shells to create
     * @param type the method used to generate the shells
     * @param isScale should the component be scaled up for more precise shell creation
     * @throws ShellAnalysisException
     */
    public ShellDetector(@NonNull CellularComponent component, int shellCount, @NonNull ShrinkType type, boolean isScale) throws ShellAnalysisException {
        nShells = shellCount;
        this.type = type;
        this.isScale = isScale;
        component.getBounds().getX();
        fine("Creating shell for component at "+ component.getBounds().getX()+" - "+ component.getBounds().getY() );
        createShells(component);
    }

    /**
     * Get the shells created
     * 
     * @return
     */
    public List<Shell> getShells() {
        return shells;
    }

    /**
     * Find the shell in the template object that the given point lies within,
     * or -1 if the point is not found
     * 
     * @param p
     * @return
     */
    public int findShell(@NonNull IPoint p) {

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
     * Find the number of pixels of the component within each shell
     * 
     * @param signal
     * @return
     */
    public long[] findPixelCounts(@NonNull CellularComponent component) {
        long[] counts = makeZeroArray();
        for (int i=0; i<shells.size(); i++) {
            Shell shell = shells.get(i);
            counts[i] = shell.getPixelCount(component);
        }
        return correctNestedIntensities(counts);
    }
    
    /**
     * Find the number of pixels within the area of each shell.
     * 
     * @return
     */
    public long[] findPixelCounts() {
        long[] result = makeZeroArray();
        for (int i=0; i<shells.size(); i++) {
            Shell shell = shells.get(i);
            result[i] = shell.getPixelCount();
        }
        return correctNestedIntensities(result);
    }
    
    /**
     * Find the total pixel intensity per shell contained within the component
     * 
     * @param signal the component to measure.
     * @return
     */
    public long[] findPixelIntensities(@NonNull CellularComponent component) {
        long[] result = makeZeroArray();
        try {
            for (int i = 0; i < shells.size(); i++) {
                Shell shell = shells.get(i);
                result[i] = shell.getPixelIntensity(component);
            }
        } catch (UnloadableImageException e) {
            warn("Unable to load image for signal");
            fine("Error loading image", e);
            return makeZeroArray();
        }
        result = correctNestedIntensities(result);
        System.out.println(Arrays.toString(result));
        return result;
    }


    /**
     * Count the total pixel intensity in each shell for the given image
     * 
     * @param st the image stack to analyse
     * @param channel the RGB channel in the stack
     * @return
     */
    public long[] findPixelIntensities(@NonNull ImageStack st, int channel) {
        long[] result = makeZeroArray();
        for (int i = 0; i < shells.size(); i++) {
            Shell shell = shells.get(i);
            result[i] = shell.getPixelIntensity(st, channel);
        }
        return correctNestedIntensities(result);
    }
    
    /*
     * PROTECTED AND PRIVATE METHODS
     * 
     */
    
    /**
     * Assuming each interior shell is a subset of the preceding shell,
     * subtract the interior values to leave only the counts for the shell. 
     * @param array
     * @return
     */
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
    private void createShells(@NonNull CellularComponent c) {

        // Position of the shells is with respect to the source image
        Roi objectRoi = new PolygonRoi(c.toOriginalPolygon(), Roi.POLYGON);
        double[] areas = new double[nShells];
        double[] ratios = new double[nShells];
        // First shell encloses the entire object        
//        areas[0] = Stats.area(objectRoi);
//        shells.add(new Shell(objectRoi, c));
        
        RoiShrinker re = new RoiShrinker((Roi)objectRoi.clone());

        // start with the next shell in nucleus, and shrink shell by shell
        for (int i=0; i<nShells; i++) {
            
            // this approach makes shells of equal radius, not equal area. 
            // Is that more or less useful?
            Roi shrinkingRoi = re.shrink(type, i);

            // Make the shell
            areas[i] = Stats.area(shrinkingRoi);
            ratios[i] = areas[i]/areas[0];
            shells.add(new Shell(shrinkingRoi, c));
        }
        fine("Shells at "+ objectRoi.getBounds().getX()+" - "+ objectRoi.getBounds().getY() );
        fine("Areas: "+Arrays.toString(areas));
        fine("Ratios: "+Arrays.toString(ratios));

    }

    public class Shell implements Imageable {

        /**
         * The roi of the shell at the original position in the source image of
         * the component from which it was made. Things that are to be compared
         * to it must be offset to the same coordinate space
         */
        private Roi shellRoi;
        private Imageable source;

        /**
         * Create a shell from the given roi, with the given source object
         * @param r
         * @param source
         */
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
        
        public boolean contains(IPoint p){
            return shellRoi.contains(p.getXAsInt(), p.getYAsInt());
        }
        
        /**
         * Find the number of pixels within this shell
         *
         * @return the count of pixels in this shell 
         */
        public long getPixelCount() {
            long count = getPixelCount(this.toRoi());
            return count;
        }

        /**
         * Count the number of pixels within the object that are also within
         * this shell.
         * 
         * @param s the component
         * @return
         */
        public long getPixelCount(CellularComponent s) {
            long count = getPixelCount(s.toRoi());
            return count;
        }
        
        /**
         * Get the total number of pixels within the given shape and this shell
         * 
         * @param mask the shape to test
         * @return
         */
        private long getPixelCount(@NonNull Roi mask) {

            int result = 0;

            Rectangle roiBounds = mask.getBounds();

            int minX = roiBounds.x;
            int maxX = minX + roiBounds.width;

            int minY = roiBounds.y;
            int maxY = minY + roiBounds.height;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (mask.contains(x, y) && shellRoi.contains(x, y))
                        result ++;
                }
            }

            return result;
        }

        /**
         * Find the sum of all pixel intensities within this shell from the
         * given channel.
         *
         * @param st the stack to measure
         * @param channel within the stack to measure
         * @return the sum of intensities in the shell
         */
        private long getPixelIntensity(ImageStack st, int channel) {
            int stackNumber = ImageImporter.rgbToStack(channel);
            ImageProcessor ip = st.getProcessor(stackNumber);
//            if(isScale)
//                ip = ip.resize(ip.getWidth()*DEFAULT_SCALE_FACTOR);
            return getPixelIntensity(ip, this.toShape());
        }

        /**
         * Find the sum of pixel intensities in the signal channel within this
         * shell which also lie within the given component area.
         *
         * @param s the component
         * @return the sum of signal intensities in the signal
         * @throws UnloadableImageException
         */
        private long getPixelIntensity(CellularComponent s) throws UnloadableImageException {
            try {
                ImageStack st = new ImageImporter(s.getSourceFile()).importToStack();
                int stackNumber = ImageImporter.rgbToStack(s.getChannel());
                ImageProcessor ip = st.getProcessor(stackNumber);
                return getPixelIntensity(ip, s.toOriginalShape());
            } catch (ImageImportException e) {
                e.printStackTrace();
                throw new UnloadableImageException(
                        "Error importing image source file " + s.getSourceFile().getAbsolutePath(), e);
            }
        }


        /**
         * Get the total intensity of pixels within the given shape.
         * 
         * @param ip the image processor to test
         * @param mask the shape to test
         * @return
         */
        private long getPixelIntensity(@NonNull ImageProcessor ip, @NonNull Shape mask) {

            long result = 0;

            Rectangle roiBounds = mask.getBounds();

            int minX = roiBounds.x;
            int maxX = minX + roiBounds.width;

            int minY = roiBounds.y;
            int maxY = minY + roiBounds.height;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (mask.contains(x, y) && shellRoi.contains(x, y))
                        result += ip.getPixel(x, y);
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

    
    /**
     * This is a replacement for the default ImageJ RoiEnlarger.
     * It is spcifically designed to erode the roi for shell analysis,
     * using the fixed number of shells to set threshold in the EDM
     * @author bms41
     * @since 1.13.8
     *
     */
    private class RoiShrinker {
        
        private final Roi roi;
        private final Rectangle bounds;
        
        public RoiShrinker(@NonNull Roi r){
            roi = r;
            bounds = roi.getBounds();
        }

        /**
         * Shrink the roi to match the given shell
         * @param type the type of shrinking to apply
         * @param shell the shell to fetch
         * @return
         */
        public Roi shrink(@NonNull ShrinkType type, int shell){
            switch(type){
                case RADIUS: return shrinkByRadius(shell);
                case AREA:   return shrinkByArea(shell);
                default:     return shrinkByRadius(shell);
            }
        }

        private Roi shrinkByRadius(int shell) {
            if(shell==0)
                return roi;

            double ratio = (double) (shell)  / (double) nShells;

            ImageProcessor ip = createFromRoi();
            boolean bb = Prefs.blackBackground;
            Prefs.blackBackground = true;
            
            
            final ImageProcessor ip1 = ip.duplicate();
            new EDM().toEDM(ip1); // zero at edge, 255 at centre
            int max = getMaxPixelValue(ip1); // the EDM may not use all 255 values 
            
            int threshold = (int) Math.round(ratio * max);          
            final ImageProcessor newIp = setThreshold(ip1, threshold);
                        
            Prefs.blackBackground = bb;
            newIp.setThreshold(threshold, 255, ImageProcessor.NO_LUT_UPDATE);

            Roi roi2 = (new ThresholdToSelection()).convert(newIp);
            Rectangle bounds2 = roi2.getBounds();
            if (bounds2.width<=0 && bounds2.height<=0)
                return roi;
            roi2.setLocation(bounds.x+bounds2.x-1, bounds.y+bounds2.y-1);           
            
            return roi2;
        }
        
        
        private Roi shrinkByArea(int shell) {
//            if(shell==0)
//                return roi;
            
            // scale the image such that the EDM can be calculated

            double ratio = (double) (nShells-shell)  / (double) nShells;
            
            ImageProcessor ip = createFromRoi();
//            if(isScale)
//                ip = ip.resize(ip.getWidth()*DEFAULT_SCALE_FACTOR);
            boolean bb = Prefs.blackBackground;
            Prefs.blackBackground = true;

            final ImageProcessor ip1 = ip.duplicate();

            new EDM().toEDM(ip1); // zero at edge, 255 at centre
            
            int threshold = 0;
            Roi roi2 = (Roi) roi.clone();
            double area = Stats.area(roi2);
            double scaledArea = area;
            double desiredArea = scaledArea*ratio;

            
            while(scaledArea>desiredArea || threshold==0) { // ensure we go through at least once
                final ImageProcessor newIp = ip1.duplicate();
                newIp.setThreshold(threshold, 255, ImageProcessor.NO_LUT_UPDATE);
                roi2 = (new ThresholdToSelection()).convert(newIp);
                scaledArea = Stats.area(roi2);
                threshold++;
            }
            fine("Shell "+shell+" Ratio: "+ratio+" Thresh: "+threshold+" Des: "+desiredArea+" Act:"+scaledArea);
            Prefs.blackBackground = bb;
            
            Rectangle bounds2 = roi2.getBounds();
            if (bounds2.width<=0 && bounds2.height<=0)
                return roi;
            roi2.setLocation(bounds.x+bounds2.x-1, bounds.y+bounds2.y-1);           
            return roi2;

        }
        
        private ImageProcessor createFromRoi() {
            int width = bounds.width + 2;
            int height = bounds.height + 2;
            final ImageProcessor ip = new ByteProcessor(width, height);
            roi.setLocation(1, 1);
            ip.setColor(255);
            ip.fill(roi);
            roi.setLocation(bounds.x, bounds.y);
            return ip;
        }
        
        private ImageProcessor setThreshold(ImageProcessor ip, int threshold) {
        	ImageProcessor newIp = new ByteProcessor(ip.getWidth(), ip.getHeight());
        	for(int i=0; i<ip.getWidth()*ip.getHeight(); i++){
                    int value = ip.get(i);
                    newIp.set(i, value>=threshold?value:0);
            }
        	return newIp;
        }
        
        private int getMaxPixelValue(ImageProcessor ip){
            int max = 0;
            for(int i=0; i<ip.getWidth()*ip.getHeight(); i++){
                int value = ip.get(i);
                max = value>max?value:max;
            }
            return max;
        }
    }

}
