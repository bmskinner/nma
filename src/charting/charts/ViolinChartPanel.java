package charting.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.Range;

@SuppressWarnings("serial")
public class ViolinChartPanel extends ExportableChartPanel{
	
	public ViolinChartPanel(JFreeChart chart) {
		super(chart);
	}
	

	@Override
	public void restoreAutoBounds() {
		
		Plot p = this.getChart().getPlot();
		
		if(p instanceof CategoryPlot){
		
		CategoryPlot plot = (CategoryPlot)p;

			if(plot.getDataset(0) instanceof ViolinCategoryDataset){
				if(  ((ViolinCategoryDataset) plot.getDataset(0)).hasProbabilities()){
					Range result = ((ViolinCategoryDataset) plot.getDataset(0)).getProbabiltyRange();
					for(int i=1; i<plot.getDatasetCount(); i++){
						ViolinCategoryDataset ds = (ViolinCategoryDataset) plot.getDataset(i);
						Range r = ds.getProbabiltyRange();
						result = Range.combine(result, r);
					}
					plot.getRangeAxis().setRange(result);
				} else {
					super.restoreAutoBounds();
				}
			} else {
				super.restoreAutoBounds();
			}
		} else {
			super.restoreAutoBounds();
		}
	}

}
