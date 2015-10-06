package gui.components;

import ij.IJ;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.Layer;

/**
 * This extension of the ChartPanel provides a new MouseAdapter
 * to mark selected locations rather than zooming the chart
 *
 */
@SuppressWarnings("serial")
public class SelectableChartPanel extends ChartPanel {
	
	MouseMarker mouseMarker = null;

	public SelectableChartPanel(JFreeChart chart){
		super(chart);

		this.setRangeZoomable(false);
		this.setDomainZoomable(false);
		mouseMarker = new MouseMarker(this);
		this.addMouseListener(mouseMarker);

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
	}
	
	private final static class MouseMarker extends MouseAdapter{
        private Marker marker;
        private Double markerStart = Double.NaN;
        private Double markerEnd = Double.NaN;
        private final XYPlot plot;
        private final JFreeChart chart;
        private  final ChartPanel panel;

        public MouseMarker(ChartPanel panel) {
            this.panel = panel;
            this.chart = panel.getChart();
            this.plot = (XYPlot) chart.getPlot();
        }

        private void updateMarker(){
            if (marker != null){
                plot.removeDomainMarker(marker,Layer.BACKGROUND);
//                IJ.log("Marker removed");
            }
            
            
            if (!( markerStart.isNaN() && markerEnd.isNaN())){
            	
                if ( markerEnd > markerStart){
                    marker = new IntervalMarker(markerStart, markerEnd);
                    marker.setPaint(new Color(128, 128, 128, 255));
                    marker.setAlpha(0.5f);
                    plot.addDomainMarker(marker,Layer.BACKGROUND);
//                    IJ.log("Marker added: "+markerStart+"-"+markerEnd);
                }
                
            }
        }

        private Double getPosition(MouseEvent e){
            Point2D p = panel.translateScreenToJava2D( e.getPoint()); // Translates a panel (component) location to a Java2D point.
            Rectangle2D plotArea = panel.getScreenDataArea(); // get the area covered by the panel
//            Rectangle2D plotArea =  panel.getChartRenderingInfo().getPlotInfo().getDataArea();
            XYPlot plot = (XYPlot) chart.getPlot();
            
//            Converts a coordinate in Java2D space to the corresponding data value, assuming that the axis runs along one edge of the specified dataArea.
//            Parameters:
//                java2DValue - the coordinate in Java2D space.
//                area - the area in which the data is plotted.
//                edge - the edge along which the axis lies.
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
//            IJ.log("Mouse down: marker start "+markerStart);
//            updateMarker();
        }
    }

}
