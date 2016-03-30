/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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

import gui.components.ColourSelecter;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import stats.DipTester;
import stats.StatisticDimension;
import utility.Constants;
import analysis.AnalysisDataset;
import charting.ChartComponents;
import charting.datasets.CellDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.options.ChartOptions;
import components.AbstractCellularComponent;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class MorphologyChartFactory extends AbstractChartFactory {
	
	
	/**
	 * Create an empty chart to display when no datasets are selected
	 * @return a chart
	 */
	public static JFreeChart makeEmptyProfileChart(ProfileType type){
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				"Position", type.getLabel(), null);
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		
		if(type.getDimension().equals(StatisticDimension.ANGLE)){
			plot.getRangeAxis().setRange(0,360);
		}
		plot.setBackgroundPaint(Color.WHITE);
		return chart;
	}
	
	
	/**
	 * Create a profile chart for the given options
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart createProfileChart(ChartOptions options) throws Exception {
		
		if( ! options.hasDatasets()){
			return makeEmptyProfileChart(options.getType());
		}
		
		if(options.isSingleDataset()){
			return makeSingleProfileChart(options);
		}
		
		if(options.getDatasets().size()>1){
			return makeMultiProfileChart(options);
		}
		return makeEmptyProfileChart(options.getType());
	}
	
	/**
	 * Make a profile chart for a single nucleus
	 * @param n
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart makeIndividualNucleusProfileChart(Nucleus n, ChartOptions options) throws Exception{
		
		XYDataset  ds 	 = NucleusDatasetCreator.createSegmentedProfileDataset(n, options.getType());
		JFreeChart chart = makeProfileChart(ds, n.getBorderLength(), options.getSwatch(), options.getType());
		return chart;
	}
		
	/**
	 * Create a segmented profile chart from a given XYDataset. Set the series 
	 * colours for each component. Draw lines on the offset indexes
	 * @param dataset the dataset the values come from
	 * @param normalised should the scales be normalised
	 * @param rightAligm should the chart be aligned to the right
	 * @return a chart
	 */
	private static JFreeChart makeSingleProfileChart(ChartOptions options) throws Exception {
		
		XYDataset ds = null;
		AnalysisDataset dataset = options.firstDataset();
		CellCollection collection = dataset.getCollection();
		JFreeChart chart = null;
				
		if(options.hasDatasets()){

			if(options.getType().equals(ProfileType.FRANKEN)){
				ds = NucleusDatasetCreator.createFrankenSegmentDataset(options);

			} else {
				ds = NucleusDatasetCreator.createSegmentedProfileDataset(options);
			}
			

			int length = 100 ; // default if normalised


			// if we set raw values, get the maximum nucleus length
			if(!options.isNormalised()){
				length = (int) collection.getMaxProfileLength();
			}

			ColourSwatch swatch = dataset.getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : dataset.getSwatch();
			chart = makeProfileChart(ds, length, swatch, options.getType());

			// mark the reference and orientation points

			XYPlot plot = chart.getXYPlot();

			for (BorderTag tag : collection.getProfileCollection(options.getType()).getOffsetKeys()){

				// get the index of the tag
				int index = collection.getProfileCollection(options.getType()).getOffset(tag);

				// get the offset from to the current draw point
				int offset = collection.getProfileCollection(options.getType()).getOffset(options.getTag());

				// adjust the index to the offset
				index = AbstractCellularComponent.wrapIndex( index - offset, collection.getProfileCollection(options.getType()).getAggregate().length());

				double indexToDraw = index; // convert to a double to allow normalised positioning

				if(options.isNormalised()){ // set to the proportion of the point along the profile
					indexToDraw =  (( indexToDraw / collection.getProfileCollection(options.getType()).getAggregate().length() ) * 100);
				}
				if(options.getAlignment().equals(ProfileAlignment.RIGHT) && !options.isNormalised()){
					int maxX = DatasetUtilities.findMaximumDomainValue(ds).intValue();
					int amountToAdd = maxX - collection.getProfileCollection(options.getType()).getAggregate().length();
					indexToDraw += amountToAdd;

				}

				if(options.isShowMarkers()){
					
					addMarkerToXYPlot(plot, tag, indexToDraw);
					
				}

			}
		} else {
			chart = makeProfileChart(null, 100, ColourSwatch.REGULAR_SWATCH, options.getType());
		}
		return chart;
	}
	
	
	/**
	 * Create a profile chart with the median line only, segmented. Can have multiple datasets shown.
	 * If a single dataset is shown, the chart is real length, otherwise normalised
	 * @param options
	 * @throws Exception
	 */	
	public static JFreeChart makeMultiSegmentedProfileChart(ChartOptions options) throws Exception {
		
		
		int length = 100;
				
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
				                false);
//		JFreeChart chart = makeProfileChart(null, length, list.get(0).getSwatch());
		
		XYPlot plot = chart.getXYPlot();
		
		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,length);

		// always set the y range to 360 degrees
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);

		// the 180 degree line
		plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
		
		int datasetIndex = 0;
		for(AnalysisDataset dataset : options.getDatasets()){

			XYDataset ds = NucleusDatasetCreator.createSegmentedMedianProfileDataset(dataset, options.isNormalised(), options.getAlignment(), options.getTag());

			plot.setDataset(datasetIndex, ds);
			
			DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
			renderer.setBaseShapesVisible(false);
			plot.setRenderer(datasetIndex, renderer);

			int seriesCount = plot.getDataset(datasetIndex).getSeriesCount();

			for (int i = 0; i < seriesCount; i++) {
				
				renderer.setSeriesVisibleInLegend(i, false);
				
				String name = (String) ds.getSeriesKey(i);
//				IJ.log("    Series "+i + ": "+name);
				
				// segments along the median profile
				if(name.startsWith("Seg_")){
					int colourIndex = getIndexFromLabel(name);
					renderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
					ColourSwatch swatch = dataset.getSwatch() == null ? options.getSwatch() : dataset.getSwatch();
					renderer.setSeriesPaint(i, swatch.color(colourIndex));
				} 
			}	

			datasetIndex++;
		}
		
		// Add segment name annotations
		if(options.isSingleDataset()){
			
			for(NucleusBorderSegment seg :  options.firstDataset().getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(options.getTag())
					.getOrderedSegments()){

				int midPoint = seg.getMidpointIndex();

				double x = ((double) midPoint / (double) seg.getTotalLength() ) * 100;
				XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), x, 320);
				segmentAnnotation.setPaint(options.firstDataset().getSwatch().color(seg.getPosition()));
				plot.addAnnotation(segmentAnnotation);
			}
		}
		return chart;
	}
	
			
	/**
	 * Create a profile chart from a given XYDataset. Set the series 
	 * colours for each component
	 * @param ds the profile dataset
	 * @return a chart
	 */
	private static JFreeChart makeProfileChart(XYDataset ds, int xLength, ColourSwatch swatch, ProfileType type) throws Exception{


		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Position", type.getLabel(), ds, PlotOrientation.VERTICAL, true, true,
						false);



		XYPlot plot = chart.getXYPlot();

		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,xLength);

		// always set the y range to 360 degrees
		if(type.equals(ProfileType.REGULAR) || type.equals(ProfileType.FRANKEN)){
			plot.getRangeAxis().setRange(0,360);
			// the 180 degree line
			plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
		}
		plot.setBackgroundPaint(Color.WHITE);
		StandardXYToolTipGenerator tooltip = new StandardXYToolTipGenerator();
		plot.getRenderer().setBaseToolTipGenerator(tooltip);

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
			String name = (String) ds.getSeriesKey(i);

			// segments along the median profile
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, ChartComponents.SEGMENT_STROKE);
				plot.getRenderer().setSeriesPaint(i, swatch.color(colourIndex));

			} 

			// entire nucleus profile
			if(name.startsWith("Nucleus_")){
				plot.getRenderer().setSeriesStroke(i, ChartComponents.PROFILE_STROKE);
				plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			} 

			// quartile profiles
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, ChartComponents.QUARTILE_STROKE);
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 

			// simple profiles
			if(name.startsWith("Profile_")){
				plot.getRenderer().setSeriesStroke(i, ChartComponents.PROFILE_STROKE);
				plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			} 

		}	

		return chart;
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
	private static JFreeChart makeMultiProfileChart(ChartOptions options)  throws Exception{
				
		List<XYSeriesCollection> iqrProfiles = null;
		XYDataset medianProfiles			 = null;
				
		if(options.getType().equals(ProfileType.FRANKEN)){
			iqrProfiles     = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset(  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());				
			medianProfiles	= NucleusDatasetCreator.createMultiProfileFrankenDataset(	  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());
		} else {
			iqrProfiles     = NucleusDatasetCreator.createMultiProfileIQRDataset( options );				
			medianProfiles	= NucleusDatasetCreator.createMultiProfileDataset(	  options );
		
		}
		
		
		// find the maximum profile length - used when rendering raw profiles
		int length = 100;

		if(!options.isNormalised()){
			for(AnalysisDataset d : options.getDatasets()){
				length = (int) Math.max( d.getCollection().getMedianArrayLength(), length);
			}
		}
		
		
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,length);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);

		// add 180 degree horizontal line
		plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);

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
			Color profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(i)
					: options.getDatasets().get(index).getDatasetColour();

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
			
			Color profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(j)
					: options.getDatasets().get(index).getDatasetColour();
					
			medianRenderer.setSeriesPaint(j, profileColour.darker());
		}
		
		
		
		return chart;
	}
	
	public static JFreeChart makeVariabilityChart(ChartOptions options) throws Exception {
		
		if( ! options.hasDatasets()){
			return makeEmptyProfileChart(options.getType());
		}
		
		if(options.isSingleDataset()){
			return makeSingleVariabilityChart(options);
		}
		
		if(options.isMultipleDatasets()){
			return makeMultiVariabilityChart(options);
		}
		
		return makeEmptyProfileChart(options.getType());
	}
	
	/**
	 * Create a variabillity chart showing the IQR for a single dataset. Segment colours
	 * are applied. 
	 * @param list the dataset
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	private static JFreeChart makeSingleVariabilityChart(ChartOptions options) throws Exception {
		XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(options);

		ColourSwatch swatch = options.firstDataset().getSwatch() == null 
				? ColourSwatch.REGULAR_SWATCH 
						: options.firstDataset().getSwatch();
		
		JFreeChart chart = MorphologyChartFactory.makeProfileChart(ds, 100, swatch, options.getType());
		XYPlot plot = chart.getXYPlot();
		plot.getRangeAxis().setLabel("IQR");
		plot.getRangeAxis().setAutoRange(true);
		return chart;
	}
	
	/**
	 * Create a variabillity chart showing the IQR for a multiple datasets.
	 * @param list the datasets
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	private static JFreeChart makeMultiVariabilityChart(ChartOptions options) throws Exception {
		XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(options);
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "IQR", ds, PlotOrientation.VERTICAL, true, true,
				                false);

		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setAutoRange(true);
		plot.setBackgroundPaint(Color.WHITE);

		for (int j = 0; j < ds.getSeriesCount(); j++) {
			plot.getRenderer().setSeriesVisibleInLegend(j, false);
			plot.getRenderer().setSeriesStroke(j, ChartComponents.QUARTILE_STROKE);
			int index = MorphologyChartFactory.getIndexFromLabel( (String) ds.getSeriesKey(j));
			Color profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(index)
					: options.getDatasets().get(index).getDatasetColour();
			
			plot.getRenderer().setSeriesPaint(j, profileColour);
		}	
		return chart;
	}
		
	/**
	 * Create a segment start XY position chart for multiple analysis datasets
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private static JFreeChart makeMultiSegmentStartPositionChart(ChartOptions options) throws Exception {
		
		XYDataset positionDataset = CellDatasetCreator.createPositionFeatureDataset(options);
		
		XYDataset nuclearOutlines = NucleusDatasetCreator.createMultiNucleusOutline(options.getDatasets());
		
		if(positionDataset == null || nuclearOutlines == null){
			// a null dataset is returned if segment counts do not match
			return ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
		}
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                null, null, null, PlotOrientation.VERTICAL, true, true,
				                false);
		
		

		XYPlot plot = chart.getXYPlot();

		plot.setDataset(0, positionDataset);
		
		/*
		 * Points only for the segment positions
		 */
		StandardXYToolTipGenerator tooltip = new StandardXYToolTipGenerator();
		XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();
		pointRenderer.setBaseShapesVisible(true);
		pointRenderer.setBaseLinesVisible(false);
		pointRenderer.setBaseStroke(ChartComponents.QUARTILE_STROKE);
		pointRenderer.setBaseSeriesVisibleInLegend(false);
		pointRenderer.setBaseToolTipGenerator(tooltip);
		plot.setRenderer(0, pointRenderer);
		
		boolean hasConsensus = ConsensusNucleusChartFactory.hasConsensusNucleus(options.getDatasets());
		if(hasConsensus){
			// Find the bounds of the consensus nuclei in the options
			double max = ConsensusNucleusChartFactory.getconsensusChartRange(options.getDatasets());
			plot.setDataset(1, nuclearOutlines);
					
			plot.getDomainAxis().setRange(-max,max);
			plot.getRangeAxis().setRange(-max,max);
			
			
			/*
			 * Lines only for the consensus nuclei
			 */
			XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
			lineRenderer.setBaseShapesVisible(false);
			lineRenderer.setBaseLinesVisible(true);
			lineRenderer.setBaseStroke(ChartComponents.QUARTILE_STROKE);
			lineRenderer.setBaseSeriesVisibleInLegend(false);
			plot.setRenderer(1, lineRenderer);
			
		} else {
			plot.getDomainAxis().setAutoRange(true);
			plot.getRangeAxis().setAutoRange(true);
		}
		plot.setBackgroundPaint(Color.WHITE);

		for (int j = 0; j < positionDataset.getSeriesCount(); j++) {

			Color profileColour = options.getDatasets().get(j).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(j)
					: options.getDatasets().get(j).getDatasetColour();
			
			pointRenderer.setSeriesPaint(j, profileColour);
			
			if(hasConsensus){
				plot.getRenderer(1).setSeriesPaint(j, profileColour);
			}
		}
		return chart;
		
	}
	
	
	/**
	 * Create a chart showing the distribution of xy points for a segment start
	 * @param options the chart options. Should have a segID
	 * @return a chart
	 */
	public static JFreeChart makeSegmentStartPositionChart(ChartOptions options) throws Exception {
		
		if(  options.hasDatasets()){
			return  makeMultiSegmentStartPositionChart(options);
		}
		return ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
	}
	
		
	/**
	 * Create a chart showing the angle values at the given normalised profile position within the
	 * AnalysisDataset. The chart holds two chart datasets: 0 is the probabililty density function.
	 * 1 is the actual values as dots on the x-axis
	 * @param position
	 * @param dataset
	 * @return
	 * @throws Exception
	 */
	private static JFreeChart createModalityChart(Double position, List<AnalysisDataset> list, ProfileType type) throws Exception {
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						type.getLabel(), "Probability", null, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		
		if(type.getDimension().equals(StatisticDimension.ANGLE)){
			plot.getDomainAxis().setRange(0, 360);
		}
		

		plot.addDomainMarker(new ValueMarker(180, Color.BLACK, ChartComponents.MARKER_STROKE));
		
		int datasetCount = 0;
		int iteration = 0;
		for(AnalysisDataset dataset : list){
			
			XYDataset ds 	 = NucleusDatasetCreator.createModalityProbabililtyDataset(position, dataset, type);
			XYDataset values = NucleusDatasetCreator.createModalityValuesDataset(position, dataset, type);
			
			Color colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(iteration)
					: dataset.getDatasetColour();

			plot.setDataset(datasetCount, ds);
			
			XYLineAndShapeRenderer lineRenderer =  new XYLineAndShapeRenderer(true, false);
			plot.setRenderer(datasetCount,lineRenderer);
			int seriesCount = plot.getDataset(datasetCount).getSeriesCount();
			for(int i=0; i<seriesCount;i++){
				
				lineRenderer.setSeriesPaint(i, colour);
				lineRenderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
				lineRenderer.setSeriesVisibleInLegend(i, false);
			}
			
			datasetCount++;
			
			plot.setDataset(datasetCount, values);

			// draw the individual points
			XYLineAndShapeRenderer shapeRenderer =  new XYLineAndShapeRenderer(false, true);
			
			plot.setRenderer(datasetCount,shapeRenderer);
			seriesCount = plot.getDataset(datasetCount).getSeriesCount();
			for(int i=0; i<seriesCount;i++){
				shapeRenderer.setSeriesPaint(i, colour);
				shapeRenderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
				shapeRenderer.setSeriesVisibleInLegend(i, false);
			}
			datasetCount++;
			iteration++;
			
		}

		return chart;
	}
	
	public static JFreeChart createModalityProfileChart(ChartOptions options) throws Exception {
		
		XYDataset ds = NucleusDatasetCreator.createModalityProfileDataset(options);
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Position", "Probability", ds, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 100);
		plot.getRangeAxis().setRange(0, 1);
		
		for(int i=0; i<options.getDatasets().size(); i++){
			
			AnalysisDataset dataset = options.getDatasets().get(i);
			
			Color colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(i)
							: dataset.getDatasetColour();

			plot.getRenderer().setSeriesPaint(i, colour);
			plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
		}
		return chart;
	}
		
	public static JFreeChart createModalityPositionChart(ChartOptions options) throws Exception {

		JFreeChart chart = MorphologyChartFactory.createModalityChart(options.getModalityPosition(), options.getDatasets(), options.getType());
		XYPlot plot = chart.getXYPlot();

		double yMax = 0;
		DecimalFormat df = new DecimalFormat("#0.000");

		for(int i = 0; i<plot.getDatasetCount(); i++){

			// Ensure annotation is placed in the right y position
			double y = DatasetUtilities.findMaximumRangeValue(plot.getDataset(i)).doubleValue();
			yMax = y > yMax ? y : yMax;

		}

		int index = 0;
		for(AnalysisDataset dataset : options.getDatasets()){

			// Do the stats testing
			double pvalue = DipTester.getPValueForPositon(dataset.getCollection(),
					options.getModalityPosition(), 
					options.getType()); 
			
			// Add the annotation
			double yPos = yMax - ( index * (yMax / 20));
			String statisticalTesting = "p(unimodal) = "+df.format(pvalue);
			if(pvalue<Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
				statisticalTesting = "* " + statisticalTesting;
			}
			XYTextAnnotation annotation = new XYTextAnnotation(statisticalTesting,355, yPos);

			// Set the text colour
			Color colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(index)
							: dataset.getDatasetColour();
					annotation.setPaint(colour);
					annotation.setTextAnchor(TextAnchor.TOP_RIGHT);
					plot.addAnnotation(annotation);
					index++;
		}
		return chart;
	}
	
	
	/**
	 * Create a QQ plot for the given datasets, at the given positon along a profile
	 * @param position
	 * @param list
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart createQQChart(Double position, List<AnalysisDataset> list, ProfileType type) throws Exception {

		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Angle", "Probability", null, PlotOrientation.VERTICAL, true, true,
						false);

		XYPlot plot = chart.getXYPlot();
		
		int datasetCount = 0;
		int iteration = 0;
		for(AnalysisDataset dataset : list){
			
//			XYDataset ds 	 = NucleusDatasetCreator.createModalityProbabililtyDataset(position, dataset, type);
//			XYDataset values = NucleusDatasetCreator.createModalityValuesDataset(position, dataset, type);
			CellCollection collection = dataset.getCollection();

			double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(position);
			XYDataset ds = NucleusDatasetCreator.createQQDataset(values);
			
			Color colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(iteration)
					: dataset.getDatasetColour();

			plot.setDataset(datasetCount, ds);
			
			XYLineAndShapeRenderer lineRenderer =  new XYLineAndShapeRenderer(false, true);
			plot.setRenderer(datasetCount,lineRenderer);
			int seriesCount = plot.getDataset(datasetCount).getSeriesCount();
			for(int i=0; i<seriesCount;i++){
				
				lineRenderer.setSeriesPaint(i, colour);
				lineRenderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
				lineRenderer.setSeriesVisibleInLegend(i, false);
			}
			
			datasetCount++;
			iteration++;
			
		}
		return chart;
	}
	
	/**
	 * Create a blank chart with default formatting for probability values
	 * across a normalised profile
	 * @return
	 */
	public static JFreeChart makeBlankProbabililtyChart() {
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Position", "Probability", null, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 100);
		plot.getRangeAxis().setRange(0, 1);
		
		return chart;
	}
	
	/**
	 * Create a Kruskal-Wallis probability chart comparing two datasets.
	 * @param options the options to plot
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart makeKruskalWallisChart(ChartOptions options, boolean frankenNormalise) throws Exception {
		
		XYDataset kruskalDataset = null;
		
		if(frankenNormalise){
			kruskalDataset = NucleusDatasetCreator.createFrankenKruskalProfileDataset(options);
		} else {
			kruskalDataset = NucleusDatasetCreator.createKruskalProfileDataset(options);
		}

		XYDataset firstProfileDataset = NucleusDatasetCreator.createNonsegmentedMedianProfileDataset(options.firstDataset(),
				true,
				options.getAlignment(),
				options.getTag());
		
		XYDataset secondProfileDataset = NucleusDatasetCreator.createNonsegmentedMedianProfileDataset(options.getDatasets().get(1),
				true,
				options.getAlignment(),
				options.getTag());
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Position", "Probability", null, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 100);
		
		LogAxis rangeAxis = new LogAxis("Probability");
		rangeAxis.setBase(10);
		DecimalFormat df=new DecimalFormat();
		df.applyPattern("0.#E0");
		rangeAxis.setNumberFormatOverride(df);
		rangeAxis.setStandardTickUnits(new StandardTickUnitSource());

		plot.setRangeAxis(rangeAxis);
		
		NumberAxis angleAxis = new NumberAxis("Angle");
		angleAxis.setRange(0, 360);
		
		plot.setRangeAxis(0, rangeAxis);
		plot.setRangeAxis(1, angleAxis);
		
		plot.setDataset(0, kruskalDataset);
		plot.setDataset(1, firstProfileDataset);
		plot.setDataset(2, secondProfileDataset);
		
		
		XYItemRenderer logRenderer = new XYLineAndShapeRenderer(true, false);
		logRenderer.setSeriesPaint(0, Color.BLACK);
		logRenderer.setSeriesVisibleInLegend(0, false);
		logRenderer.setSeriesStroke(0, ChartComponents.MARKER_STROKE);
		
		XYItemRenderer angleRendererOne = new XYLineAndShapeRenderer(true, false);
		Color colorOne = options.getDatasets().get(0).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(0) 
					: options.getDatasets().get(0).getDatasetColour();
		angleRendererOne.setSeriesPaint(0, colorOne);
		angleRendererOne.setSeriesVisibleInLegend(0, false);
		angleRendererOne.setSeriesStroke(0, ChartComponents.MARKER_STROKE);
		
		XYItemRenderer angleRendererTwo = new XYLineAndShapeRenderer(true, false);
		Color colorTwo = options.getDatasets().get(1).getDatasetColour() == null 
				? ColourSelecter.getSegmentColor(1) 
				: options.getDatasets().get(1).getDatasetColour();
		angleRendererTwo.setSeriesPaint(0, colorTwo);
		angleRendererTwo.setSeriesVisibleInLegend(0, false);
		angleRendererTwo.setSeriesStroke(0, ChartComponents.MARKER_STROKE);

		
		plot.setRenderer(0, logRenderer);
		plot.setRenderer(1, angleRendererOne);
		plot.setRenderer(2, angleRendererTwo);
		
		plot.mapDatasetToRangeAxis(0, 0);
		plot.mapDatasetToRangeAxis(1, 1);
		plot.mapDatasetToRangeAxis(2, 1);
		

		return chart;
	}
}
