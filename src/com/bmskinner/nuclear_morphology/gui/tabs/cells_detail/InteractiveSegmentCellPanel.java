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
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.analysis.image.AbstractImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.InterfaceUpdater;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent.SegmentUpdateType;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ImageProcessor;

public class InteractiveSegmentCellPanel extends InteractiveCellPanel {
	
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
		finer("Redrawing cell image");
		InterfaceUpdater u = () ->{
			output = null;
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
				an2.annotateSegmentsOnCroppedNucleus(n);
			}    
			
			imageLabel.setIcon(an2.toImageIcon());
			input = an2.toProcessor().getBufferedImage();
			sourceWidth = an.toProcessor().getWidth();
			sourceHeight = an.toProcessor().getHeight();
		};
		new Thread(u).start(); // avoid thread manager
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
		@Override
        public synchronized void mouseWheelMoved(MouseWheelEvent e) {
			if(imageLabel.getIcon()==null)
				return;
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) ==
                InputEvent.CTRL_DOWN_MASK){
            	int temp = smallRadius +( 1*e.getWheelRotation());
            	temp = temp>100?100:temp;
            	temp = temp<5?5:temp;
                smallRadius = temp;
            } else {
            	int temp = bigRadius +( 3 * e.getWheelRotation());
            	temp = temp>200?200:temp;
            	temp = temp<10?10:temp;
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
			updateImage(p.getXAsInt(), p.getYAsInt());
		}
		
		@Override
		public synchronized void mouseClicked(MouseEvent e) {
			if(imageLabel.getIcon()==null)
				return;
			IPoint clickedPoint = translatePanelLocationToSourceImage(e.getX(), e.getY());
			
			// Not a circle around the valid point to click, but close enough
			Optional<IBorderPoint> point = cell.getNucleus().getBorderList()
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
		
		private synchronized JPopupMenu createPopup(IBorderPoint point) {
			JPopupMenu popupMenu = new JPopupMenu("Popup");
			if(cell==null)
				return popupMenu;
			
			try {
				
				int rawIndex = cell.getNucleus().getBorderIndex(point);
				
				int rpIndex = cell.getNucleus().getBorderIndex(Tag.REFERENCE_POINT);
				
				// Get the index of the clicked point in the RP-indexed profile
				int index = cell.getNucleus().wrapIndex(rawIndex-rpIndex);

				IBorderSegment seg = cell.getNucleus().getProfile(ProfileType.ANGLE)
						.getSegmentContaining(rawIndex);

				
				IBorderSegment prev = seg.prevSegment();
				IBorderSegment next = seg.nextSegment();
				
				JMenuItem prevItem = new JMenuItem("Extend "+prev.getName()+" to here");
				prevItem.setBorder(BorderFactory.createLineBorder(ColourSelecter.getColor(prev.getPosition()), 3));
				prevItem.setBorderPainted(true);

				prevItem.addActionListener(e->{
					fine(String.format("Updating segment %s start to %d", next.getID(), index));
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
					fine(String.format("Updating segment %s start to %d", next.getID(), index));
					fireSegmentEvent(next.getID(), index, SegmentUpdateType.MOVE_START_INDEX);
					cellUpdateHandler.fireCelllUpdateEvent(cell, dataset);
					createImage();
				});
				popupMenu.add(nextItem);
				
			} catch (UnavailableProfileTypeException | UnavailableBorderTagException e) {
				stack("Cannot get border tag index", e);
			}
			return popupMenu;
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
