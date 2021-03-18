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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.image.AbstractImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.charting.image.MeshAnnotator;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.InterfaceUpdater;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.NumberTools;

import ij.process.ImageProcessor;

/**
 * Show annotated cell images with signals or other elements of the cell.
 * 
 * The 'bulging' code was adapted from https://stackoverflow.com/questions/22824041/explanation-for-the-bulge-effect-algorithm
 * @author ben
 * @since 1.14.0
 *
 */
public class InteractiveCellOutlinePanel extends InteractiveCellPanel {
	
	private static final Logger LOGGER = Logger.getLogger(InteractiveCellOutlinePanel.class.getName());

	public InteractiveCellOutlinePanel(@NonNull CellUpdatedEventListener parent){
		super(parent);
		CellImageMouseListener mouseListener = new CellImageMouseListener();
		imageLabel.addMouseWheelListener(mouseListener);
		imageLabel.addMouseMotionListener(mouseListener);
		imageLabel.addMouseListener(mouseListener);
	}

	@Override
	protected void createImage() {
		if(displayOptions.getBoolean(CellDisplayOptions.SHOW_MESH)) {
			createMeshImage();
			return;
		}
		if(displayOptions.getBoolean(CellDisplayOptions.WARP_IMAGE)) {
			createWarpImage();
			return;
		}
		createCellImage();
	}
	
	/**
	 * Create the default annotated cell outline image
	 */
	private synchronized void createCellImage() {
		InterfaceUpdater u = () ->{
			output = null;
			
			ImageProcessor ip = loadCellImage();
			ImageAnnotator an = new ImageAnnotator(ip);
			cropImageToCell(an);
			updateSourceImageDimensions(an);
			
			ImageAnnotator an2 = scaleSmallImageToPanel(an);

			for(Nucleus n : cell.getNuclei()){
				an2.annotateSignalsOnCroppedNucleus(n);
			}    
			
			if(displayOptions.getBoolean(CellDisplayOptions.ROTATE_VERTICAL)) {
				an2 = rotateVertical(an2);
			}
			
			// Whatever the canvas size, rescale the final image to the panel
			ImageAnnotator an3 = scaleImageToPanel(an2);
			displayAnnotatorContents(an3);
		};
		new Thread(u).start();
	}
	
	/**
	 * Load the image for the selected component, or
	 * create a blank canvas on error
	 * @return
	 */
	private ImageProcessor loadCellImage() {
		ImageProcessor ip;
		try{
			ip = component.getImage();
			
		} catch(UnloadableImageException e){
			LOGGER.finer("Unable to load image: "+component.getSourceFile());
			ip = AbstractImageFilterer.createWhiteColorProcessor( 1500, 1500); //TODO make based on cell location
		}
		return ip;
	}
	
	/**
	 * Crop the annotator to the region of the image 
	 * containing the object of interest
	 * @param an
	 */
	private void cropImageToCell(ImageAnnotator an) {
		if(cell.hasCytoplasm()){
			an.crop(cell.getCytoplasm());
		} else{
			an.crop(cell.getNuclei().get(0));
		}
	}
	
	/**
	 * If the image is smaller than the available space,
	 * create a new annotator that fills this space. 
	 * 
	 * If the image is larger than the available space,
	 * shrink it later, otherwise there will not be room 
	 * to draw the features
	 * @param an
	 * @return
	 */
	private ImageAnnotator scaleSmallImageToPanel(ImageAnnotator an) {
		ImageAnnotator an2 = new ImageAnnotator(an.toProcessor());
		boolean isSmaller = an.toProcessor().getWidth()<getWidth() && an.toProcessor().getHeight()<getHeight();
		if(isSmaller) {
			an2 = scaleImageToPanel(an);
		}
		return an2;
	}
	
	/**
	 * Create a new annotator that fills the panel space.
	 * Images will be up or down-scaled appropriately 
	 * @param an
	 * @return
	 */
	private ImageAnnotator scaleImageToPanel(ImageAnnotator an) {
		return new ImageAnnotator(an.toProcessor(), getWidth(), getHeight());
	}
	
	/**
	 * Set the annotator contents to the currently active image
	 * @param an
	 */
	private void displayAnnotatorContents(ImageAnnotator an) {
		imageLabel.setIcon(an.toImageIcon());
		input = an.toBufferedImage();
	}
	
	/**
	 * Set the source image width and height to allow coordinate
	 * conversion
	 * @param an
	 */
	private void updateSourceImageDimensions(ImageAnnotator an) {
		sourceWidth = an.toProcessor().getWidth();
		sourceHeight = an.toProcessor().getHeight();
	}
	
	/**
	 * Rotate the given annotator to vertical and scale to
	 * fit the panel size
	 * @param an
	 * @return
	 */
	private ImageAnnotator rotateVertical(ImageAnnotator an) {
		try {
			ImageProcessor rot = rotateToVertical(cell, an.toProcessor());
			rot.flipVertical(); // Y axis needs inverting since images have 0 at top
			if(cell.getNucleus().isClockwiseRP())
				rot.flipHorizontal();
			return new ImageAnnotator(rot, getWidth(), getHeight());
			
		} catch (UnavailableBorderTagException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			return an;
		}
	}
	
	/**
	 * Listener for mouse interactions with the image on display
	 * @author bms41
	 * @since 1.15.0
	 *
	 */
	private class CellImageMouseListener extends MouseAdapter {
		
		private static final int SMALL_MULTIPLIER = 1;
		private static final int LARGE_MULTIPLIER = 3;

		/** Minimum radius of the zoomed image */
		private static final int SMALL_MIN_RADIUS = 5;
		private static final int SMALL_MAX_RADIUS = 100;
		
		private static final int LARGE_MIN_RADIUS = 10;
		private static final int LARGE_MAX_RADIUS = 200;
		
		public CellImageMouseListener() { super(); }
		
		
		@Override
        public synchronized void mouseWheelMoved(MouseWheelEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			// Modify the square size
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) ==
                InputEvent.CTRL_DOWN_MASK){
            	int temp = smallRadius +( SMALL_MULTIPLIER * e.getWheelRotation());
            	smallRadius = NumberTools.constrain(temp, SMALL_MIN_RADIUS, SMALL_MAX_RADIUS);
            } else {
            	// Modify the zoom
            	int temp = bigRadius +( LARGE_MULTIPLIER * e.getWheelRotation());
            	bigRadius = NumberTools.constrain(temp, LARGE_MIN_RADIUS, LARGE_MAX_RADIUS);
            }
            IPoint p = translatePanelLocationToRenderedImage(e); 
			updateImage(p.getXAsInt(), p.getYAsInt());
        }
		
		@Override
		public synchronized void mouseMoved(MouseEvent e){
			if(imageLabel.getIcon()==null)
				return;
			IPoint p = translatePanelLocationToRenderedImage(e); 
			if(p==null)
				return;
			updateImage(p.getXAsInt(), p.getYAsInt());
		}		
	}
	
	private void createMeshImage() {
		InterfaceUpdater u = () ->{
			try {
				output= null;
				ImageProcessor ip = loadCellImage();
				ImageAnnotator an = new ImageAnnotator(ip);
				cropImageToCell(an);
				
				Mesh<Nucleus> consensusMesh = new DefaultMesh<>(dataset.getCollection().getConsensus());
				for(Nucleus n : cell.getNuclei()) {
					
					Mesh<Nucleus> m = new DefaultMesh<>(n, consensusMesh);
					Mesh<Nucleus> compMesh = m.comparison(consensusMesh);
					MeshAnnotator an3 = new MeshAnnotator( an.toProcessor(), getWidth(), getHeight(), compMesh);
					an3.annotateNucleusMeshEdges();
										
					ImageAnnotator an4 = an3.toAnnotator();
					if(displayOptions.getBoolean(CellDisplayOptions.ROTATE_VERTICAL)) {
						an4 = rotateVertical(an4);
					}
					
					// Whatever the canvas size, rescale the final image to the panel
					an4 = scaleImageToPanel(an4);
					displayAnnotatorContents(an4);
				}

			} catch (MeshCreationException | IllegalArgumentException e) {
				LOGGER.log(Loggable.STACK, "Error making mesh or loading image", e);
				setNull();
			}
		};
		ThreadManager.getInstance().submit(u);
	}
	
	private void createWarpImage() {
		InterfaceUpdater u = () ->{
			try {
				output= null;
				ImageProcessor ip = loadCellImage();
				ImageAnnotator an = new ImageAnnotator(ip);
				cropImageToCell(an);
				updateSourceImageDimensions(an);
				
				Mesh<Nucleus> consensusMesh = new DefaultMesh<>(dataset.getCollection().getConsensus());
        		for(Nucleus n : cell.getNuclei()) {
        			Mesh<Nucleus> m = new DefaultMesh<>(n, consensusMesh);
        			MeshImage<Nucleus> im = new DefaultMeshImage<>(m, ip.duplicate());
        			ImageProcessor drawn = im.drawImage(consensusMesh);
        			drawn.flipVertical();
        			an = new ImageAnnotator(drawn, getWidth(), getHeight());
        		}
        		displayAnnotatorContents(an);
			} catch (MeshCreationException | IllegalArgumentException | MeshImageCreationException | UncomparableMeshImageException e) {
				LOGGER.log(Loggable.STACK, "Error making mesh or loading image", e);
				setNull();
			}
		};
		ThreadManager.getInstance().submit(u);
	}
	
	@Override
	protected synchronized void computeBulgeImage(BufferedImage input, int cx, int cy, 
	        int small, int big, BufferedImage output){
		
		int dx1 = cx-big; // the big rectangle
		int dy1 = cy-big;
		int dx2 = cx+big;
		int dy2 = cy+big;
		
		int sx1 = cx-small; // the small source rectangle
		int sy1 = cy-small;
		int sx2 = cx+small;
		int sy2 = cy+small;
		
		IPoint clickedPoint = translateRenderedLocationToSourceImage(cx, cy);

		Optional<IBorderPoint> point = cell.getNucleus().getBorderList()
				.stream().filter(
					p->clickedPoint.getX()>=p.getX()-0.4 && 
							clickedPoint.getX()<=p.getX()+0.4 &&
							clickedPoint.getY()>=p.getY()-0.4 && 
							clickedPoint.getY()<=p.getY()+0.4)
				.findFirst();

		Graphics2D g2 = output.createGraphics();
		
		g2.drawImage(input, 0, 0, null);
		g2.drawImage(input, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
		Color c = g2.getColor();
		Stroke s = g2.getStroke();
		
		if(point.isPresent()) {
			g2.setColor(Color.CYAN);
			try {
				
				if(cell.getNucleus().hasBorderTag(Tag.TOP_VERTICAL) && 
						cell.getNucleus().getBorderPoint(Tag.TOP_VERTICAL).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Tag.TOP_VERTICAL));
				}
				if(cell.getNucleus().hasBorderTag(Tag.BOTTOM_VERTICAL) && 
						cell.getNucleus().getBorderPoint(Tag.BOTTOM_VERTICAL).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Tag.BOTTOM_VERTICAL));
				}
				if(cell.getNucleus().hasBorderTag(Tag.REFERENCE_POINT) && 
						cell.getNucleus().getBorderPoint(Tag.REFERENCE_POINT).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Tag.REFERENCE_POINT));
				}
				if(cell.getNucleus().hasBorderTag(Tag.ORIENTATION_POINT) && 
						cell.getNucleus().getBorderPoint(Tag.ORIENTATION_POINT).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Tag.ORIENTATION_POINT));
				}

			} catch (UnavailableBorderTagException e) {
				// no action needed, colour remains cyan
			}

			g2.setStroke(new BasicStroke(3));
		} else {
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(2));
		}

		g2.drawRect(dx1, dy1, big*2, big*2);
		
		g2.setColor(c);
		g2.setStroke(s);
	}
}
