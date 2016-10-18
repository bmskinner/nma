package charting.charts.panels;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.Range;

import charting.datasets.ViolinCategoryDataset;

@SuppressWarnings("serial")
public class ViolinChartPanel extends ExportableChartPanel{
	
	public ViolinChartPanel(JFreeChart chart) {
		super(chart);
	}
	

	@Override
	public void restoreAutoBounds() {
		
		Plot p = this.getChart().getPlot();
		
		if( !(p instanceof CategoryPlot)){
			finer("Not a category plot in volin panel");
			super.restoreAutoBounds();
			return;
		}
			
		CategoryPlot plot = (CategoryPlot)p;

		if(  ! (plot.getDataset(0) instanceof ViolinCategoryDataset)){
			finer("Not a violin dataset in volin panel");
			super.restoreAutoBounds();
			return;
		}
		
		
		ViolinCategoryDataset dataset = (ViolinCategoryDataset) plot.getDataset(0);
		
		
		if( ! dataset.hasProbabilities()){
			finer("No probabilities in dataset in volin panel");
			super.restoreAutoBounds();
			return;
		}
			
		Range result = dataset.getProbabiltyRange();
		
		
		for(int i=1; i<plot.getDatasetCount(); i++){
			ViolinCategoryDataset ds = (ViolinCategoryDataset) plot.getDataset(i);
			Range r = ds.getProbabiltyRange();
			result = Range.combine(result, r);
		}
		finest("Probability range: "+result.toString());
		
		plot.getRangeAxis().setRange(result);
		

	}


}
