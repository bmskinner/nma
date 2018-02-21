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
  DETECTOR
  -----------------------
  Contains the variables for detecting
  nuclei and signals
*/
package com.bmskinner.nuclear_morphology.analysis.detection;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ByteProcessor;
import ij.process.ByteStatistics;
import ij.process.ColorStatistics;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.PolygonFiller;
import ij.process.ShortProcessor;
import ij.process.ShortStatistics;

/**
 * This abstract class is extended for detecting objects in images. This
 * provides methods for setting size, circularity and thresholding criteria. It
 * calls ImageJ's RoiManager to find the ROIs in the image.
 * 
 * @author bms41
 *
 */
public abstract class Detector implements Loggable {

    public static final int CLOSED_OBJECTS = 0; // Flags to allow detection of
                                                // open or closed objects
    public static final int OPEN_OBJECTS   = 1;

    public static final String COM_X = "XM";
    public static final String COM_Y = "YM";

    public static final String RESULT_TABLE_PERIM = "Perim.";
    
    private static final String NO_IMG_ERR = "No image to analyse";
    private static final String NO_DETECTION_PARAMS_ERR ="Detection parameters not set";
    private static final String SIZE_MISMATCH_ERR = "Minimum size >= maximum size";
    private static final String CIRC_MISMATCH_ERR = "Minimum circularity >= maximum circularity";
    

    private double minSize;
    private double maxSize;
    private double minCirc;
    private double maxCirc;

    private boolean includeHoles = true;
    private boolean excludeEdges = true;

    private int threshold = 128;
    
    /**
     * Set the minimum and maximum size ROIs to detect
     * 
     * @param min
     *            the minimum in pixels
     * @param max
     *            the maximum in pixels
     */
    public void setSize(double min, double max) {
        minSize = min;
        maxSize = max;
    }

    /**
     * Set the minimum and maximum circularity ROIs to detect
     * 
     * @param min
     *            the minimum circularity
     * @param max
     *            the maximum circularity
     */
    public void setCirc(double min, double max) {
        minCirc = min;
        maxCirc = max;
    }

    public void setMinSize(double d) {
        this.minSize = d;
    }
    
    protected double getMinSize(){
        return minSize;
    }

    public void setMaxSize(double d) {
        this.maxSize = d;
    }
    
    protected double getMaxSize(){
        return maxSize;
    }

    public void setMinCirc(double d) {
        this.minCirc = d;
    }

    public void setMaxCirc(double d) {
        this.maxCirc = d;
    }

    public void setThreshold(int i) {
        this.threshold = i;
    }

    /**
     * Set whether the ROIs should include holes - i.e. should holes be flood
     * filled before detection
     * 
     * @param b
     */
    public void setIncludeHoles(boolean b) {
        includeHoles = b;
    }
    
    /**
     * Set whether the ROIs should include holes - i.e. should holes be flood
     * filled before detection
     * 
     * @param b
     */
    public void setExcludeEdges(boolean b) {
        excludeEdges = b;
    }
    
    /**
     * Detect and measure ROIs in this image
     * @param image
     * @return
     */
    protected synchronized Map<Roi, StatsMap> detectRois(@NonNull ImageProcessor image){
        if (image == null) {
            throw new IllegalArgumentException(NO_IMG_ERR);
        }

        if (Double.isNaN(this.minSize) || Double.isNaN(this.maxSize) || Double.isNaN(this.minCirc)
                || Double.isNaN(this.maxCirc)) {
            throw new IllegalArgumentException(NO_DETECTION_PARAMS_ERR);
        }

        if (this.minSize >= this.maxSize) {
            throw new IllegalArgumentException(SIZE_MISMATCH_ERR);
        }
        if (this.minCirc >= this.maxCirc) {
            throw new IllegalArgumentException(CIRC_MISMATCH_ERR);
        }
                
        return findInImage(image);
    }

    /**
     * Get the stats for the region covered by the given roi. Uses the channel
     * previously set.
     * 
     * @param roi
     *            the region to measure
     * @return
     */
    private synchronized StatsMap measure(@NonNull Roi roi, @NonNull ImageProcessor image) {

        ImageProcessor searchProcessor = image.duplicate();
        ImagePlus imp = new ImagePlus(null, searchProcessor);
        imp.setRoi(roi);
        ResultsTable rt = new ResultsTable();
        Analyzer analyser = new Analyzer(imp,
                Measurements.CENTER_OF_MASS | Measurements.AREA | Measurements.PERIMETER | Measurements.FERET, rt);
        analyser.measure();
        StatsMap values = new StatsMap();
        values.add(StatsMap.AREA, rt.getValue(StatsMap.AREA, 0));
        values.add(StatsMap.FERET, rt.getValue(StatsMap.FERET, 0));
        values.add(StatsMap.PERIM, rt.getValue(RESULT_TABLE_PERIM, 0));
        values.add(COM_X, rt.getValue(COM_X, 0));
        values.add(COM_Y, rt.getValue(COM_Y, 0));
        return values;
    }

    /*
     * PRIVATE METHODS
     * 
     * 
     */

    private synchronized Map<Roi, StatsMap> findInImage(@NonNull ImageProcessor image) {

        if (!(image instanceof ByteProcessor || image instanceof ShortProcessor)) {
            throw new IllegalArgumentException("Processor must be byte or short");
        }

        ImageProcessor searchProcessor = image.duplicate();

        searchProcessor.threshold(this.threshold);

        return this.runAnalyser(searchProcessor);
    }
//
//    private synchronized Map<Roi, StatsMap> runAnalyser(ImageProcessor processor) {
//        Map<Roi, StatsMap> list = new HashMap<>();
//
//        ImagePlus image = new ImagePlus(null, processor);
//
//        RoiManager manager = new RoiManager(true);
//        manager.reset(); // RoiManager is initialized as a static instance
//
//        Prefs.blackBackground = true;
//
//        // run the particle analyser
//        // By default, add all particles to the ROI manager, and do not count
//        // anythig touching the edge
//        int options = ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
//        if (includeHoles) {
//            options = options | ParticleAnalyzer.INCLUDE_HOLES;
//        }
//
//        ParticleAnalyzer pa = new ParticleAnalyzer(options, Measurements.FERET, minSize, maxSize, minCirc,
//                maxCirc);
//
//        pa.setRoiManager(manager);
//        boolean success = pa.analyze(image);
//        if (!success) {
//            warn("Unable to perform particle analysis");
//        }
//
//        image.close();
//        for(Roi r : manager.getRoisAsArray()){
//            StatsMap m = measure(r, processor);
//            list.put(r, m);
//        }
//
//
//        manager.reset(); 
//        return list;
//    }

    private synchronized Map<Roi, StatsMap> runAnalyser(ImageProcessor processor) {
        Map<Roi, StatsMap> result = new HashMap<>();

        // run the particle analyser
        // By default, add all particles to the ROI manager, and do not count
        // anything touching the edge
        int options = 0;
        
        if(excludeEdges){
            options = options | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
        }
        
        if (includeHoles) {
            options = options | ParticleAnalyzer.INCLUDE_HOLES;
        }

        ParticleAnalyzer pa = new ParticleAnalyzer(options);


        boolean success = pa.analyze(processor);
        if (!success) {
            warn("Unable to perform particle analysis");
        }

        for(Roi r : pa.getRois()){
            StatsMap m = measure(r, processor);
            result.put(r, m);
        }

        return result;
    }
    

    
    /**
     * This recapitulates the basic function of the ImageJ particle
     * analyzer without using the static roi manager. It works better for
     * multithreading.
     * @author bms41
     * @since 1.13.8
     *
     */
    protected class ParticleAnalyzer implements Measurements {
                
        /** Do not measure particles touching edge of image. */
        public static final int EXCLUDE_EDGE_PARTICLES = 8;

        /** Flood fill to ignore interior holes. */
        public static final int INCLUDE_HOLES = 1024;
        
        
        /** Use 4-connected particle tracing. */
        private static final int FOUR_CONNECTED = 8192;

        
        static final String OPTIONS = "ap.options";
        
        static final int BYTE=0, SHORT=1, FLOAT=2, RGB=3;
        static final double DEFAULT_MIN_SIZE = 0.0;
        static final double DEFAULT_MAX_SIZE = Double.POSITIVE_INFINITY;

        protected static final int NOTHING=0, OUTLINES=1, BARE_OUTLINES=2, ELLIPSES=3, MASKS=4, ROI_MASKS=5,
            OVERLAY_OUTLINES=6, OVERLAY_MASKS=7;

        protected int slice;
        protected boolean processStack;
        protected boolean excludeEdgeParticles,
           floodFill;

        private double level1, level2;

        private int options;
        private int measurements;

        private double fillColor;
        private int width;

        private Wand wand;
        private int imageType, imageType2;
        private int minX, maxX, minY, maxY;

        private PolygonFiller pf;

        private Rectangle r;

        private FloodFiller ff;
        private Polygon polygon;

        private int roiType;
        private int wandMode = Wand.LEGACY_MODE;

        boolean blackBackground;
        
        private final Set<Roi> rois = new HashSet<>();

                
        /** Constructs a ParticleAnalyzer.
            @param options  a flag word created by Oring SHOW_RESULTS, EXCLUDE_EDGE_PARTICLES, etc.
        */
        public ParticleAnalyzer(int options) {
            this.options = options;
            this.measurements = Measurements.FERET;

            slice = 1;

            if ((options&FOUR_CONNECTED)!=0) {
                wandMode = Wand.FOUR_CONNECTED;
                options |= INCLUDE_HOLES;
            }
        }
        
        /**
         * Get the detected Rois
         * @return
         */
        public Set<Roi> getRois(){
            return rois;
        }
        

        /** Performs particle analysis on the specified ImagePlus and
            ImageProcessor. Returns false if there is an error. */
        public boolean analyze(ImageProcessor ip) {
            rois.clear();

            excludeEdgeParticles = (options&EXCLUDE_EDGE_PARTICLES)!=0;
            floodFill = (options&INCLUDE_HOLES)==0;

            ip.snapshot();

            if (!setThresholdLevels(ip))
                return false;
            width = ip.getWidth();
            
            byte[] pixels = null;
            if (ip instanceof ByteProcessor)
                pixels = (byte[])ip.getPixels();
            if (r==null) {
                r = ip.getRoi();
            }
            minX=r.x; maxX=r.x+r.width; minY=r.y; maxY=r.y+r.height;

            int offset;
            double value;

            wand = new Wand(ip);
            pf = new PolygonFiller();
            if (floodFill) {
                ImageProcessor ipf = ip.duplicate();
                ipf.setValue(fillColor);
                ff = new FloodFiller(ipf);
            }
            roiType = Wand.allPoints()?Roi.FREEROI:Roi.TRACED_ROI;

            boolean done = false;
            for (int y=r.y; y<(r.y+r.height); y++) {
                offset = y*width;
                for (int x=r.x; x<(r.x+r.width); x++) {
                    if (pixels!=null)
                        value = pixels[offset+x]&255;
                    else if (imageType==SHORT)
                        value = ip.getPixel(x, y);
                    else
                        value = ip.getPixelValue(x, y);
                    if (value>=level1 && value<=level2 && !done) {
                        analyzeParticle(x, y, ip);
                        done = level1==0.0&&level2==255.0;
                    }
                }
            }

            ip.resetRoi();
            ip.reset();

            return true;
        }
        
       private boolean setThresholdLevels(ImageProcessor ip) {
            double t1 = ip.getMinThreshold();
            double t2 = ip.getMaxThreshold();

            if (ip instanceof ShortProcessor)
                imageType = SHORT;
            else if (ip instanceof FloatProcessor)
                imageType = FLOAT;
            else
                imageType = BYTE;
            if (t1==ImageProcessor.NO_THRESHOLD) {

                if (imageType!=BYTE)
                    return false;

                boolean threshold255 = false;
                if (Prefs.blackBackground)
                    threshold255 = !threshold255;
                if (threshold255) {
                    level1 = 255;
                    level2 = 255;
                    fillColor = 64;
                } else {
                    level1 = 0;
                    level2 = 0;
                    fillColor = 192;
                }
            } else {
                level1 = t1;
                level2 = t2;
                if (imageType==BYTE) {
                    if (level1>0)
                        fillColor = 0;
                    else if (level2<255)
                        fillColor = 255;
                } else if (imageType==SHORT) {
                    if (level1>0)
                        fillColor = 0;
                    else if (level2<65535)
                        fillColor = 65535;
                } else if (imageType==FLOAT)
                        fillColor = -Float.MAX_VALUE;
                else
                    return false;
            }
            imageType2 = imageType;

            return true;
        }
                
       private void analyzeParticle(int x, int y, ImageProcessor ip) {

            wand.autoOutline(x, y, level1, level2, wandMode);
            if (wand.npoints==0)
                {return;}
            Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, roiType);
            Rectangle r = roi.getBounds();
            if (r.width>1 && r.height>1) {
                PolygonRoi proi = (PolygonRoi)roi;
                pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(), proi.getNCoordinates());
                ip.setMask(pf.getMask(r.width, r.height));
                if (floodFill) ff.particleAnalyzerFill(x, y, level1, level2, ip.getMask(), r);
            }
            ip.setRoi(r);
            ip.setValue(fillColor);
            ImageStatistics stats = getStatistics(ip, measurements);
            boolean include = true;
            if (excludeEdgeParticles) {
                if (r.x==minX||r.y==minY||r.x+r.width==maxX||r.y+r.height==maxY)
                    include = false;
                if (polygon!=null) {
                    Rectangle bounds = roi.getBounds();
                    int x1=bounds.x+wand.xpoints[wand.npoints-1];
                    int y1=bounds.y+wand.ypoints[wand.npoints-1];
                    int x2, y2;
                    for (int i=0; i<wand.npoints; i++) {
                        x2=bounds.x+wand.xpoints[i];
                        y2=bounds.y+wand.ypoints[i];
                        if (!polygon.contains(x2, y2))
                            {include = false; break;}
                        if ((x1==x2 && ip.getPixel(x1,y1-1)==fillColor) || (y1==y2 && ip.getPixel(x1-1,y1)==fillColor))
                            {include = false; break;}
                        x1=x2; y1=y2;
                    }
                }
            }
            ImageProcessor mask = ip.getMask();
            if (minCirc>0.0 || maxCirc<1.0) {
                double perimeter = roi.getLength();
                double circularity = perimeter==0.0?0.0:4.0*Math.PI*(stats.pixelCount/(perimeter*perimeter));
                if (circularity>1.0) circularity = 1.0;

                if (circularity<minCirc || circularity>maxCirc) include = false;
            }
            if (stats.pixelCount>=minSize && stats.pixelCount<=maxSize && include) {
                stats.xstart=x; stats.ystart=y;
                rois.add( (Roi) roi.clone());
            }

            ip.fill(mask);
        }

       private ImageStatistics getStatistics(ImageProcessor ip, int mOptions) {
            
            Calibration cal = new Calibration();
            switch (imageType2) {
                case BYTE:
                    return new ByteStatistics(ip, mOptions, cal);
                case SHORT:
                    return new ShortStatistics(ip, mOptions, cal);
                case FLOAT:
                    return new FloatStatistics(ip, mOptions, cal);
                case RGB:
                    return new ColorStatistics(ip, mOptions, cal);
                default:
                    return null;
            }
        }
        

    }
}
