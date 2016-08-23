/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package gui.components.panels;

import gui.BorderTagEventListener;
import gui.ChartSetEvent;
import gui.ChartSetEventListener;
import gui.SegmentEvent;
import gui.SegmentEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.BorderTagEvent;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import charting.charts.DraggableOverlayChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.charts.PositionSelectionChartPanel;
import charting.charts.RectangleOverlayObject;

/**
 * This holds two JFreeChart ChartPanels. One is an overview, with a draggable
 * overlay to choose the region to focus on in the second chart. Is able to fire 
 * BorderTagEvents to registered listeners
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class DualChartPanel extends JPanel implements SignalChangeListener, SegmentEventListener, ChartSetEventListener {
	
	protected DraggableOverlayChartPanel chartPanel;
	
	protected PositionSelectionChartPanel rangePanel;
	
	List<Object> listeners = new ArrayList<Object>();
	
	public DualChartPanel(){
		
		
		super();
		
		this.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth  = 1; 
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;      //reset to default
		c.weightx = 1.0; 
		c.weighty = 0.7;

		
		JFreeChart profileChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);

		chartPanel.setMinimumDrawWidth(  0 );
		chartPanel.setMinimumDrawHeight( 0 );
		chartPanel.addSignalChangeListener(this);
		chartPanel.addChartSetEventListener(this);
		this.add(chartPanel, c);
		
		
		/*
		 * A second chart panel at the south
		 * with a domain overlay crosshair to define the 
		 * centre of the zoomed range on the 
		 * centre chart panel 
		 */
		JFreeChart rangeChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		rangePanel = new PositionSelectionChartPanel(rangeChart);
		rangePanel.addSignalChangeListener(this);
		rangePanel.addChartSetEventListener(this);

		c.weighty = 0.3;
		c.gridx = 0;
		c.gridy = 1;
		this.add(rangePanel, c);
		updateChartPanelRange();
		
		
	}
	
	
	public ChartPanel getMainPanel(){
		return chartPanel;
	}
	
	public ChartPanel getRangePanel(){
		return rangePanel;
	}
	
	public void setCharts(JFreeChart chart, JFreeChart rangeChart){
		this.chartPanel.setChart(chart);
		this.rangePanel.setChart(rangeChart);
		this.updateChartPanelRange();
	}
		
	/**
	 * Set the main chart panel domain range to centre on the 
	 * position in the range panel
	 */
	protected void updateChartPanelRange(){
		
		RectangleOverlayObject ob = rangePanel.getDomainRectangleOverlay();
		
		double min = ob.getMinValue();
		double max = ob.getMaxValue();
		
		chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
	}
	
	
	/**
	 * Toggle wait cursor on element
	 * @param b
	 */
	public void setAnalysing(boolean b){
		if(b){
			
			for(Component c : this.getComponents()){
				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); //new Cursor(Cursor.WAIT_CURSOR));
			}
			
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
		} else {
			
			for(Component c : this.getComponents()){
				c.setCursor(Cursor.getDefaultCursor());
			}
			this.setCursor(Cursor.getDefaultCursor());
		}
	}
	
	
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {		
		
		// Change the range of the main chart based on the lower chart  
		if(event.type().contains("UpdatePosition") && event.getSource().equals(rangePanel)){
			
			updateChartPanelRange();
		}

	}
	
	
	public synchronized void addBorderTagEventListener(BorderTagEventListener l){
		listeners.add(l);
	}
	
	public synchronized void removeBorderTagEventListener(BorderTagEventListener l){
		listeners.remove(l);
	}
	
	protected synchronized void fireBorderTagEvent(BorderTagEvent e){
		for(Object l : listeners){
			((BorderTagEventListener) l).borderTagEventReceived(e);
		}
	}
	
//	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
//        listeners.add( l );
//    }
//    
//    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
//        listeners.remove( l );
//    }
//    
//    protected synchronized void fireSignalChangeEvent(String message) {
//    	
//        SignalChangeEvent event = new SignalChangeEvent( this, message, this.getClass().getSimpleName());
//        Iterator<Object> iterator = listeners.iterator();
//        while( iterator.hasNext() ) {
//            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
//        }
//    }
//    
//    protected synchronized void fireSignalChangeEvent(SignalChangeEvent event) {
//    	Iterator<Object> iterator = listeners.iterator();
//    	while( iterator.hasNext() ) {
//    		( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
//    	}
//    }
    
    public synchronized void addSegmentEventListener(SegmentEventListener l){
		listeners.add(l);
	}
	
	public synchronized void removeSegmentEventListener(SegmentEventListener l){
		listeners.remove(l);
	}
	
	protected synchronized void fireSegmentEvent(SegmentEvent e){
		for(Object l : listeners){
			((SegmentEventListener) l).segmentEventReceived(e);
		}
	}
	
	protected synchronized void fireSegmentEvent(UUID id, int index, int type){
		SegmentEvent e = new SegmentEvent(this, id, index, type);
		
		for(Object l : listeners){
			((SegmentEventListener) l).segmentEventReceived(e);
		}
	}
	
	
	@Override
	public void segmentEventReceived(SegmentEvent event) {
		for(Object l : listeners){
			((SegmentEventListener) l).segmentEventReceived(event);
		}
	}
	
	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		
		// One of the two charts was set - ensure the charts remain coupled
		this.updateChartPanelRange();
		
	}

}
