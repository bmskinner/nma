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
package charting.charts;

import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import ij.IJ;
import jebl.evolution.graphs.Edge;
import jebl.evolution.graphs.Node;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;

/**
 * This extension of the ChartPanel provides a new MouseAdapter
 * to mark selected locations rather than zooming the chart
 *
 */
@SuppressWarnings("serial")
public class SelectableChartPanel extends ExportableChartPanel implements SignalChangeListener, ChartMouseListener {
	
	private String name = null;
	MouseMarker mouseMarker = null;
	public static final String SOURCE_COMPONENT = "SelectableChartPanel"; 
	private List<Object> listeners = new ArrayList<Object>();
	List<Line2D.Double> lines = new ArrayList<Line2D.Double>(); // drwaing lines on the chart
	
	private Crosshair xCrosshair;

	public SelectableChartPanel(JFreeChart chart, String name){
		super(chart);
		this.name = name;
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);
		mouseMarker = new MouseMarker(this);
		this.addSignalChangeListener(mouseMarker);
		this.addMouseListener(mouseMarker);
		this.addChartMouseListener(this);

	    CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
	    this.xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
	    this.xCrosshair.setLabelVisible(false);

	    crosshairOverlay.addDomainCrosshair(xCrosshair);
	    this.addOverlay(crosshairOverlay);

		
	}
	

	
	public String getName(){
		return this.name;
	}
	
	public Double getGateLower(){
		return mouseMarker.getMarkerStart();
	}
	
	public Double getGateUpper(){
		return mouseMarker.getMarkerEnd();
	}
	
	@Override
	//override the default zoom to keep aspect ratio
	public void zoom(java.awt.geom.Rectangle2D selection){
		
	}
	
	@Override
	public void setChart(JFreeChart chart){
		super.setChart(chart);
		this.removeMouseListener(mouseMarker);
		mouseMarker = new MouseMarker(this);
		this.addMouseListener(mouseMarker);
		mouseMarker.addSignalChangeListener(this);
	}
		
	public void addLine(Line2D.Double line){
		clearLines();
		this.lines.add(line);
	}
	
	public void clearLines(){
		this.lines = new ArrayList<Line2D.Double>();
	}
	
	@Override
	public void paint(Graphics g){
				
		super.paint(g);
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(Color.BLACK);
		g2.setStroke(new BasicStroke(2f));
		for(Line2D.Double line : lines){
			g2.draw(line);
		}
	}
	
	private final static class MouseMarker extends MouseAdapter implements SignalChangeListener{
        private Marker marker;
        private Double markerStart = Double.NaN;
        private Double markerEnd = Double.NaN;
        private final XYPlot plot;
        private final JFreeChart chart;
        private  final ChartPanel panel;
        
        public static final String SOURCE_COMPONENT = "MouseMarker"; 
    	private List<Object> listeners = new ArrayList<Object>();

        public MouseMarker(ChartPanel panel) {
            this.panel = panel;
            this.chart = panel.getChart();
            this.plot = (XYPlot) chart.getPlot();
        }
        
        public Double getMarkerEnd(){
        	return markerEnd.doubleValue();
        }
        
        public Double getMarkerStart(){
        	return markerStart.doubleValue();
        }

        private void updateMarker(){
            if (marker != null){
                plot.removeDomainMarker(marker,Layer.BACKGROUND);
            }
            
            
            if (!  ( markerStart.isNaN() && markerEnd.isNaN())  ){
            	
                if ( markerEnd > markerStart){
                    marker = new IntervalMarker(markerStart, markerEnd);
                    marker.setPaint(new Color(128, 128, 128, 255));
                    marker.setAlpha(0.5f);
                    plot.addDomainMarker(marker,Layer.BACKGROUND);
                    
                    if(!markerEnd.isNaN()){
                    	fireSignalChangeEvent("MarkerPositionUpdated");
                    }
                }
                
            }
        }

        private Double getPosition(MouseEvent e){
            Point2D p = panel.translateScreenToJava2D( e.getPoint()); // Translates a panel (component) location to a Java2D point.
            Rectangle2D plotArea = panel.getChartRenderingInfo().getPlotInfo().getDataArea();// panel.getScreenDataArea(); // get the area covered by the panel
            
            XYPlot plot = (XYPlot) chart.getPlot();
            
            return plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            markerEnd = getPosition(e);
//            IJ.log("Mouse up: marker end "+markerEnd);
            updateMarker();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            markerStart = getPosition(e);
        }
        
        private synchronized void fireSignalChangeEvent(String message) {
//        	IJ.log("Mouse marker has fired a change");
            SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT );
            Iterator<Object> iterator = listeners.iterator();
            while( iterator.hasNext() ) {
                ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
            }
        }

		@Override
		public void signalChangeReceived(SignalChangeEvent event) {
			// TODO Auto-generated method stub
			
		}
		
		public synchronized void addSignalChangeListener( SignalChangeListener l ) {
	        listeners.add( l );
	    }
	    
	    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
	        listeners.remove( l );
	    }
    }
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		// pass messages up
		if(event.type().equals("MarkerPositionUpdated")){
			fireSignalChangeEvent("MarkerPositionUpdated");
		}
		
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT );
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }

	@Override
	public void chartMouseClicked(ChartMouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent e) {
					
		Rectangle2D dataArea = this.getScreenDataArea();
        JFreeChart chart = e.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        double x = xAxis.java2DToValue(e.getTrigger().getX(), dataArea, 
                RectangleEdge.BOTTOM);
        this.xCrosshair.setValue(x);		
	
		
	}

}
