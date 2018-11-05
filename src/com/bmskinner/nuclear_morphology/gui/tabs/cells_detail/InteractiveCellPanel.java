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
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.CelllUpdateEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
	protected boolean isShowMesh;
	protected boolean isWarpImage;
	
	// the undistorted image
	protected BufferedImage input;
	protected BufferedImage output;
	protected int smallRadius = 25;
	protected int bigRadius   = 50;
	protected int sourceWidth;
	protected int sourceHeight;
	
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
		setCell(null, null, null, false, false);
	}

	/**
	 * Set the cell to display, and basic display options.
	 * @param dataset the dataset the cell is present in
	 * @param cell the cell to draw
	 * @param component the cellular component within the cell to draw
	 * @param isShowMesh should the comparison mesh with the consensus nucleus be drawn?
	 * @param isWarpImage should the image be warped to fit the consensus nucleus? (cannot be true at the same time as isShowMesh)
	 */
	public void setCell(@Nullable IAnalysisDataset dataset, @Nullable ICell cell, @Nullable CellularComponent component, boolean isShowMesh, boolean isWarpImage) {
		if(dataset==null || cell==null || component==null) {
			imageLabel.setIcon(null);
			return;
		}
		this.dataset     = dataset;
		this.cell        = cell;
		this.component   = component;
		this.isShowMesh  = isShowMesh;
		this.isWarpImage = isWarpImage;
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

}
