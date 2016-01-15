package gui.components;

import ij.IJ;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

import stats.NucleusStatistic;
import components.Cell;
import components.generic.MeasurementScale;

@SuppressWarnings("serial")
public class CellBackgroundChartPanel extends ChartPanel {
	
	private Cell cell;

	public CellBackgroundChartPanel(JFreeChart chart){
		super(chart);
	}
	
	public void setCell(Cell c){
		this.cell = c;
	}
	
	
	@Override
	public void repaint(){
		
//		if(this.getChart()!=null){
//			if(this.getChart().getPlot()!=null){
//				setPlotArea();
//			}
//		}
		super.repaint();
	}
	
	private void setPlotArea(){
		if(cell!=null){
			try {
				XYPlot plot = (XYPlot) this.getChart().getPlot();

				
				double chartWidth = this.getWidth();
				double chartHeight = this.getHeight();

				double nucleusAspectRatio = cell.getNucleus().getStatistic(NucleusStatistic.ASPECT, MeasurementScale.PIXELS);

				double chartAspectRatio = chartWidth / chartHeight; 

				Rectangle2D dataArea = this.getChartRenderingInfo().getPlotInfo().getDataArea();

				double newWidth = dataArea.getWidth();
				double newHeight = dataArea.getHeight();

				if(chartAspectRatio > nucleusAspectRatio){

					// width is too high
					newWidth = dataArea.getHeight() * nucleusAspectRatio;


				} else {

					// height is too high
					newHeight = dataArea.getWidth() /  nucleusAspectRatio;
				}
				
				double newXDiff = dataArea.getWidth() - newWidth;
				double newX = dataArea.getX() + (newXDiff / 2);
				
				double newYDiff = dataArea.getHeight() - newHeight;
				double newY = dataArea.getY() + (newYDiff / 2);

				Rectangle2D newDataArea = new Rectangle2D.Double(newX, newY, newWidth, newHeight);

				this.getChartRenderingInfo().getPlotInfo().setDataArea(newDataArea);
				this.revalidate();
//				Graphics2D g2 = (Graphics2D) this.getGraphics();
//				plot.drawBackgroundImage(g2,  newDataArea);

			} catch (Exception e) {
				IJ.log("Error in resize");
			}
		}
	}
					
	@Override
	public void restoreAutoBounds() {
		XYPlot plot = (XYPlot) this.getChart().getPlot();
		
		double chartWidth = this.getWidth();
		double chartHeight = this.getHeight();
		double aspectRatio = chartWidth / chartHeight;
		
		// start with impossible values
		double xMin = chartWidth;
		double yMin = chartHeight;
//		
		double xMax = 0;
		double yMax = 0;
		
		// get the max and min values of the chart
		for(int i = 0; i<plot.getDatasetCount();i++){
			XYDataset dataset = plot.getDataset(i);
			
			xMax = DatasetUtilities.findMaximumDomainValue(dataset).doubleValue() > xMax
					? DatasetUtilities.findMaximumDomainValue(dataset).doubleValue()
					: xMax;
			
			xMin = DatasetUtilities.findMinimumDomainValue(dataset).doubleValue() < xMin
					? DatasetUtilities.findMinimumDomainValue(dataset).doubleValue()
					: xMin;
					
			yMax = DatasetUtilities.findMaximumRangeValue(dataset).doubleValue() > yMax
					? DatasetUtilities.findMaximumRangeValue(dataset).doubleValue()
					: yMax;
			
			yMin = DatasetUtilities.findMinimumRangeValue(dataset).doubleValue() < yMin
					? DatasetUtilities.findMinimumRangeValue(dataset).doubleValue()
					: yMin;
		}
		

		// find the ranges they cover
		double xRange = xMax - xMin;
		double yRange = yMax - yMin;
		
//		double aspectRatio = xRange / yRange;

		double newXRange = xRange;
		double newYRange = yRange;

		// test the aspect ratio
//		IJ.log("Old range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
		if( (xRange / yRange) > aspectRatio){
			// width is not enough
//			IJ.log("Too narrow: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
			newXRange = xRange * 1.1;
			newYRange = newXRange / aspectRatio;
		} else {
			// height is not enough
//			IJ.log("Too short: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
			newYRange = yRange * 1.1; // add some extra x space
			newXRange = newYRange * aspectRatio; // get the new Y range
		}
		

		// with the new ranges, find the best min and max values to use
		double xDiff = (newXRange - xRange)/2;
		double yDiff = (newYRange - yRange)/2;

		xMin -= xDiff;
		xMax += xDiff;
		yMin -= yDiff;
		yMax += yDiff;
//		IJ.log("New range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);

		plot.getRangeAxis().setRange(yMin, yMax);
		plot.getDomainAxis().setRange(xMin, xMax);	
	
	} 	
	
	private void updateBackgroundImage(){
		/*
		 * Update background images to the loaded cell dimensions
		 */
//		double maxX = Double.MIN_VALUE;
//		double maxY = Double.MIN_VALUE;
//		double minX = Double.MAX_VALUE;
//		double minY = Double.MAX_VALUE;
//		
//		for(int i = 0; i<this.getChart().getXYPlot().getDatasetCount(); i++){
//			maxX = Math.max(maxX,DatasetUtilities.findMaximumDomainValue(this.getChart().getXYPlot().getDataset(i)).doubleValue());
//			maxY = Math.max(maxY,DatasetUtilities.findMaximumRangeValue(this.getChart().getXYPlot().getDataset(i)).doubleValue());
//			minX = Math.min(minX,DatasetUtilities.findMinimumDomainValue(this.getChart().getXYPlot().getDataset(i)).doubleValue());
//			minY = Math.min(minY,DatasetUtilities.findMinimumRangeValue(this.getChart().getXYPlot().getDataset(i)).doubleValue());
//		
//		}
//		
//		
//		Graphics2D g2 = (Graphics2D) this.getGraphics();
//		
//		Rectangle2D dataArea = getScreenDataArea();
//		ValueAxis xAxis = plot.getDomainAxis();
//		ValueAxis yAxis = plot.getRangeAxis();
//		
//		int min2DX = (int) xAxis.valueToJava2D(minX, dataArea, RectangleEdge.BOTTOM);
//		int max2DX = (int) xAxis.valueToJava2D(maxX, dataArea, RectangleEdge.BOTTOM);
//		
//		int min2DY = (int) yAxis.valueToJava2D(minY, dataArea, RectangleEdge.BOTTOM);
//		int max2DY = (int) yAxis.valueToJava2D(maxY, dataArea, RectangleEdge.BOTTOM);
//
//		int w = max2DX - min2DX;
//		int h = max2DY > min2DY ? max2DY - min2DY : min2DY - max2DY; // when y axis is inverted
//		int x = min2DX;
//		int y = max2DY > min2DY ? min2DY : max2DY ; // when y axis is inverted
//					
//		
//		Rectangle2D area   = new Rectangle2D.Double(x, y, w, h);
//		
//		programLogger.log(Level.INFO, "Coordinates   :  X: "+minX+"-"+maxX+" Y: "+minY+"-"+maxY );
//		programLogger.log(Level.INFO, "Data area     : "+ dataArea.toString());
//		programLogger.log(Level.INFO, "New image area: "+ area.toString());
	}
}

