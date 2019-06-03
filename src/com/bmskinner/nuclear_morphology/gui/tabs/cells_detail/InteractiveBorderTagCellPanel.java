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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.InterfaceUpdater;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Show annotated cell images, and allow selection of tags
 * or other elements of the cell.
 * 
 * The 'bulging' code was adapted from https://stackoverflow.com/questions/22824041/explanation-for-the-bulge-effect-algorithm
 * @author ben
 * @since 1.14.0
 *
 */
public class InteractiveBorderTagCellPanel extends InteractiveCellPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public InteractiveBorderTagCellPanel(@NonNull CellUpdatedEventListener parent){
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
	 * Create the default annotated cell image, with border, segments and border tags highlighted
	 * @param dataset
	 * @param cell
	 * @param component
	 */
	private synchronized void createCellImage() {
		InterfaceUpdater u = () ->{
			output= null;
			ImageProcessor ip;
			try{
				ip = component.getImage();
				
			} catch(UnloadableImageException e){
				ip = AbstractImageFilterer.createWhiteColorProcessor( 1500, 1500); //TODO make based on cell location
			}

			ImageAnnotator an = new ImageAnnotator(ip);

			if(cell.hasCytoplasm()){
				an.crop(cell.getCytoplasm());
			} else{
				an.crop(cell.getNuclei().get(0));
			}
			ImageAnnotator an2 = new ImageAnnotator(an.toProcessor(), getWidth(), getHeight());

			for(Nucleus n : cell.getNuclei()){
				an2.annotateTagsOnCroppedNucleus(n);
			}    
			
			
			if(displayOptions.getBoolean(CellDisplayOptions.ROTATE_VERTICAL)) {
				try {
					ImageProcessor rot = rotateToVertical(cell, an2.toProcessor());
					rot.flipVertical(); // Y axis needs inverting since images have 0 at top
					if(cell.getNucleus().isClockwiseRP())
						rot.flipHorizontal();
					an2 = new ImageAnnotator(rot, getWidth(), getHeight());
					
				} catch (UnavailableBorderTagException e) {
					LOGGER.log(Loggable.STACK, e.getMessage(), e);
				}
			}
			
			imageLabel.setIcon(an2.toImageIcon());
			input = an2.toBufferedImage();
			sourceWidth = an.toProcessor().getWidth();
			sourceHeight = an.toProcessor().getHeight();
		};
		new Thread(u).start();
//		ThreadManager.getInstance().submit(u);
	}
	
	/**
	 * Listener for mouse interactions with the image on display
	 * @author bms41
	 * @since 1.15.0
	 *
	 */
	private class CellImageMouseListener extends MouseAdapter {
		public CellImageMouseListener() { super(); }
		private static final int MAX_BIG_RADIUS = 200;
		private static final int MIN_BIG_RADIUS = 10;
		private static final int MAX_SMALL_RADIUS = 100;
		private static final int MIN_SMALL_RADIUS = 5;
		
		
		@Override
        public synchronized void mouseWheelMoved(MouseWheelEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			// Modify the square size
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) ==
                InputEvent.CTRL_DOWN_MASK){
            	int temp = smallRadius +( 1*e.getWheelRotation());
            	temp = temp>MAX_SMALL_RADIUS?MAX_SMALL_RADIUS:temp;
            	temp = temp<MAX_SMALL_RADIUS?MAX_SMALL_RADIUS:temp;
                smallRadius = temp;
            } else {
            	// Modify the zoom
            	int temp = bigRadius +( 3 * e.getWheelRotation());
            	temp = temp>MAX_BIG_RADIUS?MAX_BIG_RADIUS:temp;
            	temp = temp<MIN_BIG_RADIUS?MIN_BIG_RADIUS:temp;
            	bigRadius = temp;
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
		
		private synchronized void updateTag(Tag tag, int newIndex) {

			ThreadManager.getInstance().execute(()->{
				boolean wasLocked = cell.getNucleus().isLocked();
				cell.getNucleus().setLocked(false);

				cell.getNucleus().setBorderTag(tag, newIndex);
				cell.getNucleus().updateVerticallyRotatedNucleus();

				if(tag.equals(Tag.ORIENTATION_POINT) || tag.equals(Tag.REFERENCE_POINT)) {
					cell.getNucleus().setStatistic(PlottableStatistic.OP_RP_ANGLE, Statistical.STAT_NOT_CALCULATED);
				}
				cell.getNucleus().updateDependentStats();
				cell.getNucleus().setLocked(wasLocked);
				dataset.getCollection().clear(PlottableStatistic.OP_RP_ANGLE, CellularComponent.NUCLEUS);
				cellUpdateHandler.fireCelllUpdateEvent(cell, dataset);
				createImage();
			});
		}

		private synchronized JPopupMenu createPopup(IBorderPoint point) {
			List<Tag> tags = dataset.getCollection().getProfileCollection().getBorderTags();
			JPopupMenu popupMenu = new JPopupMenu("Popup");
			Collections.sort(tags);

			for (Tag tag : tags) {

				if (tag.equals(Tag.INTERSECTION_POINT))
					continue; // The IP is determined solely by the OP

				JMenuItem item = new JMenuItem(tag.toString());

				item.addActionListener(a -> {
					int index = cell.getNucleus().getBorderIndex(point);
					updateTag(tag, index);
					repaint();
				});
				popupMenu.add(item);
			}

			// Find border tags with rulesets that have not been assigned in the median
			List<Tag> unassignedTags = new ArrayList<Tag>();
			for (Tag tag : BorderTagObject.values()) {
				if (tag.equals(Tag.INTERSECTION_POINT))
					continue;
				if (!tags.contains(tag)) {
					unassignedTags.add(tag);
				}
			}

			if (!unassignedTags.isEmpty()) {
				Collections.sort(unassignedTags);

				popupMenu.addSeparator();

				for (Tag tag : unassignedTags) {
					JMenuItem item = new JMenuItem(tag.toString());
					item.setForeground(Color.DARK_GRAY);

					item.addActionListener(a -> {
						int index = cell.getNucleus().getBorderIndex(point);
						updateTag(tag, index);
					});
					popupMenu.add(item);
				}
			}
			return popupMenu;
		}

		@Override
		public synchronized void mouseClicked(MouseEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			IPoint clickedPoint = translatePanelLocationToSourceImage(e.getX(), e.getY());

			Optional<IBorderPoint> point = cell.getNucleus().getBorderList()
					.stream().filter(p->{
						return clickedPoint.getX()>=p.getX()-0.4 && 
								clickedPoint.getX()<=p.getX()+0.4 &&
								clickedPoint.getY()>=p.getY()-0.4 && 
								clickedPoint.getY()<=p.getY()+0.4;
						
					})
					.findFirst();

			if(point.isPresent()) {
				JPopupMenu popup = createPopup(point.get());
				popup.show(imageLabel, e.getX(), e.getY());
			}

		}
		
	}
	
	private void createMeshImage() {
		InterfaceUpdater u = () ->{
			try {
				output= null;
				ImageProcessor ip;
				try{
					ip = component.getImage();
				} catch(UnloadableImageException e){
					ip = AbstractImageFilterer.createWhiteColorProcessor( 1500, 1500); //TODO make based on cell location
				}
				ImageAnnotator an = new ImageAnnotator(ip);

				if(cell.hasCytoplasm()){
					an.crop(cell.getCytoplasm());
				} else{
					an.crop(cell.getNuclei().get(0));
				}
				
				Mesh<Nucleus> consensusMesh = new DefaultMesh(dataset.getCollection().getConsensus());
				for(Nucleus n : cell.getNuclei()) {
					Mesh<Nucleus> m = new DefaultMesh(n, consensusMesh);
					Mesh<Nucleus> compMesh = m.comparison(consensusMesh);
					MeshAnnotator an3 = new MeshAnnotator( an.toProcessor(), getWidth(), getHeight(), compMesh);
					an3.annotateNucleusMeshEdges();
					imageLabel.setIcon(an3.toImageIcon());
					input = an3.toProcessor().getBufferedImage();
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
				ImageProcessor ip;
				try{
					ip = component.getImage();
				} catch(UnloadableImageException e){
					ip = AbstractImageFilterer.createWhiteColorProcessor( 1500, 1500); //TODO make based on cell location
				}
				ImageAnnotator an = new ImageAnnotator(ip);

				if(cell.hasCytoplasm()){
					an.crop(cell.getCytoplasm());
				} else{
					an.crop(cell.getNuclei().get(0));
				}
				
				Mesh<Nucleus> consensusMesh = new DefaultMesh(dataset.getCollection().getConsensus());
        		for(Nucleus n : cell.getNuclei()) {
        			Mesh<Nucleus> m = new DefaultMesh(n, consensusMesh);
        			MeshImage im = new DefaultMeshImage(m, ip.duplicate());
        			ImageProcessor drawn = im.drawImage(consensusMesh);
        			drawn.flipVertical();
        			an = new ImageAnnotator(drawn, getWidth(), getHeight());
        		}
        		input = an.toProcessor().getBufferedImage();
        		imageLabel.setIcon(an.toImageIcon());
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
				.stream().filter(p->{
					return clickedPoint.getX()>=p.getX()-0.4 && 
							clickedPoint.getX()<=p.getX()+0.4 &&
							clickedPoint.getY()>=p.getY()-0.4 && 
							clickedPoint.getY()<=p.getY()+0.4;
					
				})
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
					g2.setColor(Color.GREEN);
				}
				if(cell.getNucleus().hasBorderTag(Tag.BOTTOM_VERTICAL) && 
						cell.getNucleus().getBorderPoint(Tag.BOTTOM_VERTICAL).overlapsPerfectly(point.get())) {
					g2.setColor(Color.GREEN);
				}
				if(cell.getNucleus().hasBorderTag(Tag.REFERENCE_POINT) && 
						cell.getNucleus().getBorderPoint(Tag.REFERENCE_POINT).overlapsPerfectly(point.get())) {
					g2.setColor(Color.ORANGE);
				}
				if(cell.getNucleus().hasBorderTag(Tag.ORIENTATION_POINT) && 
						cell.getNucleus().getBorderPoint(Tag.ORIENTATION_POINT).overlapsPerfectly(point.get())) {
					g2.setColor(Color.BLUE);
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
