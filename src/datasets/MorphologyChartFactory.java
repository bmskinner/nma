package datasets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.components.Profile;
import no.gui.ColourSelecter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

public class MorphologyChartFactory {
	
	/**
	 * Create an empty chart to display when no datasets are selected
	 * @return a chart
	 */
	public static JFreeChart makeEmptyProfileChart(){
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				"Position", "Angle", null);
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		return chart;
	}
	
	/**
	 * Create a profile chart from a given XYDataset. Set the series 
	 * colours for each component
	 * @param ds the profile dataset
	 * @return a chart
	 */
	public static JFreeChart makeProfileChart(XYDataset ds, int xLength){
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		
		XYPlot plot = chart.getXYPlot();
		
		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,xLength);
		
		// always set the y range to 360 degrees
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		
		// the 180 degree line
		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			
			// segments along the median profile
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			
			// entire nucleus profile
			if(name.startsWith("Nucleus_")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
				plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			} 
			
			// quartile profiles
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 
			
			// simple profiles
			if(name.startsWith("Profile_")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
				plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			} 
			
		}	
		return chart;
	}
	
	/**
	 * Get a series or dataset index for colour selection when drawing charts. The index
	 * is set in the DatasetCreator as part of the label. The format is Name_index_other
	 * @param label the label to extract the index from 
	 * @return the index found
	 */
	public static int getIndexFromLabel(String label){
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
	}
	
	/**
	 * Create a multi dataset profile chart. Shows medians and iqrs for each dataset, and
	 * scales to the given length
	 * @param list the analysis datasets (contain colour information)
	 * @param medianProfiles the medians
	 * @param iqrProfiles the iqrs
	 * @param xLength the length of the x axis
	 * @return a chart
	 */
	public static JFreeChart makeMultiProfileChart(List<AnalysisDataset> list, XYDataset medianProfiles, List<XYSeriesCollection> iqrProfiles, int xLength){
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,xLength);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);

		// add 180 degree horizontal line
		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));

		int lastSeries = 0;

		for(int i=0;i<iqrProfiles.size();i++){
			XYSeriesCollection seriesCollection = iqrProfiles.get(i);

			// add to dataset
			plot.setDataset(i, seriesCollection);


			// find the series index
			String name = (String) seriesCollection.getSeriesKey(0);

			// index should be the position in the AnalysisDatase list
			// see construction in NucleusDatasetCreator
			int index = MorphologyChartFactory.getIndexFromLabel(name); 

			// make a transparent color based on teh profile segmenter system
			Color profileColour = list.get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(i)
					: list.get(index).getDatasetColour();

					Color iqrColour		= ColourSelecter.getTransparentColour(profileColour, true, 128);

					// fill beteween the upper and lower IQR with single colour; do not show shapes
					XYDifferenceRenderer differenceRenderer = new XYDifferenceRenderer(iqrColour, iqrColour, false);

					// go through each series in the collection, and set the line colour
					for(int series=0;series<seriesCollection.getSeriesCount();series++){
						differenceRenderer.setSeriesPaint(series, iqrColour);
						differenceRenderer.setSeriesVisibleInLegend(series, false);

					}
					plot.setRenderer(i, differenceRenderer);

					lastSeries++; // track the count of series
		}

		plot.setDataset(lastSeries, medianProfiles);
		StandardXYItemRenderer medianRenderer = new StandardXYItemRenderer();
		plot.setRenderer(lastSeries, medianRenderer);

		for (int j = 0; j < medianProfiles.getSeriesCount(); j++) {
			medianRenderer.setSeriesVisibleInLegend(j, Boolean.FALSE);
			medianRenderer.setSeriesStroke(j, new BasicStroke(2));
			String name = (String) medianProfiles.getSeriesKey(j);
			int index = MorphologyChartFactory.getIndexFromLabel(name); 
			
			Color profileColour = list.get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(j)
					: list.get(index).getDatasetColour();
					
			medianRenderer.setSeriesPaint(j, profileColour.darker());
		}
		
		
		
		return chart;
	}
	
	/**
	 * Create a variabillity chart showing the IQR for a single dataset. Segment colours
	 * are applied. 
	 * @param list the dataset
	 * @param ds the XYDataset
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	public static JFreeChart makeSingleVariabilityChart(List<AnalysisDataset> list, XYDataset ds, int xLength){
		CellCollection n = list.get(0).getCollection();
		JFreeChart chart = MorphologyChartFactory.makeProfileChart(ds, xLength);
		XYPlot plot = chart.getXYPlot();
//		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setLabel("IQR");
		plot.getRangeAxis().setAutoRange(true);
		List<Integer> maxima = n.getProfileCollection().findMostVariableRegions(n.getOrientationPoint());
		Profile xpoints = n.getProfileCollection().getProfile(n.getOrientationPoint()).getPositions(xLength);
		for(Integer i : maxima){

			plot.addDomainMarker(new ValueMarker(xpoints.get(i), Color.BLACK, new BasicStroke(1.0f)));
		}
		return chart;
	}
	
	/**
	 * Create a variabillity chart showing the IQR for a multiple datasets.
	 * @param list the datasets
	 * @param ds the XYDataset
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	public static JFreeChart makeMultiVariabilityChart(List<AnalysisDataset> list, XYDataset ds, int xLength){
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "IQR", ds, PlotOrientation.VERTICAL, true, true,
				                false);

		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,xLength);
		plot.getRangeAxis().setAutoRange(true);
		plot.setBackgroundPaint(Color.WHITE);

		for (int j = 0; j < ds.getSeriesCount(); j++) {
			plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
			plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
			int index = MorphologyChartFactory.getIndexFromLabel( (String) ds.getSeriesKey(j));
			Color profileColour = list.get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(index)
					: list.get(index).getDatasetColour();
			
			plot.getRenderer().setSeriesPaint(j, profileColour);
		}	
		return chart;
	}
	
	public static ChartPanel makeProfileChartPanel(JFreeChart chart){
		ChartPanel panel = new ChartPanel(chart){
			@Override
			public void restoreAutoBounds() {
				XYPlot plot = (XYPlot) this.getChart().getPlot();
				
				int length = 100;
				for(int i = 0; i<plot.getDatasetCount();i++){
					XYDataset dataset = plot.getDataset(i);
					Number maximum = DatasetUtilities.findMaximumDomainValue(dataset);
					length = maximum.intValue() > length ? maximum.intValue() : length;
				}
				plot.getRangeAxis().setRange(0, 360);
				plot.getDomainAxis().setRange(0, length);				
				return;
			} 
		};
		return panel;
	}
	
	/**
	 * Create an empty boxplot
	 * @return
	 */
	public static JFreeChart makeEmptyBoxplot(){
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	
		boxplot.getPlot().setBackgroundPaint(Color.WHITE);
		return boxplot;
	}
	
	/**
	 * Create and format a boxplot based on a dataset
	 * @param ds the dataset
	 * @return
	 */
	public static JFreeChart makeSegmentBoxplot(BoxAndWhiskerCategoryDataset ds, List<AnalysisDataset> list){
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false);	
		
		if(list.size()>1){
			return makeEmptyBoxplot();
		}
		
		CategoryPlot plot = boxplot.getCategoryPlot();
		plot.setBackgroundPaint(Color.WHITE);
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		plot.setRenderer(renderer);
		renderer.setUseOutlinePaintForWhiskers(true);   
		renderer.setBaseOutlinePaint(Color.BLACK);
		renderer.setBaseFillPaint(Color.LIGHT_GRAY);
		
		if(list!=null && !list.isEmpty()){
						
			for(int i=0;i<plot.getDataset().getRowCount();i++){
				
				AnalysisDataset d = list.get(0);

				Color color = d.getDatasetColour() == null 
							? ColourSelecter.getSegmentColor(i)
							: d.getDatasetColour();
							
							renderer.setSeriesPaint(i, color);
			}
			renderer.setMeanVisible(false);
			renderer.setItemMargin(0.02);
			renderer.setMaximumBarWidth(0.08);
		}
		
		return boxplot;
	}
	
	

}
