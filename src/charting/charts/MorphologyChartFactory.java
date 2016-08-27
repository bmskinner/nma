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
import java.util.logging.Level;

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
import components.generic.BooleanProfile;
import components.generic.BorderTagObject;
import components.generic.Profile;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class MorphologyChartFactory extends AbstractChartFactory {
	
	private static MorphologyChartFactory instance = null;
	
	protected MorphologyChartFactory(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static MorphologyChartFactory getInstance(){
		if(instance==null){
			instance = new MorphologyChartFactory();
		}
		return instance;
	}
	
	
	/**
	 * Create an empty chart
	 * @return
	 */
	public JFreeChart makeEmptyChart(){
		return makeEmptyProfileChart(ProfileType.ANGLE);
	}
	
	/**
	 * Create an empty chart to display when no datasets are selected
	 * @return
	 */
	public JFreeChart makeEmptyChart(ProfileType type){
		return makeEmptyProfileChart(type);
	}
	
	/**
	 * Create an empty chart to display when no datasets are selected
	 * @return a chart
	 */
	private JFreeChart makeEmptyProfileChart(ProfileType type){
		
		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();

		plot.getDomainAxis().setLabel("Position");
		plot.getDomainAxis().setRange(0,100);
		
		plot.getRangeAxis().setLabel(type.toString());
		
		if(type.getDimension().equals(StatisticDimension.ANGLE)){
			plot.getRangeAxis().setRange(0,360);
			plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
		}
		return chart;
	}
	
	
	/**
	 * Create a profile chart for the given options
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public JFreeChart createProfileChart(ChartOptions options) throws Exception {
		
		if( ! options.hasDatasets()){
			return makeEmptyProfileChart(options.getType());
		}
		
		if(options.isSingleDataset() &&  !options.isHideProfiles() ){
			return makeSingleProfileChart(options);
		}
		
		if(options.isMultipleDatasets() || options.isHideProfiles() ){
			return makeMultiProfileChart(options);
		}
		return makeEmptyProfileChart(options.getType());
	}
	
	/**
	 * Make a profile chart for a single nucleus
	 * @param options
	 * @return
	 */
	public JFreeChart makeIndividualNucleusProfileChart(ChartOptions options) {
		
		if(options.isMultipleDatasets()){
			return makeEmptyChart();
		}
		
		if(options.getCell()==null){
			return makeEmptyChart();
		}
		
		finest("Creating individual nucleus profile chart");
				
		Nucleus n = options.getCell().getNucleus();
		
		XYDataset  ds 	 = NucleusDatasetCreator.getInstance().createSegmentedProfileDataset(n, options.getType());
		JFreeChart chart = makeProfileChart(ds, n.getBorderLength(), options.getType());
		
		XYPlot plot = chart.getXYPlot();
				
		DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		renderer.setBaseShapesVisible(options.isShowPoints());
		renderer.setBaseLinesVisible(options.isShowLines());
		renderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		plot.setRenderer(0, renderer);
		
		// Colour the segments in the plot
		int seriesCount = plot.getDataset().getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			
			renderer.setSeriesVisibleInLegend(i, false);
			
			String name = (String) ds.getSeriesKey(i);
			
			// segments along the median profile
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				renderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);

				
				Color colour = ColourSelecter.getColor(colourIndex);
				
				renderer.setSeriesPaint(i, colour);
				renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
			} 
		}
					
		// Add markers
		if(options.isShowMarkers()){

			finest("Adding segment markers");
			for (BorderTagObject tag : n.getBorderTags().keySet()){

				// get the index of the tag

				int index = n.getBorderIndex(tag);
				finest("Raw index of "+tag+" is "+index);
				
				// Correct to start from RP
				// get the offset from to the current draw point
				int offset = n.getBorderIndex(options.getTag());

				// adjust the index to the offset
				index = AbstractCellularComponent.wrapIndex( index - offset, n.getBorderLength());

				finest("Index of "+tag+" from RP is "+index);
				double indexToDraw = index; // convert to a double to allow normalised positioning

				addMarkerToXYPlot(plot, tag, indexToDraw);

			}
		}

		// Add segment name annotations

		if(options.isShowAnnotations()){
			finest("Adding segment annotations");
			for(NucleusBorderSegment seg : n.getProfile(options.getType(), options.getTag()).getOrderedSegments()){

				int midPoint = seg.getMidpointIndex();

				double x = midPoint;
				if(options.isNormalised()){
					x = ((double) midPoint / (double) seg.getTotalLength() ) * 100;
				}
				XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), x, 320);
				
				Color colour = ColourSelecter.getColor(seg.getPosition());
				
				segmentAnnotation.setPaint(colour);
				plot.addAnnotation(segmentAnnotation);
			}
		}
		
				
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
	private JFreeChart makeSingleProfileChart(ChartOptions options) throws Exception {
		
		XYDataset ds = null;
		AnalysisDataset dataset = options.firstDataset();
		CellCollection collection = dataset.getCollection();
		JFreeChart chart = null;
				
		if(options.hasDatasets()){

			if(options.getType().equals(ProfileType.FRANKEN)){
				ds = NucleusDatasetCreator.getInstance().createFrankenSegmentDataset(options);

			} else {
				ds = NucleusDatasetCreator.getInstance().createSegmentedProfileDataset(options);
			}
			

			int length = 100 ; // default if normalised


			// if we set raw values, get the maximum nucleus length
			if(!options.isNormalised()){
				length = (int) collection.getMaxProfileLength();
			}

			chart = makeProfileChart(ds, length, options.getType());

			// mark the reference and orientation points

			XYPlot plot = chart.getXYPlot();

			for (BorderTagObject tag : collection.getProfileCollection(options.getType()).getBorderTags()){

				// get the index of the tag
				int index = collection.getProfileCollection(options.getType()).getIndex(tag);

				// get the offset from to the current draw point
				int offset = collection.getProfileCollection(options.getType()).getIndex(options.getTag());

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
			chart = makeProfileChart(null, 100, options.getType());
		}
		return chart;
	}
	
	
	/**
	 * Create a profile chart with the median line only, segmented. Can have multiple datasets shown.
	 * If a single dataset is shown, the chart is real length, otherwise normalised
	 * @param options
	 * @throws Exception
	 */	
	public JFreeChart makeMultiSegmentedProfileChart(ChartOptions options)  {
		
		// Set the length to 100 if normalised or multiple datasets.
		// Otherwise use the median profile length
		int length = options.isNormalised() || options.isMultipleDatasets()
				   ? 100 
				   : options.firstDataset()
				   		.getCollection()
				   		.getProfileCollection(ProfileType.ANGLE)
				   		.getProfile(BorderTagObject.REFERENCE_POINT, Constants.MEDIAN)
				   		.size();
				
		
		JFreeChart chart = this.makeEmptyChart(options.getType());
		XYPlot plot = chart.getXYPlot();
		
//		JFreeChart chart = ChartFactory.createXYLineChart(null,
//				                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
//				                false);
////		JFreeChart chart = makeProfileChart(null, length, list.get(0).getSwatch());
//		
//		XYPlot plot = chart.getXYPlot();
		
		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,length);

//		// always set the y range to 360 degrees
//		plot.getRangeAxis().setRange(0,360);
//		plot.setBackgroundPaint(Color.WHITE);

		// the 180 degree line
		plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
				
		int datasetIndex = 0;
		for(AnalysisDataset dataset : options.getDatasets()){

			XYDataset ds = NucleusDatasetCreator.getInstance().createSegmentedMedianProfileDataset( options);

			plot.setDataset(datasetIndex, ds);
			
			DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
			renderer.setBaseShapesVisible(options.isShowPoints());
			renderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
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
					
					Color colour = ColourSelecter.getColor(colourIndex);
				
					renderer.setSeriesPaint(i, colour);
					renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
				} 
			}	

			datasetIndex++;
		}
		
		
		if(options.isSingleDataset()){
			
			// Add markers
			if(options.isShowMarkers()){

				CellCollection collection = options.firstDataset().getCollection();
				for (BorderTagObject tag : collection.getProfileCollection(options.getType()).getBorderTags()){

					// get the index of the tag
					int index = collection.getProfileCollection(options.getType()).getIndex(tag);

					// get the offset from to the current draw point
					int offset = collection.getProfileCollection(options.getType()).getIndex(options.getTag());

					// adjust the index to the offset
					index = AbstractCellularComponent.wrapIndex( index - offset, collection.getProfileCollection(options.getType()).getAggregate().length());

					double indexToDraw = index; // convert to a double to allow normalised positioning

					if(options.isNormalised()){ // set to the proportion of the point along the profile
						indexToDraw =  (( indexToDraw / collection.getProfileCollection(options.getType()).getAggregate().length() ) * 100);
					}

					addMarkerToXYPlot(plot, tag, indexToDraw);
					
				}
			}
			
			// Add segment name annotations
			
			if(options.isShowAnnotations()){
				for(NucleusBorderSegment seg :  options.firstDataset().getCollection()
						.getProfileCollection(ProfileType.ANGLE)
						.getSegmentedProfile(options.getTag())
						.getOrderedSegments()){

					int midPoint = seg.getMidpointIndex();

					double x = ((double) midPoint / (double) seg.getTotalLength() ) * 100;
					XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), x, 320);
					
					Color colour = ColourSelecter.getColor(seg.getPosition());
					segmentAnnotation.setPaint(colour);
					plot.addAnnotation(segmentAnnotation);
				}
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
	private JFreeChart makeProfileChart(XYDataset ds, int xLength, ProfileType type) {


		JFreeChart chart = makeEmptyProfileChart(type);
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(ds);

		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,xLength);

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
				Color colour = ColourSelecter.getColor(colourIndex);
				plot.getRenderer().setSeriesPaint(i, colour);

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
	private JFreeChart makeMultiProfileChart(ChartOptions options)  throws Exception{
				
		List<XYSeriesCollection> iqrProfiles = null;
		XYDataset medianProfiles			 = null;
				
		if(options.getType().equals(ProfileType.FRANKEN)){
			iqrProfiles     = NucleusDatasetCreator.getInstance().createMultiProfileIQRFrankenDataset(  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());				
			medianProfiles	= NucleusDatasetCreator.getInstance().createMultiProfileFrankenDataset(	options );
		} else {
			iqrProfiles     = NucleusDatasetCreator.getInstance().createMultiProfileIQRDataset( options );				
			medianProfiles	= NucleusDatasetCreator.getInstance().createMultiProfileDataset(    options );
		
		}
		
		
		// find the maximum profile length - used when rendering raw profiles
		int length = 100;

		if( ! options.isNormalised()){
			for(AnalysisDataset d : options.getDatasets()){
				length = (int) Math.max( d.getCollection().getMedianArrayLength(), length);
			}
		}
		
		
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", options.getType().getLabel(), null, PlotOrientation.VERTICAL, true, true,
				                false);
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,length);
		
		if(options.getType().getDimension().equals(StatisticDimension.ANGLE)){
			plot.getRangeAxis().setRange(0,360);
			plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
		}
		plot.setBackgroundPaint(Color.WHITE);


		int lastSeries = 0;

		for(int i=0;i<iqrProfiles.size();i++){
			XYSeriesCollection seriesCollection = iqrProfiles.get(i);

			// add to dataset
			plot.setDataset(i, seriesCollection);


			// find the series index
			String name = (String) seriesCollection.getSeriesKey(0);

			// index should be the position in the AnalysisDatase list
			// see construction in NucleusDatasetCreator.getInstance()
			int index = MorphologyChartFactory.getIndexFromLabel(name); 

			// make a transparent color based on teh profile segmenter system
			Color profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getColor(i)
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
					? ColourSelecter.getColor(j)
					: options.getDatasets().get(index).getDatasetColour();
					
			medianRenderer.setSeriesPaint(j, profileColour.darker());
		}
		
		
		
		return chart;
	}
	
	public JFreeChart makeVariabilityChart(ChartOptions options) {
		
		if( ! options.hasDatasets()){
			return makeEmptyProfileChart(options.getType());
		}
		
		try {

			if(options.isSingleDataset()){
				return makeSingleVariabilityChart(options);
			}

			if(options.isMultipleDatasets()){
				return makeMultiVariabilityChart(options);
			}
		} catch(Exception e){
			warn("Error making variability chart");
			log(Level.FINE, "Error making variability chart", e);
			return makeEmptyProfileChart(options.getType());
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
	private JFreeChart makeSingleVariabilityChart(ChartOptions options) {
		XYDataset ds = NucleusDatasetCreator.getInstance().createIQRVariabilityDataset(options);
		
		JFreeChart chart = makeProfileChart(ds, 100, options.getType());
		XYPlot plot = chart.getXYPlot();
		plot.getRangeAxis().setLabel("IQR");
		plot.getRangeAxis().setAutoRange(true);
		
		
		if(options.isShowMarkers()){ // add the bimodal regions
			CellCollection collection = options.firstDataset().getCollection();

			// dip test the profiles

			double significance   = options.getModalityPosition();
			BooleanProfile modes  = DipTester.testCollectionIsUniModal(collection, options.getTag(), significance, options.getType());


			// add any regions with bimodal distribution to the chart

			Profile xPositions = modes.getPositions(100);

			for(int i=0; i<modes.size(); i++){
				double x = xPositions.get(i);
				if(modes.get(i)==true){
					ValueMarker marker = new ValueMarker(x, Color.black, new BasicStroke(2f));
					plot.addDomainMarker(marker);
				}
			}
			
			
			try {

				double ymax = DatasetUtilities.findMaximumRangeValue(plot.getDataset()).doubleValue();
				DecimalFormat df = new DecimalFormat("#0.000"); 
				XYTextAnnotation annotation = new XYTextAnnotation("Markers for non-unimodal positions (p<"+df.format(significance)+")",1, ymax);
				annotation.setTextAnchor(TextAnchor.TOP_LEFT);
				plot.addAnnotation(annotation);
			} catch (IllegalArgumentException ex){
				fine("Missing data in variability chart");
			}
		}
		
		return chart;
	}
	
	/**
	 * Create a variabillity chart showing the IQR for a multiple datasets.
	 * @param list the datasets
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	private JFreeChart makeMultiVariabilityChart(ChartOptions options) throws Exception {
		XYDataset ds = NucleusDatasetCreator.getInstance().createIQRVariabilityDataset(options);
		
		JFreeChart chart = this.createBaseXYChart();
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(ds);
		
		plot.getDomainAxis().setRange(0,100);
		plot.getDomainAxis().setLabel("Position");;
		
		plot.getRangeAxis().setAutoRange(true);
		plot.getRangeAxis().setLabel("IQR");

		for (int j = 0; j < ds.getSeriesCount(); j++) {
			plot.getRenderer().setSeriesVisibleInLegend(j, false);
			plot.getRenderer().setSeriesStroke(j, ChartComponents.QUARTILE_STROKE);
			int index = MorphologyChartFactory.getIndexFromLabel( (String) ds.getSeriesKey(j));
			Color profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getColor(index)
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
	private JFreeChart makeMultiSegmentStartPositionChart(ChartOptions options) throws Exception {
		
		finest("Creating multi segment start position chart");
		
		XYDataset positionDataset = CellDatasetCreator.getInstance().createPositionFeatureDataset(options);
		finest("Created position dataset");
		
		
		XYDataset nuclearOutlines = NucleusDatasetCreator.getInstance().createMultiNucleusOutline(options.getDatasets(), options.getScale());
		finest("Created nucleus outline dataset");
		
		if(positionDataset == null || nuclearOutlines == null){
			// a null dataset is returned if segment counts do not match
			return ConsensusNucleusChartFactory.getInstance().makeEmptyChart();
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
		pointRenderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		pointRenderer.setBaseLinesVisible(false);
		pointRenderer.setBaseStroke(ChartComponents.QUARTILE_STROKE);
		pointRenderer.setBaseSeriesVisibleInLegend(false);
		pointRenderer.setBaseToolTipGenerator(tooltip);
		plot.setRenderer(0, pointRenderer);
		
		boolean hasConsensus = ConsensusNucleusChartFactory.getInstance().hasConsensusNucleus(options.getDatasets());
		if(hasConsensus){
			// Find the bounds of the consensus nuclei in the options
			double max = ConsensusNucleusChartFactory.getInstance().getconsensusChartRange(options.getDatasets());
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
					? ColourSelecter.getColor(j)
					: options.getDatasets().get(j).getDatasetColour();
			
			pointRenderer.setSeriesPaint(j, profileColour);
			pointRenderer.setSeriesShape(j, ChartComponents.DEFAULT_POINT_SHAPE);
			
			if(hasConsensus){
				plot.getRenderer(1).setSeriesPaint(j, profileColour);
			}
		}
		finest("Created segment position chart");
		return chart;
		
	}
	
	
	/**
	 * Create a chart showing the distribution of xy points for a segment start
	 * @param options the chart options. Should have a segID
	 * @return a chart
	 */
	public JFreeChart makeSegmentStartPositionChart(ChartOptions options) throws Exception {
		
		if(  options.hasDatasets()){
			return  makeMultiSegmentStartPositionChart(options);
		}
		return ConsensusNucleusChartFactory.getInstance().makeEmptyChart();
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
	private JFreeChart createModalityChart(Double position, List<AnalysisDataset> list, ProfileType type) throws Exception {
		
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
			
			XYDataset ds 	 = NucleusDatasetCreator.getInstance().createModalityProbabililtyDataset(position, dataset, type);
			XYDataset values = NucleusDatasetCreator.getInstance().createModalityValuesDataset(position, dataset, type);
			
			Color colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getColor(iteration)
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
	
	public JFreeChart createModalityProfileChart(ChartOptions options) throws Exception {
		
		XYDataset ds = NucleusDatasetCreator.getInstance().createModalityProfileDataset(options);
		
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
			
			Color colour = dataset.hasDatasetColour()
					? dataset.getDatasetColour()
					: ColourSelecter.getColor(i);

			plot.getRenderer().setSeriesPaint(i, colour);
			plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
		}
		return chart;
	}
		
	public JFreeChart createModalityPositionChart(ChartOptions options) throws Exception {

		if( ! options.hasDatasets()){
			return makeEmptyChart();
		}
		
		
		JFreeChart chart = createModalityChart(options.getModalityPosition(), options.getDatasets(), options.getType());
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
					? ColourSelecter.getColor(index)
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
			
//			XYDataset ds 	 = NucleusDatasetCreator.getInstance().createModalityProbabililtyDataset(position, dataset, type);
//			XYDataset values = NucleusDatasetCreator.getInstance().createModalityValuesDataset(position, dataset, type);
			CellCollection collection = dataset.getCollection();

			double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(position);
			XYDataset ds = NucleusDatasetCreator.getInstance().createQQDataset(values);
			
			Color colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getColor(iteration)
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
			kruskalDataset = NucleusDatasetCreator.getInstance().createFrankenKruskalProfileDataset(options);
		} else {
			kruskalDataset = NucleusDatasetCreator.getInstance().createKruskalProfileDataset(options);
		}

		XYDataset firstProfileDataset = NucleusDatasetCreator.getInstance().createNonsegmentedMedianProfileDataset(options.firstDataset(),
				true,
				options.getAlignment(),
				options.getTag());
		
		XYDataset secondProfileDataset = NucleusDatasetCreator.getInstance().createNonsegmentedMedianProfileDataset(options.getDatasets().get(1),
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
					? ColourSelecter.getColor(0) 
					: options.getDatasets().get(0).getDatasetColour();
		angleRendererOne.setSeriesPaint(0, colorOne);
		angleRendererOne.setSeriesVisibleInLegend(0, false);
		angleRendererOne.setSeriesStroke(0, ChartComponents.MARKER_STROKE);
		
		XYItemRenderer angleRendererTwo = new XYLineAndShapeRenderer(true, false);
		Color colorTwo = options.getDatasets().get(1).getDatasetColour() == null 
				? ColourSelecter.getColor(1) 
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
	
	/**
	 * Create a chart showing the effect of a boolean profile on a profile
	 * @param p
	 * @param limits
	 * @return
	 */
	public JFreeChart createBooleanProfileChart(Profile p, BooleanProfile limits){
		
		XYDataset ds = NucleusDatasetCreator.getInstance().createBooleanProfileDataset(p, limits);
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);

		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, ChartComponents.MARKER_STROKE));
		
		
		
		DefaultXYItemRenderer rend = new DefaultXYItemRenderer();
		rend.setBaseShapesVisible(true);
		rend.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		rend.setSeriesPaint(0, Color.BLACK);
		rend.setSeriesVisibleInLegend(0, false);
		rend.setSeriesPaint(1, Color.LIGHT_GRAY);
		rend.setSeriesVisibleInLegend(1, false);
		rend.setSeriesLinesVisible(0, false);
		rend.setSeriesShape(0, ChartComponents.DEFAULT_POINT_SHAPE);
		rend.setSeriesLinesVisible(1, false);
		rend.setSeriesShape(1, ChartComponents.DEFAULT_POINT_SHAPE);
		
		plot.setRenderer(rend);

		return chart;
	}
}
