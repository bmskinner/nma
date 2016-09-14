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

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import charting.charts.CoupledProfileOutlineChartPanel.BorderPointEventListener;
import utility.Constants;
import gui.ChartSetEvent;
import gui.ChartSetEventListener;
import ij.io.SaveDialog;
import logging.Loggable;


/**
 * This panel should add a right click menu item for 'Export'
 * This will extract the chart data, and save it to a desired location.
 * It also redraws the chart as the panel is resized for better UX
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ExportableChartPanel extends ChartPanel implements Loggable {
		
	private final List<Object> listeners = new ArrayList<Object>();
	
	public ExportableChartPanel(JFreeChart chart){
		super(chart);
		
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
	            setMaximumDrawHeight(e.getComponent().getHeight());
	            setMaximumDrawWidth(e.getComponent().getWidth());
	            setMinimumDrawWidth(e.getComponent().getWidth());
	            setMinimumDrawHeight(e.getComponent().getHeight());
	        }
	    });
		
		
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
	}
		
	private String getData(){

		String result = "";

		try{


			if(this.getChart().getPlot() instanceof CategoryPlot){

				if( this.getChart().getCategoryPlot().getDataset() instanceof BoxAndWhiskerCategoryDataset ){

					result = getBoxplotData();
				}

			} else {

				if( this.getChart().getXYPlot().getDataset() instanceof DefaultXYDataset ){


					result = getXYProfileData(); // single profiles

				}
				
				if( this.getChart().getXYPlot().getDataset() instanceof HistogramDataset ){

					result = getHistogramData();
				}

			}






		} catch (ClassCastException e2){
			
			StringBuilder builder = new StringBuilder();
			builder.append("Class cast error: "+e2.getMessage()+System.getProperty("line.separator"));

			for(StackTraceElement el : e2.getStackTrace()){
				builder.append(el.toString()+System.getProperty("line.separator"));
			}
			result = builder.toString();
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
				
			}
			
		} 

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
				builder.append(seriesName+":"+System.getProperty("line.separator"));

				for(int i=0; i<ds.getItemCount(series); i++){


					double x= ds.getXValue(series, i);
					double y = ds.getYValue(series, i);

					builder.append("\t"+ df.format(x) +"\t"+ df.format(y) +System.getProperty("line.separator"));
				}
			}
			builder.append(System.getProperty("line.separator"));
		}
		
		
		return builder.toString();
		
	}
	
	// Invoke when dealing  with an XY chart
//	private String getXYCollectionData() throws ClassCastException {
//		
//		StringBuilder builder = new StringBuilder();
//		
//		DecimalFormat df = new DecimalFormat("#0.00");
//
//		XYPlot plot = this.getChart().getXYPlot();
//		
//		for(int dataset=0; dataset<plot.getDatasetCount();dataset++){
//
//			XYSeriesCollection ds = (XYSeriesCollection) plot.getDataset(dataset);
//			
//			for(int series=0; series<ds.getSeriesCount();series++){
//
//				String seriesName = ds.getSeriesKey(series).toString();
//				builder.append(seriesName+":"+System.getProperty("line.separator"));
//
//				for(int i=0; i<ds.getItemCount(series); i++){
//
//
//					double x= ds.getXValue(series, i);
//					double y = ds.getYValue(series, i);
//
//					builder.append("\t"+ df.format(x) +"\t"+ df.format(y) +System.getProperty("line.separator"));
//				}
//			}
//			builder.append(System.getProperty("line.separator"));
//		}
//		
//		
//		return builder.toString();
//		
//	}
	
	private String getBoxplotData()throws ClassCastException {

		CategoryPlot plot = this.getChart().getCategoryPlot();
		StringBuilder builder = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#0.00");
		
		for(int dataset=0; dataset<plot.getDatasetCount();dataset++){

			DefaultBoxAndWhiskerCategoryDataset ds = (DefaultBoxAndWhiskerCategoryDataset) plot.getDataset(dataset);

			for(int column=0; column<ds.getColumnCount();column++){
				
				String columnName = ds.getColumnKey(column).toString();
				builder.append(columnName+":"+System.getProperty("line.separator"));
				
				for(int row=0; row<ds.getRowCount(); row++){
					
					String rowName = ds.getRowKey(row).toString();
					builder.append("\t"+rowName+":"+System.getProperty("line.separator"));
					

					double value = ds.getValue(row, column).doubleValue();
					
					builder.append("\tLower : "+  df.format(  ds.getMinRegularValue(row, column)  )+System.getProperty("line.separator"));
					builder.append("\tQ1    : "+  df.format(   ds.getQ1Value(row, column))+System.getProperty("line.separator"));
					builder.append("\tMedian: "+  df.format(   value)+System.getProperty("line.separator"));
					builder.append("\tQ3    : "+  df.format(   ds.getQ3Value(row, column))+System.getProperty("line.separator"));
					builder.append("\tUpper : "+  df.format(   ds.getMaxRegularValue(row, column))+System.getProperty("line.separator"));
					builder.append(System.getProperty("line.separator"));
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
				builder.append(seriesName+":"+System.getProperty("line.separator"));
				
				for(int i=0; i<ds.getItemCount(series); i++){

					double x = ds.getXValue(series, i);
					double y = ds.getYValue(series, i);
					builder.append("\t"+ df.format(x) +"\t"+ df.format(y) +System.getProperty("line.separator"));
				}
				builder.append(System.getProperty("line.separator"));
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
            ( (ChartSetEventListener) iterator.next() ).chartSetEventReceived( e );
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

}
