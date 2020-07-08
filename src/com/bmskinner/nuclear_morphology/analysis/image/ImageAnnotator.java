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
package com.bmskinner.nuclear_morphology.analysis.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

/**
 * Draw components and features on image processors.
 * 
 * @author ben
 *
 */
public class ImageAnnotator extends AbstractImageFilterer {
	
	private static final Logger LOGGER = Logger.getLogger(ImageAnnotator.class.getName());
    
    private static final int BORDER_TAG_POINT_SIZE = 7;
	private static final int RP_POINT_SIZE = 9;
	/** Converts resized images to original image dimensions */
    private double scale = 1;

    public ImageAnnotator(final ImageProcessor ip) {
        super(ip);
    }
    
    /**
     * Get the conversion scale between the annotated image width and the input image width
     * @return the scale factor
     */
    protected double getScale() { return scale; }
    
    /**
     * Create using the given image, and rescale all annotations
     * such that the image fits the given dimensions preserving
     * aspect ratio
     * @param ip the input image processor
     * @param maxWidth the maximum width the output image can be
     * @param maxHeight the maximum height the output image can be
     */
    public ImageAnnotator(final ImageProcessor ip, final int maxWidth, final int maxHeight) {
        super(ip);
        int originalWidth = ip.getWidth();
        int originalHeight = ip.getHeight();

        // keep the image aspect ratio
        double ratio = (double) originalWidth / (double) originalHeight;

        double finalWidth = maxHeight * ratio; // fix height
        finalWidth = finalWidth > maxWidth ? maxWidth : finalWidth; // but
                                                                    // constrain
                                                                    // width too
        
        scale = finalWidth / originalWidth;
        ImageProcessor result = ip.duplicate().resize((int) finalWidth);
        this.ip = result;
    }

    /**
     * Draw the borders of all cell components. Assumes that the image contains
     * the original coordinates of the cell
     * 
     * @param cell
     * @return
     */
    final public ImageAnnotator annotateCellBorders(final ICell cell) {

        if (cell.hasCytoplasm()) {
            annotateBorder(cell.getCytoplasm(), Color.CYAN);
        }

        for (Nucleus n : cell.getNuclei()) {
            annotateBorder(n, Color.ORANGE);
            if (n instanceof LobedNucleus) {
                for (Lobe l : ((LobedNucleus) n).getLobes()) {
                    annotateBorder(l, Color.GREEN);
                    annotatePoint(l.getCentreOfMass(), Color.GREEN);
                }
            }
        }
        return this;
    }
    
    public ImageAnnotator annotateOutlineOnCroppedComponent(final CellularComponent n, Color outlineColour, int strokeWeight) {
    	 for(IBorderPoint p : n.getBorderList()) 
         	annotatePoint(p.plus(Imageable.COMPONENT_BUFFER), outlineColour, strokeWeight);
         return this;
    }
    
    /**
     * Draw the outline of the given nucleus and any signals marked.
     * The image is assumed to be cropped to the nuclear border.
     * 
     * @param n the nucleus to draw
     * @return this annotator
     */
    public ImageAnnotator annotatesignalsOnCroppedNucleus(final Nucleus n) {
        
        try {
        	annotateOutlineOnCroppedComponent(n, Color.BLACK, 3);
            annotatePoint(n.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER), Color.PINK, RP_POINT_SIZE);
            annotateSignals(n);

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error annotating nucleus", e);
        }
        return this;
    }
    
    /**
     * Draw the outline of the given nucleus and any signals marked.
     * The image is assumed to be cropped to the nuclear border.
     * 
     * @param n the nucleus to draw
     * @return this annotator
     */
    public ImageAnnotator annotateSegmentsOnCroppedNucleus(@NonNull Nucleus n) {
        
        try {
            for(IBorderPoint p : n.getBorderList()) {
            	annotatePoint(p.plus(Imageable.COMPONENT_BUFFER), Color.BLACK, 3);
            }
            
            // Draw lines for the border tags            
            
            annotateLine(n.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER), 
                    n.getBorderPoint(Tag.REFERENCE_POINT).plus(Imageable.COMPONENT_BUFFER), 
                    Color.ORANGE, 3);
            annotateLine(n.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER)
                    , n.getBorderPoint(Tag.ORIENTATION_POINT).plus(Imageable.COMPONENT_BUFFER)
                    , Color.BLUE, 3);
            
            if(n.hasBorderTag(Tag.TOP_VERTICAL) && n.hasBorderTag(Tag.BOTTOM_VERTICAL)){
                annotateLine(n.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER)
                        , n.getBorderPoint(Tag.TOP_VERTICAL).plus(Imageable.COMPONENT_BUFFER)
                        , Color.GREEN, 3);
                annotateLine(n.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER)
                        , n.getBorderPoint(Tag.BOTTOM_VERTICAL).plus(Imageable.COMPONENT_BUFFER)
                        , Color.GREEN, 3);
            }
            
            // Colour the border points for segments    
            ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
            if (profile.hasSegments()) { 

            	for(IBorderSegment seg : profile.getOrderedSegments()) {
            		Paint color = ColourSelecter.getColor(seg.getPosition(), GlobalOptions.getInstance().getSwatch());
            		Iterator<Integer> it = seg.iterator();
            		int lastIndex = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, seg.getEndIndex());
            		while(it.hasNext()) {
            			int index = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, it.next());
            			IPoint p = n.getBorderPoint(index).plus(Imageable.COMPONENT_BUFFER);
            			// since segments overlap, draw the last index larger so the next segment can overlay
            			annotatePoint(p, (Color) color, lastIndex==index ? 5 : 3);
            		}
            	}
            	
            	// Draw the RP again because it will be drawn over by the final segment
            	IPoint rp = n.getBorderPoint(Tag.REFERENCE_POINT).plus(Imageable.COMPONENT_BUFFER);
            	annotatePoint(rp, ColourSelecter.getColor(0), 3);
            }
            annotatePoint(n.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER), Color.PINK, RP_POINT_SIZE);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error annotating nucleus", e);
        }
        return this;
    }

    /**
     * Draw the outline of the given nucleus, with the OP, RP, CoM, feret,
     * segments and any signals markerd
     * 
     * @param n
     *            the nucleus to draw
     * @return the annotator
     */
    public ImageAnnotator annotateNucleus(Nucleus n) {

        try {

            annotateOP(n);
            annotateRP(n);
            annotateCoM(n);
            annotateMinFeret(n);
            annotateSegments(n);
            annotateSignals(n);

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error annotating nucleus", e);

        }
        return this;
    }
    
    /**
     * Draw the given shell. Shells are assumed to be at the shell location
     * in the source image ofthe component
     * @param shell
     * @return
     */
    public ImageAnnotator annotate(ShellDetector.Shell shell, Color colour){
    	
    	// This is in source image coordinates
    	Roi roi = shell.toRoi();
    	
    	IPoint diff = shell.getSource().getOriginalBase().minus(shell.getOriginalBase());
    	
    	
    	
    	// add an adjustment for the buffer, and for the location within the nucleus
    	
    	IPoint base = shell.getSource().getBase().plus(Imageable.COMPONENT_BUFFER).minus(diff);
    	
    	roi.setLocation(base.getX(), base.getY());
    	annotateRoi(roi, colour, 1);
    	return this;
    }

    /**
     * Draw a point on the image processor with the default size
     * 
     * @param p the point to draw
     * @param c the colour to draw
     * @return the annotator
     */
    public ImageAnnotator annotatePoint(IPoint p, Color c) {
    	return annotatePoint(p, c, 3);
    }
    
    /**
     * Draw a point on the image processor
     * 
     * @param p the point to draw
     * @param c the colour to draw
     * @param size the point size
     * @return the annotator
     */
    public ImageAnnotator annotatePoint(IPoint p, Color c, int size) {

        if (p.getXAsInt() < 0 || p.getXAsInt() > ip.getWidth())
            throw new IllegalArgumentException("Point x "+p.getXAsInt()+" is out of image bounds (max width "+ip.getWidth()+")");

        if (p.getYAsInt() < 0 || p.getYAsInt() > ip.getHeight())
            throw new IllegalArgumentException("Point y "+p.getYAsInt()+" is out of image bounds (max height "+ip.getHeight()+")");

        ip.setColor(c);
        ip.setLineWidth(size);
        ip.drawDot( (int) (p.getX()*scale), (int) (p.getY()*scale));
        return this;
    }
    
    

    /**
     * Draw the border of the given component on the component image of the
     * template. These can be the same object.
     * 
     * @param p the points to draw
     * @param template the component to get the component image from
     * @param c the colour to draw the border
     * @return this annotator
     */
    public ImageAnnotator annotatePoint(IPoint p, Imageable template, Color c) {
        IPoint offset = Imageable.translateCoordinateToComponentImage(p, template);
        return annotatePoint(offset, c);
    }

    /**
     * Draw a line on the image processor with the default width
     * 
     * @param p1 the first endpoint
     * @param p1 the second endpoint
     * @param c the colour to draw
     * @return the annotator
     */
    private ImageAnnotator annotateLine(IPoint p1, IPoint p2, Color c) {
        return annotateLine(p1, p2, c, 1);
    }
    
    /**
     * Draw a line on the image processor
     * 
     * @param p1 the first endpoint
     * @param p1 the second endpoint
     * @param c the colour to draw
     * @param width the line width
     * @return the annotator
     */
    private ImageAnnotator annotateLine(IPoint p1, IPoint p2, Color c, int width) {
        ip.setColor(c);
        ip.setLineWidth(width);
        ip.drawLine( (int) (p1.getXAsInt()*scale), 
                (int) (p1.getYAsInt()*scale), 
                (int) (p2.getXAsInt()*scale), 
                (int) (p2.getYAsInt()*scale));
        return this;
    }

    private ImageAnnotator annotatePolygon(PolygonRoi p, Paint c) {
        ip.setColor((Color) c);
        ip.setLineWidth(2);
        ip.draw(p);
        return this;
    }
    
//    private ImageAnnotator annotateRoi(Roi p, Paint c) {
//        return annotateRoi(p, c, 2);
//    }
    
    private ImageAnnotator annotateRoi(Roi p, Paint c, int width) {
        ip.setColor((Color) c);
        ip.setLineWidth(width);
        ip.draw(p);
        return this;
    }

    /**
     * Draw the orientation point of a nucleus in cyan. For other colours use
     * {@link ImageAnnotator#annotatePoint} directly
     * 
     * @param n
     *            the nucleus
     * @return the annotator
     */
    public ImageAnnotator annotateOP(Nucleus n) {
        try {
            return annotatePoint(n.getBorderPoint(Tag.ORIENTATION_POINT), Color.CYAN);
        } catch (UnavailableBorderTagException e) {
            LOGGER.log(Loggable.STACK, "Cannot find border tag OP", e);
        }
        return this;
    }

    /**
     * Draw the reference point of a nucleus in yellow. For other colours use
     * {@link ImageAnnotator#annotatePoint} directly
     * 
     * @param n the nucleus
     * @return the annotator
     */
    public ImageAnnotator annotateRP(Nucleus n) {
        try {
            return annotatePoint(n.getBorderPoint(Tag.REFERENCE_POINT), Color.YELLOW);
        } catch (UnavailableBorderTagException e) {
            LOGGER.log(Loggable.STACK, "Cannot find border tag RP", e);
            return this;
        }
    }

    /**
     * Draw the centre of mass of a component in magenta. For other colours use
     * {@link ImageAnnotator#annotatePoint} directly
     * 
     * @param n
     *            the component
     * @return the annotator
     */
    public ImageAnnotator annotateCoM(CellularComponent n) {
        return annotatePoint(n.getCentreOfMass(), Color.MAGENTA);
    }

    /**
     * Draw the outline of the component in the given colour at the original
     * position
     * 
     * @param n the component
     * @param c the colour
     * @return the annotator
     */
    public ImageAnnotator annotateBorder(final CellularComponent n, final Color c) {
        FloatPolygon p = n.toOriginalPolygon();
        PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);

        return annotatePolygon(roi, c);
    }

    /**
     * Draw the border of the given component on the component image of the
     * template. These can be the same object.
     * 
     * @param n the component to draw
     * @param template the component to get the component image from
     * @param c the colour to draw the border
     * @return this annotator
     */
    public ImageAnnotator annotateBorder(final CellularComponent n, final Imageable template, final Color c) {
        FloatPolygon p = n.toOriginalPolygon();
        PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);

        IPoint base = n.getOriginalBase();
        IPoint offset = Imageable.translateCoordinateToComponentImage(base, template);
        roi.setLocation(offset.getX(), offset.getY());

        return annotatePolygon(roi, c);
    }

    /**
     * Annotate the image with the given text. The text background is white and
     * the text colour is black
     * 
     * @param x the string x
     * @param y the string y
     * @param s the text
     * @return
     */
    public ImageAnnotator annotateString(int x, int y, String s) {
        return this.annotateString(x, y, s, Color.BLACK);
    }

    /**
     * Annotate the image with the given text. The text background is white.
     * 
     * @param x the string x
     * @param y the string y
     * @param s the text
     * @param text the text colour
     * @return
     */
    public ImageAnnotator annotateString(int x, int y, String s, Color text) {
        return this.annotateString(x, y, s, text, Color.WHITE);
    }

    /**
     * Annotate the image with the given text.
     * 
     * @param x the string x
     * @param y the string y
     * @param s the text
     * @param text the text colour
     * @param back the background colour
     * @return
     */
    public ImageAnnotator annotateString(int x, int y, String s, Color text, Color back) {

        ip.setFont(new Font("SansSerif", Font.PLAIN, 20)); // TODO - choose text
                                                           // size based on
                                                           // image size
        ip.setColor(text);
        ip.drawString(s, x, y, back);
        return this;
    }

    /**
     * Draw the size and shape values over the CoM of the component.
     * 
     * @param n the component to draw
     * @param text the colour of the text
     * @param back the colour of the background
     * @return the annotator
     */
    public ImageAnnotator annotateStats(CellularComponent n, Color text, Color back) {

        DecimalFormat df = new DecimalFormat("#.##");

        String areaLbl;
        String perimLbl;

        double circ;
        double area;

        if (n instanceof INuclearSignal) {

            area = n.getStatistic(PlottableStatistic.AREA);
            double perim2 = Math.pow(n.getStatistic(PlottableStatistic.PERIMETER), 2);
            circ = (4 * Math.PI) * (area / perim2);

        } else {
            area = n.getStatistic(PlottableStatistic.AREA);
            circ = n.getStatistic(PlottableStatistic.CIRCULARITY);
        }

        areaLbl = "Area: " + df.format(area);
        perimLbl = "Circ: " + df.format(circ);
        String label = areaLbl + "\n" + perimLbl;

        return this.annotateString(n.getOriginalCentreOfMass().getXAsInt(), n.getOriginalCentreOfMass().getYAsInt(),
                label, text, back);
    }

    /**
     * Draw the size and shape values over the CoM of the component
     * 
     * @param n the component to draw
     * @param c the color of the text
     * @return
     */
    public ImageAnnotator annotateSignalStats(CellularComponent parent, CellularComponent signal, Color text,
            Color back) {

        DecimalFormat df = new DecimalFormat("#.##");

        String areaLbl;
        String perimLbl;

        double circ = 0;
        double area = 0;
        double fraction;

        if (signal instanceof INuclearSignal) {

            area = signal.getStatistic(PlottableStatistic.AREA);
            double perim2 = Math.pow(signal.getStatistic(PlottableStatistic.PERIMETER), 2);
            circ = (4 * Math.PI) * (area / perim2);

        }

        fraction = area / parent.getStatistic(PlottableStatistic.AREA);

        areaLbl = "Area: " + df.format(area);
        perimLbl = "Circ: " + df.format(circ);
        String fractLabel = "Fract: " + df.format(fraction);

        String label = areaLbl + "\n" + perimLbl + "\n" + fractLabel;

        return this.annotateString(signal.getOriginalCentreOfMass().getXAsInt(),
                signal.getOriginalCentreOfMass().getYAsInt(), label, text, back);

    }

    /**
     * Draw the feret diameter of a profilable component in magenta. For other
     * colours use {@link ImageAnnotator#annotateLine} directly
     * 
     * @param n
     *            the component
     * @return the annotator
     */
    public ImageAnnotator annotateMinFeret(Profileable n) {
        int minIndex;
        try {
            minIndex = n.getProfile(ProfileType.DIAMETER).getIndexOfMin();
            IBorderPoint narrow1 = n.getBorderPoint(minIndex);
            IBorderPoint narrow2 = n.findOppositeBorder(narrow1);
            return annotateLine(narrow1, narrow2, Color.MAGENTA);
        } catch (UnavailableProfileTypeException | ProfileException | UnavailableBorderPointException e) {
            LOGGER.log(Loggable.STACK, "Unable to get diameter profile", e);
            return this;
        }

    }

    /**
     * Draw the segments of a profilable component in the global colour swatch
     * colours.
     * 
     * @param n the component
     * @return the annotator
     */
    public ImageAnnotator annotateSegments(Profileable n) {

        try {
        	// only draw if there are segments
            if (n.getProfile(ProfileType.ANGLE).getSegments().size() > 0) { 
                for (int i = 0; i < n.getProfile(ProfileType.ANGLE).getSegments().size(); i++) {

                    IBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment("Seg_" + i);

                    float[] xpoints = new float[seg.length() + 1];
                    float[] ypoints = new float[seg.length() + 1];
                    for (int j = 0; j <= seg.length(); j++) {
                        int k = n.wrapIndex(seg.getStartIndex() + j);
                        IPoint p = n.getBorderPoint(k).plus(Imageable.COMPONENT_BUFFER);
                        xpoints[j] = (float) (p.getX()*scale);
                        ypoints[j] = (float) (p.getY()*scale);
                    }

                    PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);
                    Paint color = ColourSelecter.getColor(i);

                    annotatePolygon(segRoi, color);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error annotating segments", e);
        }
        return this;
    }

    /**
     * Draw the segments of a profilable component in the global colour swatch
     * colours. Draw the segments at their positions in the template component
     * image
     * 
     * @param n the component
     * @return the annotator
     */
    public ImageAnnotator annotateSegments(Profileable n, Imageable template) {

        try {

            if (n.getProfile(ProfileType.ANGLE).getSegments().size() > 0) { // only
                                                                            // draw
                                                                            // if
                                                                            // there
                                                                            // are
                                                                            // segments
                for (int i = 0; i < n.getProfile(ProfileType.ANGLE).getSegments().size(); i++) {

                    IBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment("Seg_" + i);

                    float[] xpoints = new float[seg.length() + 1];
                    float[] ypoints = new float[seg.length() + 1];
                    for (int j = 0; j <= seg.length(); j++) {
                        int k = n.wrapIndex(seg.getStartIndex() + j);
                        IBorderPoint p = n.getOriginalBorderPoint(k); // get the
                                                                      // border
                                                                      // points
                                                                      // in the
                                                                      // segment
                        xpoints[j] = (float) (p.getX()*scale);
                        ypoints[j] = (float) (p.getY()*scale);
                    }

                    PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);

                    // Offset the segment relative to the nucleus component
                    // image
                    IPoint base = IPoint.makeNew(segRoi.getXBase(), segRoi.getYBase());
                    IPoint offset = Imageable.translateCoordinateToComponentImage(base, template);

                    segRoi.setLocation(offset.getX(), offset.getY());

                    Paint color = ColourSelecter.getColor(i);
                    annotatePolygon(segRoi, color);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error annotating segments", e);
        }
        return this;
    }

    /**
     * Draw the signals within a nucleus on the current image. This assumes that the current image
     * is a nucleus component image.
     * 
     * @param n the nucleus
     * @return the annotator
     */
    public ImageAnnotator annotateSignals(@NonNull Nucleus n) {

        ISignalCollection signalCollection = n.getSignalCollection();
 
        for (UUID id : signalCollection.getSignalGroupIds()) {
        	        	            
            if(signalCollection.hasSignal(id)){

            	Color colour = ColourSelecter.getSignalColour(signalCollection.getSourceChannel(id));

            	List<INuclearSignal> signals = signalCollection.getSignals(id);


                for (INuclearSignal s : signals) {

                    annotatePoint(s.getCentreOfMass().plus(Imageable.COMPONENT_BUFFER), colour);
                	IPoint base = s.getBase().plus(Imageable.COMPONENT_BUFFER);

                    FloatPolygon p = s.toPolygon();
                    
                    float[] x = p.xpoints;
                    float[] y = p.ypoints;
                    
                    if(Math.abs(scale-1)>0.0000001){
                        for(int j=0; j<p.npoints; j++){
                            x[j]*=scale;
                            y[j]*=scale;
                        }
                    }
                    
                    PolygonRoi roi = new PolygonRoi(x, y, PolygonRoi.POLYGON);
                    roi.setLocation(base.getX()*scale, base.getY()*scale);
                    annotatePolygon(roi, colour);
                }
            }
        }
        return this;
    }
    
    /**
     * Annotate the given signal group on the nucleus
     * @param n
     * @param signalId
     * @param colour the signal colour
     * @return this annotator
     */
    public ImageAnnotator annotateSignal(@NonNull final Nucleus n, @NonNull UUID signalId, @NonNull Color colour) {

        ISignalCollection signalCollection = n.getSignalCollection();

        if(signalCollection.hasSignal(signalId)){

        	List<INuclearSignal> signals = signalCollection.getSignals(signalId);

        	for (INuclearSignal s : signals) {
        		IPoint base = s.getBase().plus(Imageable.COMPONENT_BUFFER);

        		FloatPolygon p = s.toPolygon();
        		PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);

        		roi.setLocation(base.getX(), base.getY());
        		annotatePolygon(roi, colour);
        	}
        }

        return this;
    }
}
