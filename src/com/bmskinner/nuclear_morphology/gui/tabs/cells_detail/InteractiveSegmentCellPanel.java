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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.analysis.image.AbstractImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.InterfaceUpdater;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent.SegmentUpdateType;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.NumberTools;

import ij.process.ImageProcessor;

public class InteractiveSegmentCellPanel extends InteractiveCellPanel {

	private static final Logger LOGGER = Logger.getLogger(InteractiveSegmentCellPanel.class.getName());

	/** When a clicking a feature in an image, allow the clicked point to be 
	 * this many pixels away from the true point */
	private static final double POINT_CLICK_RADIUS_PIXELS = 0.4;

	protected List<SegmentEventListener> listeners = new ArrayList<>();

	public InteractiveSegmentCellPanel(CellUpdatedEventListener parent){
		super(parent);
		MouseAdapter mouseListener = new ImageMouseAdapter();
		imageLabel.addMouseWheelListener(mouseListener);
		imageLabel.addMouseMotionListener(mouseListener);
		imageLabel.addMouseListener(mouseListener);
	}

	@Override
	protected synchronized void createImage() {
		LOGGER.finer( "Redrawing cell image");
		InterfaceUpdater u = () ->{
			output = null;
			ImageProcessor ip;
			try{
				ip = component.getImage();
			} catch(UnloadableImageException e){
				ip = AbstractImageFilterer.createWhiteColorProcessor( 1500, 1500); //TODO make based on cell location
			}
			
			// Crop to the relevant part of the image
			LOGGER.finer("Cell raw image: "+ip.getWidth()+" x "+ip.getHeight());
			ImageAnnotator an = new ImageAnnotator(ip);

			if(cell.hasCytoplasm()){
				an.crop(cell.getCytoplasm());
			} else{
				an.crop(cell.getNuclei().get(0));
			}
			LOGGER.finer("Cell cropped image: "+an.toProcessor().getWidth()+" x "+an.toProcessor().getHeight());

			// If the image is smaller than the available space, create a new annotator
			// that fills this space. If the image is larger than the available space,
			// create at full size and shrink it later; there will not be room
			// to draw the features on a smaller canvas
			ImageAnnotator an2 = new ImageAnnotator(an.toProcessor());
			boolean isSmaller = an.toProcessor().getWidth()<getWidth() && an.toProcessor().getHeight()<getHeight();
			if(isSmaller) {
				an2 = new ImageAnnotator(an.toProcessor(), getWidth(), getHeight());
			}
			
			for(Nucleus n : cell.getNuclei()){
				an2.annotateSegmentsOnCroppedNucleus(n);
			}    
			
			// Expand or shrink the canvas to fit the panel
			ImageAnnotator an3 = new ImageAnnotator(an2.toProcessor(), getWidth(), getHeight());

			imageLabel.setIcon(an3.toImageIcon());
			input = an3.toBufferedImage();
			sourceWidth = an.toProcessor().getWidth();
			sourceHeight = an.toProcessor().getHeight();
		};
		new Thread(u).start(); // avoid thread manager so updates are immediate
	}

	public synchronized void addSegmentEventListener(SegmentEventListener l) {
		listeners.add(l);
	}

	public synchronized void removeSegmentEventListener(SegmentEventListener l) {
		listeners.remove(l);
	}

	/**
	 * Fire a segmentation event
	 * @param id the segment to be altered
	 * @param index the index to be altered
	 * @param type the update type. Types are specified as static ints in SegmentEvent
	 */
	protected synchronized void fireSegmentEvent(UUID id, int index, SegmentUpdateType type) {
		SegmentEvent e = new SegmentEvent(this, id, index, type);

		for (SegmentEventListener l : listeners) {
			l.segmentEventReceived(e);
		}
	}

	/**
	 * Respond to mouse inputs
	 * @author bms41
	 * @since 1.5.4
	 *
	 */
	private class ImageMouseAdapter extends MouseAdapter {
		
		private static final int SMALL_MULTIPLIER = 1;
		private static final int LARGE_MULTIPLIER = 3;

		/** Minimum radius of the zoomed image */
		private static final int SMALL_MIN_RADIUS = 5;
		private static final int SMALL_MAX_RADIUS = 100;
		
		private static final int LARGE_MIN_RADIUS = 10;
		private static final int LARGE_MAX_RADIUS = 200;
		
		
		@Override
		public synchronized void mouseWheelMoved(MouseWheelEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) ==
					InputEvent.CTRL_DOWN_MASK){
				int temp = smallRadius +( SMALL_MULTIPLIER*e.getWheelRotation());
				smallRadius = NumberTools.constrain(temp, SMALL_MIN_RADIUS, SMALL_MAX_RADIUS);
			} else {
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
			updateImage(p.getXAsInt(), p.getYAsInt());
		}

		@Override
		public synchronized void mouseClicked(MouseEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			IPoint clickedPoint = translatePanelLocationToSourceImage(e.getX(), e.getY());

			// Not a circle around the valid point to click, but close enough
			Optional<IPoint> point = cell.getPrimaryNucleus().getBorderList()
					.stream().filter(p->{
						return clickedPoint.getX()>=p.getX()-POINT_CLICK_RADIUS_PIXELS && 
								clickedPoint.getX()<=p.getX()+POINT_CLICK_RADIUS_PIXELS &&
								clickedPoint.getY()>=p.getY()-POINT_CLICK_RADIUS_PIXELS && 
								clickedPoint.getY()<=p.getY()+POINT_CLICK_RADIUS_PIXELS;

					})
					.findFirst();

			if(point.isPresent()) {
				JPopupMenu popup = createPopup(point.get());
				popup.show(imageLabel, e.getX(), e.getY());
			}

		}
		
		private synchronized void updateTag(Landmark tag, int newIndex) {

			ThreadManager.getInstance().execute(()->{
				cell.getPrimaryNucleus().setLocked(false);

				try {
					cell.getPrimaryNucleus().setLandmark(tag, newIndex);
				} catch (IndexOutOfBoundsException | MissingProfileException | MissingLandmarkException
						| ProfileException e) {
					LOGGER.log(Level.SEVERE, "Unable to set landmark in cell", e);
				}

				cell.getPrimaryNucleus().updateDependentStats();
				cell.getPrimaryNucleus().setLocked(true);
				cellUpdateHandler.fireCelllUpdateEvent(cell, dataset);
				createImage();
			});
		}

		private synchronized JPopupMenu createPopup(IPoint point) {
			JPopupMenu popupMenu = new JPopupMenu("Popup");
			if(cell==null)
				return popupMenu;

			addSegmentsToPopup(popupMenu, point);

			popupMenu.addSeparator();

			addTagsToPopup(popupMenu, point);


			return popupMenu;
		}
		
		/**
		 * Add segment update options to the popup menu, coloured
		 * by segment
		 * @param popupMenu
		 * @param point
		 */
		private void addSegmentsToPopup(JPopupMenu popupMenu, IPoint point) {
			try {
				int rawIndex = cell.getPrimaryNucleus().getBorderIndex(point);

				int rpIndex = cell.getPrimaryNucleus().getBorderIndex(Landmark.REFERENCE_POINT);

				// Get the index of the clicked point in the RP-indexed profile
				int index = cell.getPrimaryNucleus().wrapIndex(rawIndex-rpIndex);

				IProfileSegment seg = cell.getPrimaryNucleus().getProfile(ProfileType.ANGLE)
						.getSegmentContaining(rawIndex);


				IProfileSegment prev = seg.prevSegment();
				IProfileSegment next = seg.nextSegment();

				JMenuItem prevItem = new JMenuItem("Extend "+prev.getName()+" to here");
				prevItem.setBorder(BorderFactory.createLineBorder(ColourSelecter.getColor(prev.getPosition()), 3));
				prevItem.setBorderPainted(true);

				prevItem.addActionListener(e->{
					LOGGER.fine(String.format("Updating segment %s start to %d", next.getID(), index));
					fireSegmentEvent(seg.getID(), index, SegmentUpdateType.MOVE_START_INDEX);
					cellUpdateHandler.fireCelllUpdateEvent(cell, dataset);
					createImage();
				});
				popupMenu.add(prevItem);

				popupMenu.add(Box.createVerticalStrut(2)); // stop borders touching

				JMenuItem nextItem = new JMenuItem("Extend "+next.getName()+" to here");
				nextItem.setBorder(BorderFactory.createLineBorder(ColourSelecter.getColor(next.getPosition()), 3));
				nextItem.setBorderPainted(true);

				nextItem.addActionListener(e->{
					LOGGER.fine(String.format("Updating segment %s start to %d", next.getID(), index));
					fireSegmentEvent(next.getID(), index, SegmentUpdateType.MOVE_START_INDEX);
					cellUpdateHandler.fireCelllUpdateEvent(cell, dataset);
					createImage();
				});
				popupMenu.add(nextItem);
			} catch (MissingProfileException | MissingLandmarkException | ProfileException e) {
				LOGGER.log(Loggable.STACK, "Cannot get border tag index", e);
			}
		}
		
		/**
		 * Add tags to the popup menu
		 * @param popupMenu
		 */
		private void addTagsToPopup(JPopupMenu popupMenu, IPoint point) {
			List<Landmark> tags = dataset.getCollection().getProfileCollection().getLandmarks();

			Collections.sort(tags);

			for (Landmark tag : tags) {
				// Colour the menu item by tag colour
				JMenuItem item = new JMenuItem("Move "+tag.toString().toLowerCase()+" here");
				item.setBorder(BorderFactory.createLineBorder(ColourSelecter.getColour(tag), 3));
				item.setBackground(ColourSelecter.getColour(tag).darker());
				item.setBorderPainted(true);
				item.setForeground(Color.WHITE);
				item.setOpaque(true);

				item.addActionListener(a -> {
					int pIndex = cell.getPrimaryNucleus().getBorderIndex(point);
					updateTag(tag, pIndex);
					repaint();
				});
				popupMenu.add(item);
				popupMenu.add(Box.createVerticalStrut(2)); // stop borders touching
			}

			// Find border tags with rulesets that have not been assigned in the median
			List<Landmark> unassignedTags = new ArrayList<>();
			for (Landmark tag : Landmark.defaultValues()) {
				if (!tags.contains(tag)) {
					unassignedTags.add(tag);
				}
			}

			if (!unassignedTags.isEmpty()) {
				Collections.sort(unassignedTags);

				popupMenu.addSeparator();

				for (Landmark tag : unassignedTags) {
					JMenuItem item = new JMenuItem("Set "+tag.toString().toLowerCase()+" here");
					item.setForeground(Color.DARK_GRAY);

					item.addActionListener(a -> {
						int pIndex = cell.getPrimaryNucleus().getBorderIndex(point);
						updateTag(tag, pIndex);
					});
					popupMenu.add(item);
				}
			}
		}


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


		// Find the point that was clicked
		Optional<IPoint> point = cell.getPrimaryNucleus().getBorderList()
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
			// Highlight the border depending on what border tags are present
			try {

				if(cell.getPrimaryNucleus().hasLandmark(Landmark.TOP_VERTICAL) && 
						cell.getPrimaryNucleus().getBorderPoint(Landmark.TOP_VERTICAL).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Landmark.TOP_VERTICAL));
				}
				if(cell.getPrimaryNucleus().hasLandmark(Landmark.BOTTOM_VERTICAL) && 
						cell.getPrimaryNucleus().getBorderPoint(Landmark.BOTTOM_VERTICAL).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Landmark.BOTTOM_VERTICAL));
				}
				if(cell.getPrimaryNucleus().hasLandmark(Landmark.REFERENCE_POINT) && 
						cell.getPrimaryNucleus().getBorderPoint(Landmark.REFERENCE_POINT).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Landmark.REFERENCE_POINT));
				}
				if(cell.getPrimaryNucleus().hasLandmark(Landmark.ORIENTATION_POINT) && 
						cell.getPrimaryNucleus().getBorderPoint(Landmark.ORIENTATION_POINT).overlapsPerfectly(point.get())) {
					g2.setColor(ColourSelecter.getColour(Landmark.ORIENTATION_POINT));
				}

			} catch (MissingLandmarkException e) {
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
