package gui.components;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.ui.RectangleEdge;

import charting.ChartComponents;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class DraggableOverlayChartPanel extends ChartPanel implements ChartMouseListener {
	

	private List<Object> listeners = new ArrayList<Object>();
	
	private SegmentedProfile profile = null;
	
	private List<CrosshairOverlay> lines = new ArrayList<CrosshairOverlay>(); // drwaing lines on the chart
	

	public DraggableOverlayChartPanel(JFreeChart chart, SegmentedProfile profile){
		super(chart);
		this.profile = profile;
		updateOverlays();
		this.setRangeZoomable(false);
		this.setDomainZoomable(false);

		this.addChartMouseListener(this);

		
		
		
	}
	
	private void clearOverlays(){
		
		for(Overlay o : lines){
			this.removeOverlay(o);
		}
	}
	
	private void updateOverlays(){
		/*
		 * Create an x-axis overlay for each segment start
		 */
		
		clearOverlays();
		
		if(profile!=null){
			try {

				CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
				int i=0;
				for(NucleusBorderSegment seg : profile.getSegments()){

					

					Color colour = ColourSelecter.getSegmentColor(i++);

					Crosshair xCrosshair = new Crosshair(Double.NaN, colour, ChartComponents.MARKER_STROKE);
					xCrosshair.setLabelVisible(false);
					xCrosshair.setValue(seg.getStartIndex());

					crosshairOverlay.addDomainCrosshair(xCrosshair);

				}
				this.addOverlay(crosshairOverlay);
				lines.add(crosshairOverlay);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
	}
	
	public void setChart(JFreeChart chart, SegmentedProfile profile){
		super.setChart(chart);
		this.profile = profile;
		updateOverlays();
		
		
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void chartMouseMoved(ChartMouseEvent e) {
		final int x = e.getTrigger().getX();
        final int y = e.getTrigger().getY();
        
//        IJ.log("x = "+x+"  y = "+y);
        
        Rectangle2D dataArea = this.getScreenDataArea(); 
        ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
        
        boolean isOverLine = false;
        for(CrosshairOverlay o : lines){
        	// only display a hand if the cursor is over the items
        	
        	List<Crosshair> crosshairs = o.getDomainCrosshairs();
        	for(Crosshair c :  crosshairs){
        	
        		
        		// Turn the chart coordinates into panel coordinates
        		double rectangleX = xAxis.valueToJava2D(c.getValue(), dataArea, 
                        RectangleEdge.BOTTOM);
        		
        		
        		
        		
        		final Rectangle bounds = new Rectangle( (int)rectangleX-5, 
        				(int) dataArea.getMinY(), 
        				(int) 10,   
        				(int) dataArea.getHeight() );
        		
//        		IJ.log("J2D x = "+rectangleX+"; y = "+dataArea.getMinY()+";  = "+);

        		if (bounds != null && bounds.contains(x, y)) {
        			isOverLine = true;
        		}
        	}
        }
        
        if (isOverLine) {
			this.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
		} else {
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
        
		
	}
}
