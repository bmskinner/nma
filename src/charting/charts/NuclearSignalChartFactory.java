package charting.charts;

import gui.components.ColourSelecter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

import analysis.AnalysisDataset;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;

public class NuclearSignalChartFactory  extends AbstractChartFactory {
	
	private static NuclearSignalChartFactory instance = null;
	
	private NuclearSignalChartFactory(){}
	
	public static NuclearSignalChartFactory getInstance(){
		if(instance==null){
			instance = new NuclearSignalChartFactory();
		}
		return instance;
	}
	
	public JFreeChart makeEmptyChart(){
		return ConsensusNucleusChartFactory.getInstance().makeEmptyChart();
	}
	
	private JFreeChart createEmptyShellChart(){
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		return shellsChart;
	}
	
	public JFreeChart createShellChart(ChartOptions options){
		
		if( ! options.hasDatasets()){
			return createEmptyShellChart();
		}
		
		List<CategoryDataset> list = NuclearSignalDatasetCreator.getInstance().createShellBarChartDataset(options);
		
		JFreeChart chart = ChartFactory.createBarChart(null, "Outer <--- Shell ---> Interior", "Percent", list.get(0));
		chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		chart.getCategoryPlot().getRangeAxis().setRange(0,100);
		
		int datasetCount = 0;
		for(CategoryDataset ds : list){
			
			chart.getCategoryPlot().setDataset(datasetCount, ds);
			
			AnalysisDataset d = options.getDatasets().get(datasetCount);
			
			StatisticalBarRenderer rend = new StatisticalBarRenderer();
			rend.setBarPainter(new StandardBarPainter());
			rend.setShadowVisible(false);
			rend.setErrorIndicatorPaint(Color.black);
			rend.setErrorIndicatorStroke(new BasicStroke(2));
			chart.getCategoryPlot().setRenderer(datasetCount, rend);
			
			for (int j = 0; j < ds.getRowCount(); j++) {
				
				rend.setSeriesVisibleInLegend(j, false);
				rend.setSeriesStroke(j, new BasicStroke(2));
				UUID signalGroup = getSignalGroupFromLabel(  ds.getRowKey(j).toString()    );
	            
	            Color colour = d.getCollection().getSignalGroup(signalGroup).hasColour()
	                         ? d.getCollection().getSignalGroup(signalGroup).getGroupColour()
	                         : ColourSelecter.getColor(j);
	            

				rend.setSeriesPaint(j, colour);
			}	
			
			
			datasetCount++;
		}
		
		
		
		
		
		

		

		return chart;
	}
	
	/**
	 * Create a nucleus outline chart with nuclear signals drawn as transparent
	 * circles
	 * @param dataset the AnalysisDataset to use to draw the consensus nucleus
	 * @param signalCoMs the dataset with the signal centre of masses
	 * @return
	 * @throws Exception 
	 */
	public JFreeChart makeSignalCoMNucleusOutlineChart(ChartOptions options) throws Exception{
		
		if( ! options.hasDatasets()){
			finer("No datasets for signal outline chart");
			return makeEmptyChart();
		}
		
		// Do not allow multi datasets here
		if( options.isMultipleDatasets()){
			finer("Multiple datasets for signal outline chart");
			return makeEmptyChart();
		}
		
		// Check for consensus nucleus
		if( ! options.firstDataset().getCollection().hasConsensusNucleus()){
			finer("No consensus for signal outline chart");
			return makeEmptyChart();
		}
		
		
		XYDataset signalCoMs = NuclearSignalDatasetCreator.getInstance().createSignalCoMDataset(options.firstDataset());
		
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(options.firstDataset());

		XYPlot plot = chart.getXYPlot();
		
		if(signalCoMs.getSeriesCount()>0){
			plot.setDataset(1, signalCoMs);

			XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
			for(int series=0;series<signalCoMs.getSeriesCount();series++){

				Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
				rend.setSeriesShape(series, circle);

				String name = (String) signalCoMs.getSeriesKey(series);
//				int seriesGroup = getIndexFromLabel(name);
				UUID seriesGroup = getSignalGroupFromLabel(name);
                Color colour = options.firstDataset().getCollection().getSignalGroup(seriesGroup).hasColour()
                        ? options.firstDataset().getCollection().getSignalGroup(seriesGroup).getGroupColour()
                        : ColourSelecter.getColor(series);

				rend.setSeriesPaint(series, colour);
				rend.setBaseLinesVisible(false);
				rend.setBaseShapesVisible(true);
				rend.setBaseSeriesVisibleInLegend(false);
			}
			plot.setRenderer(1, rend);

			int j=0;
			for(UUID signalGroup : options.firstDataset().getCollection().getSignalManager().getSignalGroupIDs()){
				List<Shape> shapes = NuclearSignalDatasetCreator.getInstance().createSignalRadiusDataset(options.firstDataset(), signalGroup);

				int signalCount = shapes.size();

				int alpha = (int) Math.floor( 255 / ((double) signalCount) )+20;
				alpha = alpha < 10 ? 10 : alpha > 156 ? 156 : alpha;

                Color colour = options.firstDataset().getCollection().getSignalGroup(signalGroup).hasColour()
                        ? options.firstDataset().getCollection().getSignalGroup(signalGroup).getGroupColour()
                        : ColourSelecter.getColor(j++);


				for(Shape s : shapes){
					XYShapeAnnotation an = new XYShapeAnnotation( s, null,
							null, ColourSelecter.getTransparentColour(colour, true, alpha)); // layer transparent signals
					plot.addAnnotation(an);
				}
			}
		}
		return chart;
	}

}
