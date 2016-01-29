package gui.components;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import utility.Constants;
import charting.datasets.OutlierFreeBoxAndWhiskerCategoryDataset;
import ij.IJ;
import ij.io.SaveDialog;


/**
 * This panel should add a right click menu item for 'Export'
 * This will extract the chart data, and save it to a desired location
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ExportableChartPanel extends ChartPanel {
	
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
	}
	
	private String getData(){

		String result = "";

		try{
			try{

				result = getXYProfileData(); // single profiles

			} catch (ClassCastException e){

				try{

					result = getBoxplotData();

				} catch (ClassCastException e1){

					result = getHistogramData();
				}
			}


		} catch (ClassCastException e2){
			IJ.log("Class cast error: "+e2.getMessage());
			for(StackTraceElement el : e2.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		IJ.log(result);
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
				ds = (XYSeriesCollection) plot.getDataset(dataset); // try getting a collection
			}
//			DefaultXYDataset ds = (DefaultXYDataset) plot.getDataset(dataset);
			
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

			OutlierFreeBoxAndWhiskerCategoryDataset ds = (OutlierFreeBoxAndWhiskerCategoryDataset) plot.getDataset(dataset);

			for(int column=0; column<ds.getColumnCount();column++){
				
				String columnName = ds.getColumnKey(column).toString();
				builder.append(columnName+":"+System.getProperty("line.separator"));
				
				for(int row=0; row<ds.getRowCount(); row++){
					
					String rowName = ds.getRowKey(row).toString();
					builder.append("\t"+rowName+":"+System.getProperty("line.separator"));
					

					double value = ds.getValue(row, column).doubleValue();
					
					builder.append("\tLower : "+ds.getMinRegularValue(row, column)+System.getProperty("line.separator"));
					builder.append("\tQ1    : "+ds.getQ1Value(row, column)+System.getProperty("line.separator"));
					builder.append("\tMedian: "+value+System.getProperty("line.separator"));
					builder.append("\tQ3    : "+ds.getQ3Value(row, column)+System.getProperty("line.separator"));
					builder.append("\tUpper : "+ds.getMaxRegularValue(row, column)+System.getProperty("line.separator"));
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

}
