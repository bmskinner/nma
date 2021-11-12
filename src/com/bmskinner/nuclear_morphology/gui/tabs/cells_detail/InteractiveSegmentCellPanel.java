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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.bmskinner.nuclear_morphology.analysis.image.AbstractImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ImageClickListener;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.panels.MagnifiableImagePanel;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent.SegmentUpdateType;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.gui.painters.CellImagePainter;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Displays and edit segments in a single cell
 * @author ben
 *
 */
public class InteractiveSegmentCellPanel extends InteractiveCellPanel {

	private static final Logger LOGGER = Logger.getLogger(InteractiveSegmentCellPanel.class.getName());
	
	private MagnifiableImagePanel imagePanel;
	
	/** Track the scaling ratio between the original cell and the scaled image */
//	private int cellImageWidth;
//	private int annImageWidth;
	private double scaleRatio;

	private transient List<SegmentEventListener> listeners = new ArrayList<>();

	public InteractiveSegmentCellPanel(CellUpdatedEventListener parent){
		super(parent);
		addComponentListener(new ResizeListener(this));
	}
	
	@Override
	protected synchronized void createImage() {
		LOGGER.finer( "Redrawing cell image");

		if(dataset==null || cell==null || component==null) {
			return;
		}

		ImageProcessor ip;
		try{
			ip = component.getImage();
		} catch(UnloadableImageException e){
			ip = AbstractImageFilterer.createWhiteColorProcessor(
					(int)component.getMaxX()+Imageable.COMPONENT_BUFFER, 
					(int)component.getMaxY()+Imageable.COMPONENT_BUFFER);
		}

		// Crop to the relevant part of the image
		AbstractImageFilterer an = new ImageAnnotator(ip).crop(cell);

		// Store the original image width after cropping
		int cellImageWidth = an.toProcessor().getWidth();
		
		// Expand or shrink the canvas to fit the panel
		an = new ImageAnnotator(an.toProcessor(), getWidth(), getHeight());
		BufferedImage image = an.toBufferedImage();

		scaleRatio = image.getWidth()/(double)cellImageWidth;

		if(imagePanel==null) {
			imagePanel = new MagnifiableImagePanel(image, new CellImagePainter(cell, cellImageWidth));
			imagePanel.addImageClickListener(new ImageClickAdapter());
		}
		else
			imagePanel.set(image, new CellImagePainter(cell, cellImageWidth));

		
		add(imagePanel, BorderLayout.CENTER);
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
	private class ImageClickAdapter implements ImageClickListener {
		
		/** When a clicking a feature in an image, allow the clicked point to be 
		 * this many pixels away from the true point */
		private static final double POINT_CLICK_RADIUS_PIXELS = 0.4;

		@Override
		public void imageClicked(int x, int y) {
			if(imagePanel==null)
				return;
			
			// Translate to coordinates in the cell image (remove scaling)
			double ix = (x/scaleRatio);
			double iy = (y/scaleRatio);

			double cx = ix - Imageable.COMPONENT_BUFFER + cell.getPrimaryNucleus().getBase().getX();
			double cy = iy - Imageable.COMPONENT_BUFFER + cell.getPrimaryNucleus().getBase().getY();
			
			// Not a circle around the valid point to click, but close enough
			Optional<IPoint> point = cell.getPrimaryNucleus().getBorderList()
					.stream()
					.filter(p ->
						cx >= p.getX()-POINT_CLICK_RADIUS_PIXELS && 
						cx <= p.getX()+POINT_CLICK_RADIUS_PIXELS &&
						cy >= p.getY()-POINT_CLICK_RADIUS_PIXELS && 
						cy <= p.getY()+POINT_CLICK_RADIUS_PIXELS
					)
					.findFirst();

			if(point.isPresent()) {
				JPopupMenu popup = createPopup(point.get());
				popup.show(imagePanel, x, y);
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
	
	/**
	 * Ensure the image is repainted on resizing of the panel
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	private class ResizeListener extends ComponentAdapter implements ComponentListener {
        public ResizeListener(JComponent component) {
			super(component);
		}

		@Override
		public void componentResized(ComponentEvent e) {
            createImage();
            repaint();
        }

		@Override
		public void componentMoved(ComponentEvent e) {}

		@Override
		public void componentShown(ComponentEvent e) {}

		@Override
		public void componentHidden(ComponentEvent e) {}

		@Override
		public Object getValueAt(int row, int column) {
			return null;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public boolean hasFocus() {
			return false;
		}

		@Override
		public boolean isSelected() {
			return false;
		}

		@Override
		public boolean isEditable() {
			return false;
		}
}

}
