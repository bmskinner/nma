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
package com.bmskinner.nuclear_morphology.charting.charts.panels;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.RectangleEdge;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent.SegmentUpdateType;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This chart panel provides a list of draggable markers as crosshair overlays,
 * corresponding to the start positions of segments in a segmented profile. A
 * dragged segment fires a SegmentEvent to registered listeners
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DraggableOverlayChartPanel extends ExportableChartPanel {
	
	private static final Logger LOGGER = Logger.getLogger(DraggableOverlayChartPanel.class.getName());

    private ISegmentedProfile profile = null;

    /** All the crosshairs displayed on the chart     */
    private List<SegmentCrosshair> crosses = new ArrayList<>(); // drawing lines on the chart

    /** The currently active crosshair */
    protected Crosshair xCrosshair;

    private volatile boolean isChartNormalised = false;

    protected Overlay overlay = null;

    public DraggableOverlayChartPanel(@NonNull final JFreeChart chart, 
    		final ISegmentedProfile profile, boolean normalised) {
        super(chart);
        this.profile = profile;
        this.isChartNormalised = normalised;
        updateOverlays();
        this.setRangeZoomable(false);
        this.setDomainZoomable(false);
    }

    /**
     * Toggle wait cursor on element
     * 
     * @param b
     */
    public synchronized void setAnalysing(boolean b) {
        if (b) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        } else {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Remove the current overlay if present
     * 
     */
    private synchronized void clearOverlays() {
        if (overlay != null) {
            this.removeOverlay(overlay);
        }
    }

    /**
     * Get the position of the crosshair in the domain axis
     * @return the position of the crosshair, or zero is there is no crosshair
     */
    public synchronized double getDomainCrosshairPosition() {
        if (xCrosshair != null) {
            LOGGER.finest( "Domain value is " + xCrosshair.getValue());
            return xCrosshair.getValue();
        }
        return 0;
    }
    
    private static double getRescaledIndex(IProfileSegment seg, int newLength) {
        return (float) seg.getStartIndex() / (float) (seg.getProfileLength()) * (float) newLength;
    }

    /**
     * Redraw the overlays at their approprate positions based on the segment start points
     */
    private synchronized void updateOverlays() {

        clearOverlays();
        
        if(profile==null)
        	return;

        try {
        	overlay = new CrosshairOverlay();
        	List<IProfileSegment> segments = profile.getOrderedSegments();
        	for(int i=0; i<profile.getSegmentCount(); i++) {
        		// don't draw the first segment marker (the RP); it can never be altered on this chart
        		if(i==0)
        			continue;

        		IProfileSegment seg = segments.get(i);

        		Paint colour = seg.isLocked() ? Color.DARK_GRAY : ColourSelecter.getColor(i);
        			
        		
        		double maxDomain = getMaximumDomainValue();
        		double normValue = profile.getFractionOfIndex(seg.getStartIndex())*maxDomain;
        		double xValue = isChartNormalised ? normValue : seg.getStartIndex();
        		LOGGER.finest("Crosshair "+seg.getName()+": "+xValue);
        		SegmentCrosshair xCrosshair = new SegmentCrosshair(xValue, colour,
        				ChartComponents.MARKER_STROKE, seg);
        		xCrosshair.setLabelVisible(false);

        		crosses.add(xCrosshair);
        		((CrosshairOverlay) overlay).addDomainCrosshair(xCrosshair);
        	}

        	addOverlay(overlay);

        } catch (Exception e1) {
        	LOGGER.warning("Error creating segment markers");
        	LOGGER.log(Loggable.STACK, "Error creating segment markers", e1);
        }

        this.revalidate();
        this.repaint();
    }

    public synchronized void setChart(JFreeChart chart, ISegmentedProfile profile, boolean normalised) {
        super.setChart(chart);

        setProfile(profile, normalised);
    }

    public synchronized void setProfile(ISegmentedProfile profile, boolean normalised) {

        clearOverlays();
        this.profile = profile;
        this.isChartNormalised = normalised;
        crosses = new ArrayList<>();
        overlay = null;
        updateOverlays();
        LOGGER.finer( "Profile has been set");
    }

    @Override
    public synchronized void setChart(JFreeChart chart) {
        super.setChart(chart);
    }

    @Override
    public synchronized void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {

            if (xCrosshair != null && !((SegmentCrosshair) xCrosshair).getSegment().isLocked()) {
                mouseIsDown = true;
                initThread();
            }
        }
    }
    
    /**
     * Get the maximum domain value and round it to the nearest 1000
     * @return
     */
    private double getMaximumDomainValue() {
		Number maxDomainValue = DatasetUtilities.findMaximumDomainValue(getChart().getXYPlot().getDataset());
		return Math.ceil(maxDomainValue.doubleValue()/1000)*1000;
    }

    @Override
    public synchronized void mouseReleased(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseIsDown = false;
            
            if(xCrosshair==null){
            	return;
            }

            // Get the normalised position
            double xNormValue = this.getDomainCrosshairPosition();

            LOGGER.finest("Domain value is " + xNormValue);
            
            // ignore any overlays dragged out of profile bounds
            if(xNormValue<0 || xNormValue>=getMaximumDomainValue())
            	return;
            
            int xValue = (int) Math.round(xNormValue);

            // Correct for normalisation
            if (isChartNormalised) {
            	xValue = profile.getIndexOfFraction(xNormValue / getMaximumDomainValue());
            	LOGGER.finest("Unnormalised value is: " + xValue);
            }

            // Get the segment associated with the point
            IProfileSegment seg = ((SegmentCrosshair) xCrosshair).getSegment();

            // Trigger the update
            if (seg != null) {
            	fireSegmentEvent(seg.getID(), xValue, SegmentUpdateType.MOVE_START_INDEX);
            }
        }
    }

    @Override
    public synchronized void mouseMoved(MouseEvent e) {

        final int x = e.getX();
        final int y = e.getY();

        if ( ! mouseIsDown ) {

            if (pointIsOverCrosshair(x, y)) {

                if (((SegmentCrosshair) xCrosshair).getSegment().isLocked()) {
                    this.setCursor(Cursor.getDefaultCursor());
                } else {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                }
            } else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private synchronized void updateActiveCrosshairLocation(int x, int y) {
        if (xCrosshair != null) {
            Rectangle2D dataArea = getScreenDataArea();
            JFreeChart chart = getChart();
            XYPlot plot = (XYPlot) chart.getPlot();
            ValueAxis xAxis = plot.getDomainAxis();
            double movex = xAxis.java2DToValue(x, dataArea, RectangleEdge.BOTTOM);
            xCrosshair.setValue(movex);
        }
    }

    protected synchronized boolean checkAndMark() {
        if (isRunning)
            return false;
        isRunning = true;
        return true;
    }

    protected void initThread() {
        if (checkAndMark()) {
        	Runnable r = () ->{
        		do {
                     // Make the overlay under the mouse follow the mouse
                    int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
                    int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;
                    updateActiveCrosshairLocation(x, y);
                } while (mouseIsDown);
                isRunning = false;
        	};
            new Thread(r).start();
        }
    }

    public synchronized void addSegmentEventListener(SegmentEventListener l) {
        listeners.add(l);
    }

    public synchronized void removeSegmentEventListener(SegmentEventListener l) {
        listeners.remove(l);
    }

    protected synchronized void fireSegmentEvent(SegmentEvent e) {
        for (Object l : listeners) {
            ((SegmentEventListener) l).segmentEventReceived(e);
        }
    }

    /**
     * Fire a segment event
     * 
     * @param id the segment ID
     * @param index the new index
     * @param type the type of change to make
     */
    protected synchronized void fireSegmentEvent(UUID id, int index, SegmentUpdateType type) {
        SegmentEvent e = new SegmentEvent(this, id, index, type);

        for (Object l : listeners) {
            ((SegmentEventListener) l).segmentEventReceived(e);
        }
    }

    /**
     * Checks if the given cursor position is over one of the crosshair overlays
     * for the chart
     * 
     * @param x
     * @param y
     * @return
     */
    private boolean pointIsOverCrosshair(int x, int y) {
        Rectangle2D dataArea = this.getScreenDataArea();
        ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
        xCrosshair = null;

        boolean isOverLine = false;

        if (overlay instanceof CrosshairOverlay) {
            List<Crosshair> crosshairs = ((CrosshairOverlay) overlay).getDomainCrosshairs();
            // only display a hand if the cursor is over the items
            for (Crosshair c : crosshairs) {

                // Turn the chart coordinates into panel coordinates
                double rectangleX = xAxis.valueToJava2D(c.getValue(), dataArea, RectangleEdge.BOTTOM);

                final Rectangle bounds = new Rectangle((int) rectangleX - 2, (int) dataArea.getMinY(), (int) 4,
                        (int) dataArea.getHeight());

                if (bounds != null && bounds.contains(x, y)) {
                    isOverLine = true;
                    xCrosshair = (SegmentCrosshair) c;
                }
            }
        }

        return isOverLine;
    }

    /**
     * A crosshair that links to a segment
     * @author ben
     *
     */
    private class SegmentCrosshair extends Crosshair {
        private IProfileSegment segment;

        public SegmentCrosshair(double d, Paint paint, Stroke stroke, IProfileSegment segment) {
            super(d, paint, stroke);
            this.segment = segment;
        }

        public IProfileSegment getSegment() {
            return segment;
        }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((segment == null) ? 0 : segment.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			SegmentCrosshair other = (SegmentCrosshair) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (segment == null) {
				if (other.segment != null)
					return false;
			} else if (!segment.equals(other.segment))
				return false;
			return true;
		}

		private DraggableOverlayChartPanel getOuterType() {
			return DraggableOverlayChartPanel.this;
		}
        
        
    }

    protected synchronized void fireSignalChangeEvent(String message) {

        SignalChangeEvent event = new SignalChangeEvent(this, message, this.getClass().getSimpleName());
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((EventListener) iterator.next()).eventReceived(event);
        }
    }

    public synchronized void addSignalChangeListener(EventListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(EventListener l) {
        listeners.remove(l);
    }

}
