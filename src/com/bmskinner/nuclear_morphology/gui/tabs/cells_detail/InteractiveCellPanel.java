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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.CelllUpdateEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Base class for annotated cell images, allowing selection of tags
 * or other elements of the cell.
 * @author ben
 * @since 1.14.0
 *
 */
public abstract class InteractiveCellPanel extends JPanel implements Loggable {
	
	protected JLabel imageLabel;
	
	protected DatasetEventHandler dh = new DatasetEventHandler(this);
	protected CelllUpdateEventHandler cellUpdateHandler = new CelllUpdateEventHandler(this);
	
	protected IAnalysisDataset dataset = null;
	protected ICell cell = null;
	protected CellularComponent component = null;
	protected HashOptions displayOptions;

	// the undistorted image
	protected BufferedImage input;
	protected BufferedImage output;
	protected int smallRadius = 25;
	protected int bigRadius   = 50;
	protected int sourceWidth;
	protected int sourceHeight;
	
	/**
	 * Keys for display options. These are used in a HashOptions
	 * @author bms41
	 * @since 1.15.4
	 *
	 */
	public class CellDisplayOptions {
		public static final String SHOW_MESH       = "Show mesh";
		public static final String WARP_IMAGE      = "Warp image";
		public static final String ROTATE_VERTICAL = "Rotate vertical";		
		
		private CellDisplayOptions() { 
			// private constructor. Access to static fields only
		}
	}
	
	/**
	 * Create with a parent panel to listen for cell updates
	 * @param parent
	 */
	public InteractiveCellPanel(CellUpdatedEventListener parent){
		
		cellUpdateHandler.addCellUpdatedEventListener(parent);
		setLayout(new BorderLayout());
		imageLabel = new JLabel();
		imageLabel.setHorizontalAlignment(JLabel.CENTER);
		imageLabel.setVerticalAlignment(JLabel.CENTER);
		imageLabel.setHorizontalTextPosition(JLabel.CENTER);
		imageLabel.setVerticalTextPosition(JLabel.CENTER);
		add(imageLabel, BorderLayout.CENTER);
	}
	
	/**
	 * Set the panel to a null state with no cell showing
	 */
	public void setNull() {
		setCell(null, null, null, new DefaultOptions());
	}

	/**
	 * Set the cell to display, and basic display options.
	 * @param dataset the dataset the cell is present in
	 * @param cell the cell to draw
	 * @param component the cellular component within the cell to draw
	 * @param cellDisplayOptions how the cell should be displayed; uses the keys in {@link CellDisplayOptions}
	 */
	public void setCell(@Nullable IAnalysisDataset dataset, @Nullable ICell cell, @Nullable CellularComponent component, HashOptions cellDisplayOptions) {
		if(dataset==null || cell==null || component==null) {
			imageLabel.setIcon(null);
			return;
		}
		this.dataset     = dataset;
		this.cell        = cell;
		this.component   = component;
		displayOptions = cellDisplayOptions;
		createImage();
	}
		
	/**
	 * Add an event listener for dataset events
	 * @param l
	 */
	public synchronized void addDatasetEventListener(EventListener l) {
        dh.addListener(l);
    }

	/**
	 * Remove an event listener if present
	 * @param l
	 */
	public synchronized void removeDatasetEventListener(EventListener l) {
        dh.removeListener(l);
    }
	
	
	/**
	 * Update the rendered image with the mouse at the given location
	 * @param x
	 * @param y
	 */
	protected synchronized void updateImage(int x, int y) {
		if (output == null) 
			output = new BufferedImage( input.getWidth(), input.getHeight(),  BufferedImage.TYPE_INT_ARGB);
		computeBulgeImage(input, x, y, smallRadius, bigRadius, output);
		if(imageLabel!=null)
			imageLabel.setIcon(new ImageIcon(output));
		repaint();
	}
	
	/**
	 * Create the base image for the panel.
	 */
	protected abstract void createImage();
	
	/**
	 * Create the image with square overlay for rendering
	 */
	protected abstract void computeBulgeImage(BufferedImage input, int cx, int cy, 
	        int small, int big, BufferedImage output);
	
	
	/**
	 * Translate the given point from a location within this JPanel
	 * to a location within the original cell source image.
	 * @param x
	 * @param y
	 * @return
	 */
	protected synchronized IPoint translatePanelLocationToSourceImage(int x, int y) {
		// The original image dimensions
		int w = sourceWidth;
		int h = sourceHeight;
		
		// The rescaled dimensions
		int iconWidth = imageLabel.getIcon().getIconWidth();
		int iconHeight = imageLabel.getIcon().getIconHeight();
		
		// The image panel dimensions
		int panelWidth = getWidth();
		int panelHeight = getHeight();
		
		// The position of the click relative to the icon
		int iconX = x-((panelWidth-iconWidth)/2);
		int iconY = y-((panelHeight-iconHeight)/2);
		
		// The position  of the click within the original image
		double xPositionInImage = (((double)iconX/(double) iconWidth)*w)-Imageable.COMPONENT_BUFFER;
		double yPositionInImage = (((double)iconY/(double) iconHeight)*h)-Imageable.COMPONENT_BUFFER;
		return IPoint.makeNew(xPositionInImage, yPositionInImage);
	}
		
	/**
	 * Translate the given point from a location within this JPanel
	 * to a location within the rendered image.
	 * @param e the event with coordinates
	 * @return
	 */
	protected synchronized IPoint translatePanelLocationToRenderedImage(MouseEvent e) {
		// The rescaled dimensions
		if(imageLabel==null)
			return null;
		int iconWidth = imageLabel.getIcon().getIconWidth();
		int iconHeight = imageLabel.getIcon().getIconHeight();
		
		// The image panel dimensions
		int panelWidth = getWidth();
		int panelHeight = getHeight();
		
		// The position of the click relative to the icon
		int iconX = e.getX()-((panelWidth-iconWidth)/2);
		int iconY = e.getY()-((panelHeight-iconHeight)/2);
		return IPoint.makeNew(iconX, iconY);
	}
	
	/**
	 * Translate the given point from a location within the rendered image
	 * to a location within the original cell source image.
	 * @param x
	 * @param y
	 * @return
	 */
	protected synchronized IPoint translateRenderedLocationToSourceImage(double x, double y) {		
		// The rescaled dimensions
		int iconWidth = imageLabel.getIcon().getIconWidth();
		int iconHeight = imageLabel.getIcon().getIconHeight();
						
		// The position  of the click within the original image
		double xPositionInImage = ((x/iconWidth)*sourceWidth)-Imageable.COMPONENT_BUFFER;
		double yPositionInImage = ((y/iconHeight)*sourceHeight)-Imageable.COMPONENT_BUFFER;
		return IPoint.makeNew(xPositionInImage, yPositionInImage);
	}

	
	protected ImageProcessor rotateToVertical(ICell c, ImageProcessor ip) throws UnavailableBorderTagException {
        // Calculate angle for vertical rotation
        Nucleus n = c.getNucleus();

        IPoint topPoint;
        IPoint btmPoint;

        if (!n.hasBorderTag(Tag.TOP_VERTICAL) || !n.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
            topPoint = n.getCentreOfMass();
            btmPoint = n.getBorderPoint(Tag.ORIENTATION_POINT);

        } else {

            topPoint = n.getBorderPoint(Tag.TOP_VERTICAL);
            btmPoint = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

            // Sometimes the points have been set to overlap in older datasets
            if (topPoint.overlapsPerfectly(btmPoint)) {
                topPoint = n.getCentreOfMass();
                btmPoint = n.getBorderPoint(Tag.ORIENTATION_POINT);
            }
        }

        // Find which point is higher in the image
        IPoint upperPoint = topPoint.getY() > btmPoint.getY() ? topPoint : btmPoint;
        IPoint lowerPoint = upperPoint == topPoint ? btmPoint : topPoint;

        IPoint comp = IPoint.makeNew(lowerPoint.getX(), upperPoint.getY());

        /*
         * LA RA RB LB
         * 
         * T C C T B C C B \ | | / \ | | / B B T T
         * 
         * When Ux<Lx, angle describes the clockwise rotation around L needed to
         * move U above it. When Ux>Lx, angle describes the anticlockwise
         * rotation needed to move U above it.
         * 
         * If L is supposed to be on top, the clockwise rotation must be 180+a
         * 
         * However, the image coordinates have a reversed Y axis
         */

        double angleFromVertical = lowerPoint.findSmallestAngle(upperPoint, comp);

        double angle = 0;
        if (topPoint.isLeftOf(btmPoint) && topPoint.isAbove(btmPoint)) {
            angle = 360 - angleFromVertical;
            // log("LA: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (topPoint.isRightOf(btmPoint) && topPoint.isAbove(btmPoint)) {
            angle = angleFromVertical;
            // log("RA: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (topPoint.isLeftOf(btmPoint) && topPoint.isBelow(btmPoint)) {
            angle = angleFromVertical + 180;
            // angle = 180-angleFromVertical;
            // log("LB: "+angleFromVertical+" to "+angle); // Tested working
        }

        if (topPoint.isRightOf(btmPoint) && topPoint.isBelow(btmPoint)) {
            // angle = angleFromVertical+180;
            angle = 180 - angleFromVertical;
            // log("RB: "+angleFromVertical+" to "+angle); // Tested working
        }

        // Increase the canvas size so rotation does not crop the nucleus
        finer("Input: " + n.getNameAndNumber() + " - " + ip.getWidth() + " x " + ip.getHeight());
        ImageProcessor newIp = createEnlargedProcessor(ip, angle);

        newIp.rotate(angle);
        return newIp;
    }
	
	protected ImageProcessor createEnlargedProcessor(ImageProcessor ip, double degrees) {

        double rad = Math.toRadians(degrees);

        // Calculate the new width and height of the canvas
        // new width is h sin(a) + w cos(a) and vice versa for height
        double newWidth = Math.abs(Math.sin(rad) * ip.getHeight()) + Math.abs(Math.cos(rad) * ip.getWidth());
        double newHeight = Math.abs(Math.sin(rad) * ip.getWidth()) + Math.abs(Math.cos(rad) * ip.getHeight());

        int w = (int) Math.ceil(newWidth);
        int h = (int) Math.ceil(newHeight);

        // The new image may be narrower or shorter following rotation.
        // To avoid clipping, ensure the image never gets smaller in either
        // dimension.
        w = w < ip.getWidth() ? ip.getWidth() : w;
        h = h < ip.getHeight() ? ip.getHeight() : h;

        // paste old image to centre of enlarged canvas
        int xBase = (w - ip.getWidth()) >> 1;
        int yBase = (h - ip.getHeight()) >> 1;

        finer(String.format("New image %sx%s from %sx%s : Rot: %s", w, h, ip.getWidth(), ip.getHeight(), degrees));

        finest("Copy starting at " + xBase + ", " + yBase);

        ImageProcessor newIp = new ColorProcessor(w, h);

        newIp.setColor(Color.WHITE); // fill current space with white
        newIp.fill();

        newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white
        newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
        return newIp;
    }
}
