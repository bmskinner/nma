package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.InteractiveCellOutlinePanel;

/**
 * Paints cell outlines on JPanels,
 * correcting for scales
 * @author ben
 * @since 2.0.0
 *
 */
public class CellImagePainter {
	
	private static final Logger LOGGER = Logger.getLogger(CellImagePainter.class.getName());
	
	/**
	 * Paint the given cell over an input image
	 * @param input the image to paint on
	 * @param cell the cell to be painted
	 * @param ratio the size ratio new image / the original cell image
	 * @return a new image with the cell painted over the input image
	 */
	public static BufferedImage paintCell(BufferedImage input, ICell cell, double ratio) {
		
		BufferedImage output = new BufferedImage( input.getWidth(), input.getHeight(),  BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = output.createGraphics();
		g2.drawImage(input, 0, 0, null);
		g2.setStroke(new BasicStroke(3));
		
		Nucleus n = cell.getPrimaryNucleus();
		try {
			ISegmentedProfile sp = cell.getPrimaryNucleus().getProfile(ProfileType.ANGLE);
			List<IProfileSegment> segs = sp.getSegments();
			for (int i = 0; i < segs.size(); i++) {
				g2.setColor(ColourSelecter.getColor(i));
				
                IProfileSegment seg = segs.get(i);

                for (int j = 0; j <= seg.length(); j++) {
                    int k = n.wrapIndex(seg.getStartIndex() + j);
                    IPoint p = n.getBorderPoint(k)
                    		.minus(n.getBase())
                    		.plus(CellularComponent.COMPONENT_BUFFER);
                                                            
                    double x = p.getX()*ratio;
                    double y = p.getY()*ratio;

                    g2.drawLine((int)x, (int)y, (int)x, (int)y);
                }

            }
			
		} catch (MissingProfileException | ProfileException | UnavailableBorderPointException e) {
			LOGGER.log(Level.FINE, "Unable to paint cell", e);
		}
		return output;
	}
		
//	/**
//	 * Translate the given point from a location within this JPanel
//	 * to a location within the original cell source image.
//	 * @param x
//	 * @param y
//	 * @return
//	 */
//	protected synchronized IPoint translatePanelLocationToSourceImage(JPanel panel, int x, int y) {
//		// The original image dimensions
//		int w = sourceWidth;
//		int h = sourceHeight;
//		
//		// The rescaled dimensions
//		int iconWidth = imageLabel.getIcon().getIconWidth();
//		int iconHeight = imageLabel.getIcon().getIconHeight();
//		
//		// The image panel dimensions
//		int panelWidth = getWidth();
//		int panelHeight = getHeight();
//		
//		// The position of the click relative to the icon
//		int iconX = x-((panelWidth-iconWidth)/2);
//		int iconY = y-((panelHeight-iconHeight)/2);
//		
//		// The position  of the click within the original image
//		double xPositionInImage = (((double)iconX/(double) iconWidth)*w)-Imageable.COMPONENT_BUFFER;
//		double yPositionInImage = (((double)iconY/(double) iconHeight)*h)-Imageable.COMPONENT_BUFFER;
//		return IPoint.makeNew(xPositionInImage, yPositionInImage);
//	}
			
//	/**
//	 * Translate the given point from a location within the rendered image
//	 * to a location within the original cell source image.
//	 * @param x
//	 * @param y
//	 * @return
//	 */
//	protected synchronized IPoint translateRenderedLocationToSourceImage(double x, double y) {		
//		// The rescaled dimensions
//		int iconWidth = imageLabel.getIcon().getIconWidth();
//		int iconHeight = imageLabel.getIcon().getIconHeight();
//						
//		// The position  of the click within the original image
//		double xPositionInImage = ((x/iconWidth)*sourceWidth)-Imageable.COMPONENT_BUFFER;
//		double yPositionInImage = ((y/iconHeight)*sourceHeight)-Imageable.COMPONENT_BUFFER;
//		return IPoint.makeNew(xPositionInImage, yPositionInImage);
//	}

}
