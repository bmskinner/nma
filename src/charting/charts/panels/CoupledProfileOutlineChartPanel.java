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

package charting.charts.panels;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import logging.Loggable;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.ui.RectangleEdge;

import components.AbstractCellularComponent;
import components.ICell;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.BorderPoint;
import components.nuclear.IBorderPoint;

/**
 * This is designed to coordinate crosshair positions between two chart panels,
 * one showing a profile and the other showing a cell component outline. The
 * positions of border points can be tracked between the two charts
 * @author bms41
 *
 */
public class CoupledProfileOutlineChartPanel implements Loggable{
	
	private List<Object> listeners = new ArrayList<Object>();
	
	
	private ChartPanel profileChart;
	private ChartPanel outlineChart;
	
	private CrosshairOverlay profileCrosshairs; 
	private CrosshairOverlay outlineCrosshairs; 
	
	protected Crosshair xCrosshairProfile;
	protected Crosshair yCrosshairProfile;
	
	protected Crosshair xCrosshairOutline;
	protected Crosshair yCrosshairOutline;
	
	private ICell cell;
	
	
	public CoupledProfileOutlineChartPanel(ChartPanel profileChart, ChartPanel outlineChart, ICell cell){
		this.profileChart = profileChart;
		this.outlineChart = outlineChart;
		this.cell = cell;
		
		Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0);
		
		profileCrosshairs = new CrosshairOverlay();
		outlineCrosshairs = new CrosshairOverlay();
		
		xCrosshairProfile = new Crosshair(0.0, Color.DARK_GRAY, dashed);
		yCrosshairProfile = new Crosshair(0.0, Color.DARK_GRAY, dashed);
		
		profileCrosshairs.addDomainCrosshair(xCrosshairProfile);
		profileCrosshairs.addRangeCrosshair( yCrosshairProfile);
		
		xCrosshairOutline = new Crosshair(0.0, Color.DARK_GRAY, dashed);		
		yCrosshairOutline = new Crosshair(0.0, Color.DARK_GRAY, dashed);
		
		outlineCrosshairs.addDomainCrosshair(xCrosshairOutline);
		outlineCrosshairs.addRangeCrosshair( yCrosshairOutline);
		
		profileChart.addOverlay(profileCrosshairs);
		outlineChart.addOverlay(outlineCrosshairs);
		
		addMouseHandlers();

		
	}
	
	public void setCell(ICell cell){
		this.cell = cell;
	}
	
	private void addMouseHandlers(){
		profileChart.addMouseMotionListener( new MouseAdapter(){
			@Override
			public void mouseMoved(MouseEvent e){
				
				int xValue = getProfileIndexFromChart(e.getX());
				
				if(xValue<0 || xValue >= cell.getNucleus().getBorderLength()){
					return;
				}
				// Find the y-value in the chart at this point
				// Take from the angle profile directly
				double yValue = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).get(xValue);
				
				// Find the index of the border point with the current profile chart x value
				IBorderPoint p = getPointFromProfileIndex(xValue);
				
				// Update the crosshairs on both charts
				xCrosshairProfile.setValue(xValue);
				yCrosshairProfile.setValue(yValue);
				
				xCrosshairOutline.setValue(p.getX());
				yCrosshairOutline.setValue(p.getY());
			}
		});
		
		profileChart.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount()==1){
					
					int xValue = getProfileIndexFromChart(e.getX());
					
					IBorderPoint p = getPointFromProfileIndex(xValue);
					fireBorderPointEvent(p);
				}
			}
		});
	}
	
	private int getProfileIndexFromChart(double xPosition){
		// Convert the pixel coordinates to value coordinates 
		Rectangle2D dataArea = profileChart.getScreenDataArea();
		ValueAxis   xAxis    = profileChart.getChart().getXYPlot().getDomainAxis();
		
		int xValue = (int) xAxis.java2DToValue(xPosition, dataArea, 
				RectangleEdge.BOTTOM);
		return xValue;
	}
	
	private IBorderPoint getPointFromProfileIndex(int index){
		// Find the index of the border point with the current profile chart x value
		int rpIndex = cell.getNucleus().getBorderIndex(Tag.REFERENCE_POINT);
		int xIndex  = AbstractCellularComponent.wrapIndex(index+rpIndex, cell.getNucleus().getBorderLength()); 
		
		// Get that border point
		return cell.getNucleus().getBorderPoint(xIndex);
	}
	
	public ChartPanel getProfilePanel(){
		return this.profileChart;
	}
	
	public ChartPanel getOutlinePanel(){
		return this.outlineChart;
	}
	
	public synchronized void addBorderPointEventListener( BorderPointEventListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeBorderPointEventListener( BorderPointEventListener l ) {
        listeners.remove( l );
    } 
    
    protected synchronized void fireBorderPointEvent(IBorderPoint p) {
    	
    	BorderPointEvent event = new BorderPointEvent( this, p);
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (BorderPointEventListener) iterator.next() ).borderPointEventReceived( event );
        }
    }
	
	
	@SuppressWarnings("serial")
	public class BorderPointEvent extends EventObject {

		private IBorderPoint p;
		
		public BorderPointEvent(Object source, IBorderPoint p) {
			super(source);
			this.p = p;
		}
		
		public IBorderPoint getPoint(){
			return p;
		}
	}
	
	public interface BorderPointEventListener {
		
		public void borderPointEventReceived(BorderPointEvent event);
			
	} 
	
	

}
