package gui.components;

import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import charting.datasets.OutlierFreeBoxAndWhiskerCategoryDataset;
import ij.IJ;


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
	
	public void getData(){
		try{
			getXYData();
		} catch (ClassCastException e){
			getBoxplotData();
		}
	}
	
	// Invoke when dealing  with an XY chart
	public void getXYData() throws ClassCastException {
		
		IJ.log("Getting XY data");
		XYPlot plot = this.getChart().getXYPlot();
		
		for(int dataset =0; dataset<plot.getDatasetCount();dataset++){

			DefaultXYDataset ds = (DefaultXYDataset) plot.getDataset(dataset);
			
			for(int series =0; series<ds.getSeriesCount();series++){

				String seriesName = ds.getSeriesKey(series).toString();
				IJ.log("Series "+seriesName);
				for(int i=0; i<ds.getItemCount(series); i++){


					double x = ds.getXValue(series, i);
					double y = ds.getYValue(series, i);
					IJ.log(x+" - "+y);
				}
			}
		}
		
		
	}
	
	public void getBoxplotData()throws ClassCastException {
		IJ.log("Getting boxplot data");
		CategoryPlot plot = this.getChart().getCategoryPlot();
		
		for(int dataset =0; dataset<plot.getDatasetCount();dataset++){

			OutlierFreeBoxAndWhiskerCategoryDataset ds = (OutlierFreeBoxAndWhiskerCategoryDataset) plot.getDataset(dataset);
			
			for(int column =0; column<ds.getColumnCount();column++){
				
				String columnName = ds.getColumnKey(column).toString();

				IJ.log("Series "+columnName);
				for(int row=0; row<ds.getRowCount(); row++){

					double value = ds.getValue(row, column).doubleValue();

					IJ.log("Median: "+value);
					IJ.log("Q1: "+ds.getQ1Value(row, column));
					IJ.log("Q3: "+ds.getQ3Value(row, column));
					IJ.log("Max: "+ds.getMaxRegularValue(row, column));
					IJ.log("Min: "+ds.getMinRegularValue(row, column));
				}
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		super.actionPerformed(arg0);
		
		// Align two points to the vertical
		if(arg0.getActionCommand().equals("Export")){
			
			getData();
		}

		
		
		
	}

}
