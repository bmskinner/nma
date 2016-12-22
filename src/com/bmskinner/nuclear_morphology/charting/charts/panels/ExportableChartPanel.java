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
package com.bmskinner.nuclear_morphology.charting.charts.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import com.bmskinner.nuclear_morphology.charting.charts.panels.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import com.bmskinner.nuclear_morphology.charting.datasets.ExportableBoxAndWhiskerCategoryDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.ShellResultDataset;
import com.bmskinner.nuclear_morphology.gui.ChartSetEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.Constants;

import ij.io.SaveDialog;


/**
 * This panel should add a right click menu item for 'Export'
 * This will extract the chart data, and save it to a desired location.
 * It also redraws the chart as the panel is resized for better UX.
 * The flag setFixedAspectRatio can be used to give the chart a fixed aspect
 * ratio. This replaces the dedicated FixedAspectRatioChartPanel class
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ExportableChartPanel extends ChartPanel implements Loggable, ChartSetEventListener {
		
	protected final List<Object> listeners = new ArrayList<Object>();
	
	/**
	 * Control if the axis scales should be set to maintain aspect ratio
	 */
	protected boolean isFixedAspectRatio = false;
	
	
	/**
	 * Used for subclasses with mouse listeners
	 */
	protected volatile boolean mouseIsDown = false;
	
	/**
	 * Used for subclasses with mouse listeners
	 */
	protected volatile boolean isRunning = false;
	
	
	/**
	 * The default bounds of the chart when empty: both axes run -DEFAULT_AUTO_RANGE to +DEFAULT_AUTO_RANGE
	 */
	protected static final double DEFAULT_AUTO_RANGE = 10;
	
	public static final String NEWLINE = System.getProperty("line.separator");
	
	public ExportableChartPanel(JFreeChart chart){
		super(chart, false);
		
//		getChartRenderingInfo().setEntityCollection(null);
		
		JPopupMenu popup = this.getPopupMenu();
		popup.addSeparator();
		
		JMenuItem exportItem = new JMenuItem("Export data...");
		exportItem.addActionListener(this);
		exportItem.setActionCommand("Export");
		exportItem.setEnabled(true);
			
		popup.add(exportItem);
	
		this.setPopupMenu(popup);
		
		// Ensure that the chart text and images are redrawn to 
		// a proper aspect ratio when the panel is resized
		this.addComponentListener(new ComponentAdapter() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	        	restoreComponentRatio();
	        }
	    });
		
		
	}
	
	public void restoreComponentRatio(){
		setMaximumDrawHeight(this.getHeight());
        setMaximumDrawWidth(this.getWidth());
        setMinimumDrawWidth(this.getWidth());
        setMinimumDrawHeight(this.getHeight());
	}
	
	public void setFixedAspectRatio(boolean b){
		isFixedAspectRatio = b;
		
		if(b){
			this.addComponentListener(new FixedAspectAdapter() 	);
			
			restoreAutoBounds();
		} else {
			
			for(ComponentListener l : this.getComponentListeners()){
				if(l instanceof FixedAspectAdapter){
					this.removeComponentListener(l);
				}
			}

		}
	}
	
	public boolean isFixedAspectRatio(){
		return isFixedAspectRatio;
	}
	
	/**
	 * Get the ratio of the width / height of the panel
	 * @return
	 */
	public double getPanelAspectRatio(){
		return (double) this.getWidth() / (double) this.getHeight();
	}
	
	/**
	 * Get the ratio of the width / height of the plot (in chart units)
	 * @return
	 */
	public double getPlotAspectRatio(){
		// Only apply to XYPlots
		if(  !(this.getChart().getPlot() instanceof XYPlot)){
			return 1;
		}

		XYPlot plot = (XYPlot) this.getChart().getPlot();
		
		double w = plot.getDomainAxis().getRange().getLength();
		double h = plot.getRangeAxis().getRange().getLength();
		return w / h;
	}
	
	/* (non-Javadoc)
	 * @see org.jfree.chart.ChartPanel#setChart(org.jfree.chart.JFreeChart)
	 * 
	 * Allows a message to be sent to registered ChartSetEventListeners that a new
	 * chart has been set
	 */
	@Override
	public void setChart(JFreeChart chart){
		super.setChart(chart);
		try{
			fireChartSetEvent();
		} catch(NullPointerException e){
			// This occurs during init because setChart is called internally in ChartPanel
			// Catch and ignore
		}
		
		if(isFixedAspectRatio){
			restoreAutoBounds();
		}
	}
	
	@Override
	public void restoreAutoBounds() {
		
		// Only carry out if the flag is set
		if( ! isFixedAspectRatio){
			super.restoreAutoBounds();
			return;
		}
		
		try {
			
			// Only apply to XYPlots
			if(  !(this.getChart().getPlot() instanceof XYPlot)){
				super.restoreAutoBounds();
				return;
			}
			
			XYPlot plot = (XYPlot) this.getChart().getPlot();
			
			// Only apply to plots with datasets
			if(plot.getDatasetCount()==0){
				return;
			}

			// Find the aspect ratio of the chart
			double chartWidth  = this.getWidth();
			double chartHeight = this.getHeight();
			
			// If we can't get useful values for width and height, use defaults
			if(Double.valueOf(chartWidth)==null || Double.valueOf(chartHeight)==null){
				plot.getRangeAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				plot.getDomainAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				return;
			}
			
			// Calculate the panel aspect ratio
			
			double aspectRatio = chartWidth / chartHeight;
			
			finest("Plot w: "+chartWidth+"; h: "+chartHeight+"; asp: "+aspectRatio);

			// start with impossible values, before finding the real chart values
			double xMin = Double.MAX_VALUE;
			double yMin = Double.MAX_VALUE;
			//		
			double xMax = Double.MIN_VALUE;
			double yMax = Double.MIN_VALUE;
			
			// get the max and min values on the chart by looking for
			// the min and max values within each dataset in the chart
			for(int i = 0; i<plot.getDatasetCount();i++){
				XYDataset dataset = plot.getDataset(i);

				if(dataset==null){ // No dataset, skip
					finest("Null dataset "+i);
					continue;
				}
				
				// No values in the dataset, skip
				if(DatasetUtilities.findMaximumDomainValue(dataset)==null){
					continue;
				}

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
			
			// If no useful datasets were found (e.g. all datasets were malformed)
			// min and max 'impossible' values have not changed. In this case, set defaults
			if(xMin == Double.MAX_VALUE || yMin == Double.MAX_VALUE){
				xMin = -DEFAULT_AUTO_RANGE;
				yMin = -DEFAULT_AUTO_RANGE;
				xMax = DEFAULT_AUTO_RANGE;
				yMax = DEFAULT_AUTO_RANGE;
			}
			

			// find the ranges the min and max values cover
			double xRange = xMax - xMin;
			double yRange = yMax - yMin;

			double newXRange = xRange;
			double newYRange = yRange;

			// test the aspect ratio
			if( (xRange / yRange) > aspectRatio){
				// width is not enough
				newXRange = xRange * 1.1;
				newYRange = newXRange / aspectRatio;
			} else {
				// height is not enough
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
			
			if(yMin>=yMax){
				finer("Min and max are equal");
				xMin = -DEFAULT_AUTO_RANGE;
				yMin = -DEFAULT_AUTO_RANGE;
				xMax = DEFAULT_AUTO_RANGE;
				yMax = DEFAULT_AUTO_RANGE;
			} 
			
			plot.getRangeAxis().setRange(yMin, yMax);
			plot.getDomainAxis().setRange(xMin, xMax);
			
		
			

		} catch (Exception e){
			stack("Error restoring auto bounds, falling back to default", e);
			super.restoreAutoBounds();
		}
	
	}
		
	private String getData(){

		String result = "";

		try{


			if(this.getChart().getPlot() instanceof CategoryPlot){
				
				if( this.getChart().getCategoryPlot().getDataset() instanceof ShellResultDataset ){

					return getShellData();
				}

				if( this.getChart().getCategoryPlot().getDataset() instanceof BoxAndWhiskerCategoryDataset ){

					return getBoxplotData();
				}

			} else {

				if( this.getChart().getXYPlot().getDataset() instanceof DefaultXYDataset ){


					return getXYProfileData(); // single profiles

				}
				
				if( this.getChart().getXYPlot().getDataset() instanceof HistogramDataset ){

					return getHistogramData();
				}
				
				

			}

		} catch (ClassCastException e2){
			
			StringBuilder builder = new StringBuilder();
			builder.append("Class cast error: "+e2.getMessage()+NEWLINE);

			for(StackTraceElement el : e2.getStackTrace()){
				builder.append(el.toString()+NEWLINE);
			}
			return builder.toString();
		}
		return result;
	}
	
	private void export(){
						
		// get a place to save to
		SaveDialog saveDialog = new SaveDialog("Export data to...", "Chart data", Constants.TAB_FILE_EXTENSION);

		String fileName   = saveDialog.getFileName();
		String folderName = saveDialog.getDirectory();
		
		if(fileName!=null && folderName!=null){
			File saveFile = new File(folderName+File.separator+fileName);
			

			String string = getData();
			PrintWriter out;
			try {

				out = new PrintWriter(saveFile);
				out.println(string);
				out.close();
			} catch (FileNotFoundException e) {
				warn("Cannot export to file");
				stack("Error exporting", e);
			}
			
		} 

	}
	
	// Invoke when dealing  with an XY chart
	private String getShellData() throws ClassCastException {
		CategoryPlot plot = this.getChart().getCategoryPlot();
		StringBuilder builder = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#0.00");
		
		int datasetCount = plot.getDatasetCount();
		
		for(int dataset=0; dataset<datasetCount;dataset++){
			ShellResultDataset ds = (ShellResultDataset) plot.getDataset(dataset);
			
			for(int column=0; column<ds.getColumnCount();column++){
				String columnName = ds.getColumnKey(column).toString();
				builder.append("Shell_"+columnName+":"+NEWLINE);
				
				for(int row=0; row<ds.getRowCount(); row++){
					String rowName = ds.getRowKey(row).toString();
					builder.append("\t"+rowName+":"+NEWLINE);
					

					double value = ds.getValue(row, column).doubleValue();
					builder.append("\t\t" + df.format(  value) + NEWLINE);
				}
			}
		}
		
		builder.append(NEWLINE);
		return builder.toString();
	}
	
	// Invoke when dealing  with an XY chart
	private String getXYProfileData() throws ClassCastException {
		
		StringBuilder builder = new StringBuilder();
		
		DecimalFormat df = new DecimalFormat("#0.00");

		XYPlot plot = this.getChart().getXYPlot();
		
		for(int dataset=0; dataset<plot.getDatasetCount();dataset++){

			XYDataset ds;
			try{
				ds = (DefaultXYDataset) plot.getDataset(dataset);
			} catch (ClassCastException e){
				ds = (XYSeriesCollection) plot.getDataset(dataset); // try getting a collection instead
			}
			
			for(int series=0; series<ds.getSeriesCount();series++){

				String seriesName = ds.getSeriesKey(series).toString();
				builder.append(seriesName+":"+NEWLINE);

				for(int i=0; i<ds.getItemCount(series); i++){


					double x= ds.getXValue(series, i);
					double y = ds.getYValue(series, i);

					builder.append("\t"+ df.format(x) +"\t"+ df.format(y) +NEWLINE);
				}
			}
			builder.append(NEWLINE);
		}
		
		
		return builder.toString();
		
	}
		
	private String getBoxplotData()throws ClassCastException {

		CategoryPlot plot = this.getChart().getCategoryPlot();
		StringBuilder builder = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#0.00");
		
		for(int dataset=0; dataset<plot.getDatasetCount();dataset++){

			DefaultBoxAndWhiskerCategoryDataset ds = (DefaultBoxAndWhiskerCategoryDataset) plot.getDataset(dataset);
			

			for(int column=0; column<ds.getColumnCount();column++){
				
				String columnName = ds.getColumnKey(column).toString();
				builder.append(columnName+":"+NEWLINE);
				
				for(int row=0; row<ds.getRowCount(); row++){
					
					String rowName = ds.getRowKey(row).toString();
					builder.append("\t"+rowName+":"+NEWLINE);
					

					double value = ds.getValue(row, column).doubleValue();
					
					builder.append("\tMin   : "+  df.format(  ds.getMinOutlier(row, column)  )+NEWLINE);
					builder.append("\tLower : "+  df.format(  ds.getMinRegularValue(row, column)  )+NEWLINE);
					builder.append("\tQ1    : "+  df.format(  ds.getQ1Value(row, column))+NEWLINE);
					builder.append("\tMedian: "+  df.format(  value)+NEWLINE);
					builder.append("\tQ3    : "+  df.format(  ds.getQ3Value(row, column))+NEWLINE);
					builder.append("\tUpper : "+  df.format(  ds.getMaxRegularValue(row, column))+NEWLINE);
					builder.append("\tMax   : "+  df.format(  ds.getMaxOutlier(row, column)  )+NEWLINE);
					builder.append(NEWLINE);
					
					if(ds instanceof ExportableBoxAndWhiskerCategoryDataset){
						
						List rawData =( (ExportableBoxAndWhiskerCategoryDataset)ds).getRawData(rowName, columnName);
						Collections.sort(rawData);
						for(Object o : rawData){
							builder.append("\t\t"+  o.toString()+NEWLINE);
						}
						builder.append(NEWLINE);
					}
					
//					if(ds instanceof ViolinCategoryDataset){
//						
//						List<Number> pdfValues = ( ( ViolinCategoryDataset) ds).getPdfValues(rowName, columnName);
//						
//						Range range = ( ( ViolinCategoryDataset) ds).getProbabiltyRange();
//						
//						
//						
//					}
					
				}
			}
		}
		return builder.toString();
	}
	
	private String getHistogramData()throws ClassCastException {

		XYPlot plot = this.getChart().getXYPlot();
		DecimalFormat df = new DecimalFormat("#0.00");
		StringBuilder builder = new StringBuilder();
		
		for(int dataset =0; dataset<plot.getDatasetCount();dataset++){

			HistogramDataset ds = (HistogramDataset) plot.getDataset(dataset);
			
			for(int series =0; series<ds.getSeriesCount();series++){

				String seriesName = ds.getSeriesKey(series).toString();
				builder.append(seriesName+":"+NEWLINE);
				
				for(int i=0; i<ds.getItemCount(series); i++){

					double x = ds.getXValue(series, i);
					double y = ds.getYValue(series, i);
					builder.append("\t"+ df.format(x) +"\t"+ df.format(y) +NEWLINE);
				}
				builder.append(NEWLINE);
			}
		}
		return builder.toString();
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		super.actionPerformed(arg0);
		
		// Align two points to the vertical
		if(arg0.getActionCommand().equals("Export")){
			
			export();
		}
	}
	
	 /**
     * Signal listeners that the chart with the given options
     * has been rendered
     * @param options
     */
    public void fireChartSetEvent(){
    	ChartSetEvent e = new ChartSetEvent(this);
    	Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
        	
        	Object o = iterator.next();
        	
        	if(o instanceof ChartSetEventListener){
        		( (ChartSetEventListener) o ).chartSetEventReceived( e );
        	}
        }
    }
    
    
    /**
     * Add a listener for completed charts rendered into the chart cache of this panel.
     * @param l
     */
    public synchronized void addChartSetEventListener( ChartSetEventListener l ) {
    	listeners.add( l );
    }
    
    public synchronized void removeChartSetEventListener( ChartSetEventListener l ) {
    	listeners.remove( l );
    }

	public void addBorderPointEventListener( BorderPointEventListener l) {
		listeners.add( l );
	}
	
	public synchronized void removeBorderPointEventListener( BorderPointEventListener l ) {
    	listeners.remove( l );
    }
	
	public class FixedAspectAdapter extends ComponentAdapter {
		@Override
		public void componentResized(ComponentEvent e) {
			restoreAutoBounds();
		}
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		// TODO Auto-generated method stub
		
	}

}
