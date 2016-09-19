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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.chart.JFreeChart;

import charting.charts.DraggableOverlayChartPanel;
import charting.charts.ExportableChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.charts.PositionSelectionChartPanel;
import charting.charts.RectangleOverlayObject;

/**
 * This holds two JFreeChart ChartPanels. One is an overview, with a draggable
 * overlay to choose the region to focus on in the second chart. Is able to fire 
 * BorderTagEvents to registered listeners. Note, this class does not handle layout of the
 * panels.
 * @author bms41
 *
 */
public abstract class DualChartPanel 
	implements SignalChangeListener, 
			   SegmentEventListener, 
			   ChartSetEventListener {
	
	protected ExportableChartPanel chartPanel;
	
	protected PositionSelectionChartPanel rangePanel;
	
	protected List<Object> listeners = new ArrayList<Object>();
	
	public DualChartPanel(){
		
		JFreeChart profileChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);

		chartPanel.setMinimumDrawWidth(  0 );
		chartPanel.setMinimumDrawHeight( 0 );
//		chartPanel.addSignalChangeListener(this);
		chartPanel.addChartSetEventListener(this);
		
		
		/*
		 * A second chart panel at the south
		 * with a domain overlay crosshair to define the 
		 * centre of the zoomed range on the 
		 * centre chart panel 
		 */
		JFreeChart rangeChart = MorphologyChartFactory.getInstance().makeEmptyChart();
		rangePanel = new PositionSelectionChartPanel(rangeChart);
		rangePanel.setFixedAspectRatio(true);
		rangePanel.addSignalChangeListener(this);
		rangePanel.addChartSetEventListener(this);

		updateChartPanelRange();

	}
	
	
	public ExportableChartPanel getMainPanel(){
		return chartPanel;
	}
	
	public ExportableChartPanel getRangePanel(){
		return rangePanel;
	}
	
	public void restoreAutoBounds(){
		chartPanel.restoreAutoBounds();
		rangePanel.restoreAutoBounds();
	}
	
	public void setCharts(JFreeChart chart, JFreeChart rangeChart){
		
		if(chart == rangeChart){
			throw new IllegalArgumentException("Charts cannot be the same object");
		}
		this.chartPanel.setChart(chart);
		this.rangePanel.setChart(rangeChart);
		this.updateChartPanelRange();
	}
		
	/**
	 * Set the main chart panel domain range to centre on the 
	 * position in the range panel
	 */
	protected void updateChartPanelRange(){
		
		RectangleOverlayObject ob = rangePanel.getRectangleOverlay().getRectangle();
		
		double xmin = ob.getXMinValue();
		double xmax = ob.getXMaxValue();
		
		if(xmin < xmax){ // must have a positive range
			chartPanel.getChart().getXYPlot().getDomainAxis().setRange(xmin, xmax);
		}
		
		double ymin = ob.getYMinValue();
		double ymax = ob.getYMaxValue();
		
		if(ymin < ymax){ // must have a positive range
			chartPanel.getChart().getXYPlot().getRangeAxis().setRange(ymin, ymax);
		}
	}
	
	
	/**
	 * Toggle wait cursor on element
	 * @param b
	 */
	public void setAnalysing(boolean b){
		if(b){
			
//			for(Component c : this.getComponents()){
//				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); //new Cursor(Cursor.WAIT_CURSOR));
//			}
//			
//			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
		} else {
			
//			for(Component c : this.getComponents()){
//				c.setCursor(Cursor.getDefaultCursor());
//			}
//			this.setCursor(Cursor.getDefaultCursor());
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
