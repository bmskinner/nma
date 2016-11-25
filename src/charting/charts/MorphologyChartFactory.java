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
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
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
import stats.Quartile;
import stats.StatisticDimension;
import utility.Constants;
import analysis.IAnalysisDataset;
import analysis.profiles.ProfileException;
import charting.ChartComponents;
import charting.datasets.CellDatasetCreator;
import charting.datasets.ChartDatasetCreationException;
import charting.datasets.NucleusDatasetCreator;
import charting.options.ChartOptions;
import components.AbstractCellularComponent;
import components.ICellCollection;
import components.active.DefaultCellularComponent;
import components.active.generic.UnavailableBorderTagException;
import components.active.generic.UnavailableProfileTypeException;
import components.active.generic.UnsegmentedProfileException;
import components.generic.BooleanProfile;
import components.generic.IProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
import components.nuclei.Nucleus;

public class MorphologyChartFactory extends AbstractChartFactory {
	
	public MorphologyChartFactory(ChartOptions o){
		super(o);
	}
	
	
	/**
	 * Create an empty chart
	 * @return
	 */
	public static JFreeChart createEmptyChart(){
		return makeEmptyProfileChart(ProfileType.ANGLE);
	}
	
	/**
	 * Create an empty chart with display options
	 * @return
	 */
	public JFreeChart makeEmptyChart(){
		JFreeChart chart = makeEmptyProfileChart(options.getType());
		
		applyAxisOptions(chart);
		
		return chart;
	}
	
	/**
	 * Create an empty chart to display when no datasets are selected
	 * @return
	 */
	public static JFreeChart makeEmptyChart(ProfileType type){
		return makeEmptyProfileChart(type);
	}
	
	/**
	 * Create an empty chart to display when no datasets are selected
	 * @return a chart
	 */
	private static JFreeChart makeEmptyProfileChart(ProfileType type){
		
		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();

		plot.getDomainAxis().setLabel("Position");
		plot.getDomainAxis().setRange(0,100);
		
		plot.getRangeAxis().setLabel(type.getLabel());
		
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
	public JFreeChart createProfileChart(){
		
		if( ! options.hasDatasets()){
			return makeEmptyProfileChart(options.getType());
		}
		
		if(options.isSingleDataset() &&  !options.isHideProfiles() ){
			return makeSingleProfileChart();
		}
		
		if(options.isMultipleDatasets() || options.isHideProfiles() ){
			return makeMultiProfileChart();
		}
		return makeEmptyProfileChart(options.getType());
	}
	
	/**
	 * Make a profile chart for a single nucleus
	 * @param options
	 * @return
	 */
	public JFreeChart makeIndividualNucleusProfileChart() {
		
		if(options.isMultipleDatasets()){
			return makeEmptyChart();
		}
		
		if(options.getCell()==null){
			return makeEmptyChart();
		}
		
		finest("Creating individual nucleus profile chart");
				
		Nucleus n = options.getCell().getNucleus();
		
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createSegmentedProfileDataset(n, options.getType());
		} catch (ChartDatasetCreationException e) {
			fine("Error creating profile chart", e);
			return makeErrorChart();
		}
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

				
				Paint colour = ColourSelecter.getColor(colourIndex);
				
				renderer.setSeriesPaint(i, colour);
				renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
			} 
		}
					
		// Add markers
		if(options.isShowMarkers()){

			finest("Adding segment markers");
			for (Tag tag : n.getBorderTags().keySet()){

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
			try {
				for(IBorderSegment seg : n.getProfile(options.getType(), options.getTag()).getOrderedSegments()){

					int midPoint = seg.getMidpointIndex();

					double x = midPoint;
					if(options.isNormalised()){
						x = ((double) midPoint / (double) seg.getTotalLength() ) * 100;
					}
					XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), x, 320);
					
					Paint colour = ColourSelecter.getColor(seg.getPosition());
					
					segmentAnnotation.setPaint(colour);
					plot.addAnnotation(segmentAnnotation);
				}
			} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
				fine("Error adding segment annotations", e);
				return makeErrorChart();
			}
		}
		
		applyAxisOptions(chart);
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
	private JFreeChart makeSingleProfileChart() {
		
		XYDataset ds = null;
		IAnalysisDataset dataset = options.firstDataset();
		ICellCollection collection = dataset.getCollection();
		JFreeChart chart = null;
				
		if(options.hasDatasets()){
			
			try {

				if(options.getType().equals(ProfileType.FRANKEN)){
					ds = new NucleusDatasetCreator().createFrankenSegmentDataset(options);

				} else {
					ds = new NucleusDatasetCreator().createSegmentedProfileDataset(options);
				}
			} catch(ChartDatasetCreationException e){
				fine("Error making profile dataset", e);
				return makeErrorChart();
			}
			

			int length = 100 ; // default if normalised


			// if we set raw values, get the maximum nucleus length
			if(!options.isNormalised()){
				length = collection.getMaxProfileLength();
			}

			chart = makeProfileChart(ds, length, options.getType());

			// mark the reference and orientation points

			XYPlot plot = chart.getXYPlot();

			for (Tag tag : collection.getProfileCollection().getBorderTags()){

				// get the index of the tag
				
				try {
				
					int index = collection.getProfileCollection().getIndex(tag);

					// get the offset from to the current draw point
					int offset = collection.getProfileCollection().getIndex(options.getTag());

					// adjust the index to the offset
					index = DefaultCellularComponent.wrapIndex( index - offset, collection.getProfileCollection().length());

					double indexToDraw = index; // convert to a double to allow normalised positioning

					if(options.isNormalised()){ // set to the proportion of the point along the profile
						indexToDraw =  (( indexToDraw / collection.getProfileCollection().length() ) * 100);
					}
					if(options.getAlignment().equals(ProfileAlignment.RIGHT) && !options.isNormalised()){
						int maxX = DatasetUtilities.findMaximumDomainValue(ds).intValue();
						int amountToAdd = maxX - collection.getProfileCollection().length();
						indexToDraw += amountToAdd;

					}

					if(options.isShowMarkers()){

						addMarkerToXYPlot(plot, tag, indexToDraw);

					}
				
				} catch(UnavailableBorderTagException e){
					fine("Tag not present in profile: "+tag);
				}

			}
		} else {
			chart = makeProfileChart(null, 100, options.getType());
		}
		
		applyAxisOptions(chart);
		return chart;
	}
	
	
	/**
	 * Create a profile chart with the median line only, segmented. Can have multiple datasets shown.
	 * If a single dataset is shown, the chart is real length, otherwise normalised
	 * @param options
	 * @throws Exception
	 */	
	public JFreeChart makeMultiSegmentedProfileChart()  {
		
		JFreeChart chart = makeEmptyChart();
		
		if( ! options.hasDatasets()){
			return chart;
		}
		applyAxisOptions(chart);
		
		XYPlot plot = chart.getXYPlot();
	
		
		// Set the length to 100 if normalised or multiple datasets.
		// Otherwise use the median profile length
		int length;
		try {
			length = options.isNormalised() || options.isMultipleDatasets()
					   ? 100 
					   : options.firstDataset()
					   		.getCollection()
					   		.getProfileCollection()
					   		.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
					   		.size();
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			stack("Error getting median profile", e);
			return makeErrorChart();
		}
				

		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,length);

		// the 180 degree line
		plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
				
		for(int datasetIndex = 0; datasetIndex< options.datasetCount(); datasetIndex++){

			XYDataset ds;
			try {
				ds = new NucleusDatasetCreator().createSegmentedMedianProfileDataset( options);
			} catch (ChartDatasetCreationException e) {
				stack("Error creating median profile dataset", e);
				return makeErrorChart();
			}

			plot.setDataset(datasetIndex, ds);
			
			DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
			renderer.setBaseShapesVisible(options.isShowPoints());
			renderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
			plot.setRenderer(datasetIndex, renderer);

			int seriesCount = plot.getDataset(datasetIndex).getSeriesCount();

			for (int i = 0; i < seriesCount; i++) {
				
				renderer.setSeriesVisibleInLegend(i, false);
				
				String name = ds.getSeriesKey(i).toString();

				
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

				ICellCollection collection = options.firstDataset().getCollection();
				for (Tag tag : collection.getProfileCollection().getBorderTags()){

					try {
						// get the index of the tag
						int index = collection.getProfileCollection().getIndex(tag);

						// get the offset from to the current draw point
						int offset = collection.getProfileCollection().getIndex(options.getTag());

						// adjust the index to the offset
						index = DefaultCellularComponent.wrapIndex( index - offset, collection.getProfileCollection().length());

						
						if(options.isNormalised()){ // set to the proportion of the point along the profile
							// convert to a double to allow normalised positioning
							double indexToDraw =  ((  (double) index / collection.getProfileCollection().length() ) * 100);
							addMarkerToXYPlot(plot, tag, indexToDraw);
						} else {
							addMarkerToXYPlot(plot, tag, index);
						}

						
					
				} catch(UnavailableBorderTagException e){
					stack("Tag not present in profile: "+tag, e);
				}
					
				}
			}
			
			// Add segment name annotations
			
			if(options.isShowAnnotations()){
				try {
					for(IBorderSegment seg :  options.firstDataset().getCollection()
							.getProfileCollection()
							.getSegmentedProfile(ProfileType.ANGLE, options.getTag(), Quartile.MEDIAN)
							.getOrderedSegments()){

						int midPoint = seg.getMidpointIndex();
						double xToDraw = midPoint;
						if(options.isNormalised()){
							xToDraw = ((double) midPoint / (double) seg.getTotalLength() ) * 100;
						}
						

						XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), xToDraw, 320);
						
						Paint colour = ColourSelecter.getColor(seg.getPosition());
						segmentAnnotation.setPaint(colour);
						plot.addAnnotation(segmentAnnotation);
					}
				} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
					stack("Error creating median profile dataset", e);
					return makeErrorChart();
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
				Paint colour = ColourSelecter.getColor(colourIndex);
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
	private JFreeChart makeMultiProfileChart(){
				
		List<XYSeriesCollection> iqrProfiles = null;
		XYDataset medianProfiles			 = null;
		
		NucleusDatasetCreator creator = new NucleusDatasetCreator();
				
		try {

			if(options.getType().equals(ProfileType.FRANKEN)){
				iqrProfiles     = creator.createMultiProfileIQRFrankenDataset(  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());				
				medianProfiles	= creator.createMultiProfileFrankenDataset(	options );
			} else {
				iqrProfiles     = creator.createMultiProfileIQRDataset( options );				
				medianProfiles	= creator.createMultiProfileDataset(    options );

			}

		}catch(ChartDatasetCreationException e){
			return makeErrorChart();
		}
		
		
		// find the maximum profile length - used when rendering raw profiles
		int length = 100;

		if( ! options.isNormalised()){
			for(IAnalysisDataset d : options.getDatasets()){
				length = (int) Math.max( d.getCollection().getMedianArrayLength(), length);
			}
		}
		
		
		JFreeChart chart = createBaseXYChart();
//		JFreeChart chart = 
//				ChartFactory.createXYLineChart(null,
//				                "Position", options.getType().getLabel(), null, PlotOrientation.VERTICAL, true, true,
//				                false);
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setLabel("Position");
		plot.getRangeAxis().setLabel(options.getType().getLabel());
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
			Paint profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getColor(i)
					: options.getDatasets().get(index).getDatasetColour();

					Paint iqrColour		= ColourSelecter.getTransparentColour((Color) profileColour, true, 128);

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
			
			Paint profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getColor(j)
					: options.getDatasets().get(index).getDatasetColour();
					
			medianRenderer.setSeriesPaint(j, ((Color) profileColour).darker());
		}
		
		
		applyAxisOptions(chart);
		return chart;
	}
	
	public JFreeChart makeVariabilityChart() {
		
		if( ! options.hasDatasets()){
			return makeEmptyProfileChart(options.getType());
		}
		
		try {

			if(options.isSingleDataset()){
				return makeSingleVariabilityChart();
			}

			if(options.isMultipleDatasets()){
				return makeMultiVariabilityChart();
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
	private JFreeChart makeSingleVariabilityChart() {
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createIQRVariabilityDataset(options);
		} catch (ChartDatasetCreationException e) {
			fine("Error creating variability chart", e);
			return makeErrorChart();
		}
		
		JFreeChart chart = makeProfileChart(ds, 100, options.getType());
		XYPlot plot = chart.getXYPlot();
		plot.getRangeAxis().setLabel("IQR");
		plot.getRangeAxis().setAutoRange(true);
		
		
		if(options.isShowMarkers()){ // add the bimodal regions
			ICellCollection collection = options.firstDataset().getCollection();

			// dip test the profiles

			double significance   = options.getModalityPosition();
			BooleanProfile modes  = new DipTester(collection).testCollectionIsUniModal(options.getTag(), significance, options.getType());


			// add any regions with bimodal distribution to the chart

			IProfile xPositions = modes.getPositions(100);

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
		applyAxisOptions(chart);
		return chart;
	}
	
	/**
	 * Create a variabillity chart showing the IQR for a multiple datasets.
	 * @param list the datasets
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	private JFreeChart makeMultiVariabilityChart() throws Exception {
		XYDataset ds =  new NucleusDatasetCreator().createIQRVariabilityDataset(options);
		
		JFreeChart chart = createBaseXYChart();
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
			Paint profileColour = options.getDatasets().get(index).getDatasetColour() == null 
					? ColourSelecter.getColor(index)
					: options.getDatasets().get(index).getDatasetColour();
			
			plot.getRenderer().setSeriesPaint(j, profileColour);
		}	
		applyAxisOptions(chart);
		return chart;
	}
		
	/**
	 * Create a segment start XY position chart for multiple analysis datasets
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private JFreeChart makeMultiSegmentStartPositionChart(ChartOptions options) {
		
		finest("Creating multi segment start position chart");
		
		XYDataset positionDataset;
		try {
			positionDataset = new CellDatasetCreator().createPositionFeatureDataset(options);
		} catch (ChartDatasetCreationException e) {
			return makeErrorChart();
		}
		finest("Created position dataset");
		
		
		XYDataset nuclearOutlines;
		try {
			nuclearOutlines = new NucleusDatasetCreator().createMultiNucleusOutline(options.getDatasets(), options.getScale());
		} catch (ChartDatasetCreationException e) {
			return makeErrorChart();
		}
		finest("Created nucleus outline dataset");
		
		if(positionDataset == null || nuclearOutlines == null){
			// a null dataset is returned if segment counts do not match
			return ConsensusNucleusChartFactory.makeEmptyChart();
		}
		
		
		JFreeChart chart = createBaseXYChart();
		
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
		
		boolean hasConsensus = new ConsensusNucleusChartFactory(options).hasConsensusNucleus();
		if(hasConsensus){
			// Find the bounds of the consensus nuclei in the options
			double max = new ConsensusNucleusChartFactory(options).getconsensusChartRange();
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

			Paint profileColour = options.getDatasets().get(j).getDatasetColour() == null 
					? ColourSelecter.getColor(j)
					: options.getDatasets().get(j).getDatasetColour();
			
			pointRenderer.setSeriesPaint(j, profileColour);
			pointRenderer.setSeriesShape(j, ChartComponents.DEFAULT_POINT_SHAPE);
			
			if(hasConsensus){
				plot.getRenderer(1).setSeriesPaint(j, profileColour);
			}
		}
		applyAxisOptions(chart);
		finest("Created segment position chart");
		return chart;
		
	}
	
	
	/**
	 * Create a chart showing the distribution of xy points for a segment start
	 * @param options the chart options. Should have a segID
	 * @return a chart
	 */
	public JFreeChart makeSegmentStartPositionChart() {
		
		if(  options.hasDatasets()){
			return  makeMultiSegmentStartPositionChart(options);
		}
		return ConsensusNucleusChartFactory.makeEmptyChart();
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
	private JFreeChart createModalityChart(Double position, List<IAnalysisDataset> list, ProfileType type) throws Exception {
		
		JFreeChart chart = createBaseXYChart();
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setLabel(type.getLabel());
		plot.getRangeAxis().setLabel("Probability");
		
		if(type.getDimension().equals(StatisticDimension.ANGLE)){
			plot.getDomainAxis().setRange(0, 360);
		}
		

		plot.addDomainMarker(new ValueMarker(180, Color.BLACK, ChartComponents.MARKER_STROKE));
		
		int datasetCount = 0;
		int iteration = 0;
		for(IAnalysisDataset dataset : list){
			
			XYDataset ds 	 =  new NucleusDatasetCreator().createModalityProbabililtyDataset(position, dataset, type);
			XYDataset values =  new NucleusDatasetCreator().createModalityValuesDataset(position, dataset, type);
			
			Paint colour = dataset.getDatasetColour() == null 
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
	
	public JFreeChart createModalityProfileChart() {
		
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createModalityProfileDataset(options);
		} catch (ChartDatasetCreationException e) {
			return makeErrorChart();
		}
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Position", "Probability", ds, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 100);
		plot.getRangeAxis().setRange(0, 1);
		
		for(int i=0; i<options.getDatasets().size(); i++){
			
			IAnalysisDataset dataset = options.getDatasets().get(i);
			
			Paint colour = dataset.hasDatasetColour()
					? dataset.getDatasetColour()
					: ColourSelecter.getColor(i);

			plot.getRenderer().setSeriesPaint(i, colour);
			plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
		}
		applyAxisOptions(chart);
		return chart;
	}
		
	public JFreeChart createModalityPositionChart() {

		if( ! options.hasDatasets()){
			return makeEmptyChart();
		}
		
		
		JFreeChart chart;
		try {
			chart = createModalityChart(options.getModalityPosition(), options.getDatasets(), options.getType());
		} catch (Exception e) {
			return makeErrorChart();
		}
		XYPlot plot = chart.getXYPlot();

		double yMax = 0;
		DecimalFormat df = new DecimalFormat("#0.000");

		for(int i = 0; i<plot.getDatasetCount(); i++){

			// Ensure annotation is placed in the right y position
			double y = DatasetUtilities.findMaximumRangeValue(plot.getDataset(i)).doubleValue();
			yMax = y > yMax ? y : yMax;

		}

		int index = 0;
		for(IAnalysisDataset dataset : options.getDatasets()){

			// Do the stats testing
			double pvalue;
			try {
				pvalue = new DipTester(dataset.getCollection()).getPValueForPositon(	options.getModalityPosition(), 
						options.getType());
			} catch (Exception e) {
				return makeErrorChart();
			} 
			
			// Add the annotation
			double yPos = yMax - ( index * (yMax / 20));
			String statisticalTesting = "p(unimodal) = "+df.format(pvalue);
			if(pvalue<Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
				statisticalTesting = "* " + statisticalTesting;
			}
			XYTextAnnotation annotation = new XYTextAnnotation(statisticalTesting,355, yPos);

			// Set the text colour
			Paint colour = dataset.getDatasetColour() == null 
					? ColourSelecter.getColor(index)
							: dataset.getDatasetColour();
					annotation.setPaint(colour);
					annotation.setTextAnchor(TextAnchor.TOP_RIGHT);
					plot.addAnnotation(annotation);
					index++;
		}
		applyAxisOptions(chart);
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
//	public static JFreeChart createQQChart(Double position, List<AnalysisDataset> list, ProfileType type) throws Exception {
//
//		JFreeChart chart = 
//				ChartFactory.createXYLineChart(null,
//						"Angle", "Probability", null, PlotOrientation.VERTICAL, true, true,
//						false);
//
//		XYPlot plot = chart.getXYPlot();
//		
//		int datasetCount = 0;
//		int iteration = 0;
//		for(IAnalysisDataset dataset : list){
//			
////			XYDataset ds 	 = NucleusDatasetCreator.getInstance().createModalityProbabililtyDataset(position, dataset, type);
////			XYDataset values = NucleusDatasetCreator.getInstance().createModalityValuesDataset(position, dataset, type);
//			ICellCollection collection = dataset.getCollection();
//
//			double[] values = collection.getProfileCollection().getValuesAtPosition(position);
//			XYDataset ds =  new NucleusDatasetCreator().createQQDataset(values);
//			
//			Color colour = dataset.getDatasetColour() == null 
//					? ColourSelecter.getColor(iteration)
//					: dataset.getDatasetColour();
//
//			plot.setDataset(datasetCount, ds);
//			
//			XYLineAndShapeRenderer lineRenderer =  new XYLineAndShapeRenderer(false, true);
//			plot.setRenderer(datasetCount,lineRenderer);
//			int seriesCount = plot.getDataset(datasetCount).getSeriesCount();
//			for(int i=0; i<seriesCount;i++){
//				
//				lineRenderer.setSeriesPaint(i, colour);
//				lineRenderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
//				lineRenderer.setSeriesVisibleInLegend(i, false);
//			}
//			
//			datasetCount++;
//			iteration++;
//			
//		}
//		return chart;
//	}
	
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
	public JFreeChart makeKruskalWallisChart(boolean frankenNormalise){
		
		XYDataset kruskalDataset = null;

		NucleusDatasetCreator creator =  new NucleusDatasetCreator();
		
		XYDataset firstProfileDataset;
		XYDataset secondProfileDataset;
		try {
			if(frankenNormalise){

				kruskalDataset = creator.createFrankenKruskalProfileDataset(options);

			} else {
				kruskalDataset = creator.createKruskalProfileDataset(options);
			}

			firstProfileDataset = creator.createNonsegmentedMedianProfileDataset(options.firstDataset(),
					true,
					options.getAlignment(),
					options.getTag());

			secondProfileDataset = creator.createNonsegmentedMedianProfileDataset(options.getDatasets().get(1),
					true,
					options.getAlignment(),
					options.getTag());

		} catch (ChartDatasetCreationException e) {
			return makeErrorChart();
		}

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
		Paint colorOne = options.getDatasets().get(0).getDatasetColour() == null 
					? ColourSelecter.getColor(0) 
					: options.getDatasets().get(0).getDatasetColour();
		angleRendererOne.setSeriesPaint(0, colorOne);
		angleRendererOne.setSeriesVisibleInLegend(0, false);
		angleRendererOne.setSeriesStroke(0, ChartComponents.MARKER_STROKE);
		
		XYItemRenderer angleRendererTwo = new XYLineAndShapeRenderer(true, false);
		Paint colorTwo = options.getDatasets().get(1).getDatasetColour() == null 
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
	public static JFreeChart createBooleanProfileChart(IProfile p, BooleanProfile limits){
		
		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator().createBooleanProfileDataset(p, limits);
		} catch (ChartDatasetCreationException e) {
			return makeErrorChart();
		}
		
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
