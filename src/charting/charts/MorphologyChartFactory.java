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
import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import utility.Utils;
import weka.core.logging.Logger;
import analysis.AnalysisDataset;
import charting.ChartComponents;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.TailDatasetCreator;
import components.Cell;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileCollection;
import components.generic.ProfileCollectionType;
import components.nuclei.Nucleus;

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
	 * Make a profle chart for a nucleus, and annotate the border points
	 * @param ds
	 * @param n
	 * @return
	 */
	public static JFreeChart makeIndividualNucleusProfileChart(XYDataset ds, Nucleus n, ColourSwatch swatch){
		JFreeChart chart = makeProfileChart(ds, n.getLength(), swatch);

		XYPlot plot = chart.getXYPlot();

		for(BorderTag tag : n.getBorderTags().keySet()){
			Color colour = Color.BLACK;
			int index = Utils.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getLength());

			if(tag.equals(BorderTag.ORIENTATION_POINT)){
				colour = Color.BLUE;
			}
			if(tag.equals(BorderTag.REFERENCE_POINT)){
				colour = Color.ORANGE;
			}
			plot.addDomainMarker(new ValueMarker(index, colour, ChartComponents.MARKER_STROKE));	
		}
		return chart;
	}
	
	public static JFreeChart makeSingleProfileChart(ProfileChartOptions options) throws Exception {
		return makeSingleProfileChart(
				options.getDatasets().get(0),
				options.isNormalised(),
				options.getAlignment(),
				options.getTag(),
				options.isShowMarkers());
	}
	
	/**
	 * Create a segmented profile chart from a given XYDataset. Set the series 
	 * colours for each component. Draw lines on the offset indexes
	 * @param dataset the dataset the values come from
	 * @param normalised should the scales be normalised
	 * @param rightAligm should the chart be aligned to the right
	 * @return a chart
	 */
	public static JFreeChart makeSingleProfileChart(AnalysisDataset dataset, boolean normalised, ProfileAlignment alignment, BorderTag borderTag, boolean showMarkers) throws Exception {
		
		CellCollection collection = dataset.getCollection();
		XYDataset ds = NucleusDatasetCreator.createSegmentedProfileDataset(collection, normalised, alignment, borderTag);
		
		
		int length = 100 ; // default if normalised

		
		// if we set raw values, get the maximum nucleus length
		if(!normalised){
			length = (int) collection.getMaxProfileLength();
		}
		
		ColourSwatch swatch = dataset.getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : dataset.getSwatch();
		JFreeChart chart = makeProfileChart(ds, length, swatch);
		
		// mark the reference and orientation points
		
		XYPlot plot = chart.getXYPlot();

		for (BorderTag tag : collection.getProfileCollection(ProfileCollectionType.REGULAR).getOffsetKeys()){
			Color colour = Color.BLACK;
			
			// get the index of the tag
			int index = collection.getProfileCollection(ProfileCollectionType.REGULAR).getOffset(tag);
			
			// get the offset from to the current draw point
			int offset = collection.getProfileCollection(ProfileCollectionType.REGULAR).getOffset(borderTag);
			
			// adjust the index to the offset
			index = Utils.wrapIndex( index - offset, collection.getProfileCollection(ProfileCollectionType.REGULAR).getAggregate().length());
			
			double indexToDraw = index; // convert to a double to allow normalised positioning
			
			if(normalised){ // set to the proportion of the point along the profile
				indexToDraw =  (( indexToDraw / collection.getProfileCollection(ProfileCollectionType.REGULAR).getAggregate().length() ) * 100);
			}
			if(alignment.equals(ProfileAlignment.RIGHT) && !normalised){
				int maxX = DatasetUtilities.findMaximumDomainValue(ds).intValue();
				int amountToAdd = maxX - collection.getProfileCollection(ProfileCollectionType.REGULAR).getAggregate().length();
				indexToDraw += amountToAdd;
				
			}

			if(showMarkers){
				if(tag.equals(BorderTag.ORIENTATION_POINT)){
					colour = Color.BLUE;
				}
				if(tag.equals(BorderTag.REFERENCE_POINT)){
					colour = Color.ORANGE;
				}
				plot.addDomainMarker(new ValueMarker(indexToDraw, colour, ChartComponents.MARKER_STROKE));	
			}
			
		}
		return chart;
	}
	
	public static JFreeChart makeMultiSegmentedProfileChart(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag, boolean showMarkers) throws Exception {
		
		int length = 100 ; // default if normalised
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
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			String point = collection.getPoint(borderTag);
			XYDataset ds = NucleusDatasetCreator.createSegmentedMedianProfileDataset(dataset, normalised, alignment, borderTag);

			plot.setDataset(datasetIndex, ds);
//			IJ.log("Set dataset "+datasetIndex + " for "+dataset.getName());
			
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
					ColourSwatch swatch = dataset.getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : dataset.getSwatch();
					renderer.setSeriesPaint(i, swatch.color(colourIndex));
				} 
			}	

			datasetIndex++;
		}
		return chart;
	}
	
	
	public static JFreeChart makeFrankenProfileChart(ProfileChartOptions options) throws Exception {
		return makeFrankenProfileChart(
				options.getDatasets().get(0),
				options.isNormalised(),
				options.getAlignment(),
				options.getTag(),
				options.isShowMarkers());
	}
	
	/**
	 * Create a segmented profile chart from a given XYDataset. Set the series 
	 * colours for each component. Draw lines on the offset indexes
	 * @param dataset the dataset the values come from
	 * @param normalised should the scales be normalised
	 * @param rightAligm should the chart be aligned to the right
	 * @return a chart
	 */
	public static JFreeChart makeFrankenProfileChart(AnalysisDataset dataset, boolean normalised, ProfileAlignment alignment, BorderTag borderTag, boolean showMarkers) throws Exception {
		
		CellCollection collection = dataset.getCollection();
		XYDataset ds = NucleusDatasetCreator.createFrankenSegmentDataset(dataset.getCollection(), normalised, alignment, borderTag);

		ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.FRANKEN);
		
		int length = 100 ; // default if normalised - for a franken collection, it makes no difference

		ColourSwatch swatch = dataset.getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : dataset.getSwatch();
		JFreeChart chart = makeProfileChart(ds, length, swatch);
		
		// mark the reference andorientation points
		
		XYPlot plot = chart.getXYPlot();

		for (BorderTag tag : pc.getOffsetKeys()){
			Color colour = Color.BLACK;
			
			// get the index of the tag
			int index = pc.getOffset(tag);
			
			// get the offset from to the current draw point
			int offset = pc.getOffset(borderTag);
			
			// adjust the index to the offset
			index = Utils.wrapIndex( index - offset, pc.getAggregate().length());
			
			double indexToDraw = index; // convert to a double to allow normalised positioning
			
//			if(normalised){ // set to the proportion of the point along the profile
				indexToDraw =  (( indexToDraw / pc.getAggregate().length() ) * 100);
//			}
			if(alignment.equals(ProfileAlignment.RIGHT) && !normalised){
				int maxX = DatasetUtilities.findMaximumDomainValue(ds).intValue();
				int amountToAdd = maxX - pc.getAggregate().length();
				indexToDraw += amountToAdd;
				
			}

			if(showMarkers){
				if(tag.equals(BorderTag.ORIENTATION_POINT)){
					colour = Color.BLUE;
				}
				if(tag.equals(BorderTag.REFERENCE_POINT)){
					colour = Color.ORANGE;
				}
				plot.addDomainMarker(new ValueMarker(indexToDraw, colour, ChartComponents.MARKER_STROKE));	
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
	public static JFreeChart makeProfileChart(XYDataset ds, int xLength, ColourSwatch swatch){
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		try {
		
		
		XYPlot plot = chart.getXYPlot();
		
		// the default is to use an x range of 100, for a normalised chart
		plot.getDomainAxis().setRange(0,xLength);
		
		// always set the y range to 360 degrees
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		
		// the 180 degree line
		plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);

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
		} catch (Exception e){
			IJ.log("Error creating profile chart:"+e.getMessage() );
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
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
	public static JFreeChart makeMultiProfileChart(ProfileChartOptions options)  throws Exception{
		
		List<XYSeriesCollection> iqrProfiles = null;
		XYDataset medianProfiles			 = null;
		if(options.getType().equals(ProfileCollectionType.REGULAR)){
			iqrProfiles     = NucleusDatasetCreator.createMultiProfileIQRDataset( options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());				
			medianProfiles	= NucleusDatasetCreator.createMultiProfileDataset(	  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());
		}
		
		if(options.getType().equals(ProfileCollectionType.FRANKEN)){
			iqrProfiles     = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset(  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());				
			medianProfiles	= NucleusDatasetCreator.createMultiProfileFrankenDataset(	  options.getDatasets(), options.isNormalised(), options.getAlignment(), options.getTag());
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
	
	/**
	 * Create a variabillity chart showing the IQR for a single dataset. Segment colours
	 * are applied. 
	 * @param list the dataset
	 * @param xLength the length of the plot
	 * @return a chart
	 */
	public static JFreeChart makeSingleVariabilityChart(List<AnalysisDataset> list, int xLength, BorderTag tag, ProfileCollectionType type) throws Exception {
		XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(list, tag, type);
		CellCollection n = list.get(0).getCollection();
		ColourSwatch swatch = list.get(0).getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : list.get(0).getSwatch();
		JFreeChart chart = MorphologyChartFactory.makeProfileChart(ds, xLength, swatch);
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
	public static JFreeChart makeMultiVariabilityChart(List<AnalysisDataset> list, int xLength, BorderTag tag, ProfileCollectionType type) throws Exception {
		XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(list, tag, type);
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "IQR", ds, PlotOrientation.VERTICAL, true, true,
				                false);

		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,xLength);
		plot.getRangeAxis().setAutoRange(true);
		plot.setBackgroundPaint(Color.WHITE);

		for (int j = 0; j < ds.getSeriesCount(); j++) {
			plot.getRenderer().setSeriesVisibleInLegend(j, false);
			plot.getRenderer().setSeriesStroke(j, ChartComponents.QUARTILE_STROKE);
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
	 * Create a nucleus outline chart with nuclear signals drawn as transparent
	 * circles
	 * @param dataset the AnalysisDataset to use to draw the consensus nucleus
	 * @param signalCoMs the dataset with the signal centre of masses
	 * @return
	 */
	public static JFreeChart makeSignalCoMNucleusOutlineChart(AnalysisDataset dataset, XYDataset signalCoMs){
		JFreeChart chart = ConsensusNucleusChartFactory.makeNucleusOutlineChart(dataset);

		XYPlot plot = chart.getXYPlot();
		plot.setDataset(1, signalCoMs);

		XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
		for(int series=0;series<signalCoMs.getSeriesCount();series++){

			Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
			rend.setSeriesShape(series, circle);

			String name = (String) signalCoMs.getSeriesKey(series);
			int seriesGroup = getIndexFromLabel(name);
			Color colour = dataset.getSignalGroupColour(seriesGroup);
			rend.setSeriesPaint(series, colour);
			rend.setBaseLinesVisible(false);
			rend.setBaseShapesVisible(true);
			rend.setBaseSeriesVisibleInLegend(false);
		}
		plot.setRenderer(1, rend);

		for(int signalGroup : dataset.getCollection().getSignalGroups()){
			List<Shape> shapes = NuclearSignalDatasetCreator.createSignalRadiusDataset(dataset, signalGroup);

			int signalCount = shapes.size();

			int alpha = (int) Math.floor( 255 / ((double) signalCount) )+20;
			alpha = alpha < 10 ? 10 : alpha > 156 ? 156 : alpha;

			Color colour = dataset.getSignalGroupColour(signalGroup);

			for(Shape s : shapes){
				XYShapeAnnotation an = new XYShapeAnnotation( s, null,
						null, ColourSelecter.getTransparentColour(colour, true, alpha)); // layer transparent signals
				plot.addAnnotation(an);
			}
		}
		return chart;
	}

	/**
	 * Get a chart contaning the details of the given cell from the given dataset
	 * @param cell the cell to draw
	 * @param dataset the dataset the cell came from
	 * @return
	 * @throws Exception 
	 */
	public static JFreeChart makeCellOutlineChart(Cell cell, AnalysisDataset dataset) throws Exception{
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(true);

		// make a hash to track the contents of each dataset produced
		Map<Integer, String> hash = new HashMap<Integer, String>(0); 
		Map<Integer, XYDataset> datasetHash = new HashMap<Integer, XYDataset>(0); 


		// get the nucleus dataset
		XYDataset nucleus = NucleusDatasetCreator.createNucleusOutline(cell, true);
		hash.put(hash.size(), "Nucleus"); // add to the first free entry
		datasetHash.put(datasetHash.size(), nucleus);
		
		// get the index tags
		XYDataset tags = NucleusDatasetCreator.createNucleusIndexTags(cell);
		hash.put(hash.size(), "Tags"); // add to the first free entry
		datasetHash.put(datasetHash.size(), tags);
		
		// get the signals datasets and add each group to the hash
		if(cell.getNucleus().hasSignal()){
			List<DefaultXYDataset> signalsDatasets = NucleusDatasetCreator.createSignalOutlines(cell, dataset);

			for(XYDataset d : signalsDatasets){

				String name = "default_0";
				for (int i = 0; i < d.getSeriesCount(); i++) {
					name = (String) d.getSeriesKey(i);	
				}
				int signalGroup = getIndexFromLabel(name);
				hash.put(hash.size(), "SignalGroup_"+signalGroup); // add to the first free entry	
				datasetHash.put(datasetHash.size(), d);
			}
		}

		// get tail datasets if present
		if(cell.hasTail()){

			XYDataset tailBorder = TailDatasetCreator.createTailOutline(cell);
			hash.put(hash.size(), "TailBorder");
			datasetHash.put(datasetHash.size(), tailBorder);
			XYDataset skeleton = TailDatasetCreator.createTailSkeleton(cell);
			hash.put(hash.size(), "TailSkeleton");
			datasetHash.put(datasetHash.size(), skeleton);
		}

		// set the rendering options for each dataset type

		for(int key : hash.keySet()){

			plot.setDataset(key, datasetHash.get(key));
			plot.setRenderer(key, new XYLineAndShapeRenderer(true, false));

			int seriesCount = plot.getDataset(key).getSeriesCount();
			// go through each series in the dataset
			for(int i=0; i<seriesCount;i++){

				// all datasets use the same stroke
				plot.getRenderer(key).setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer(key).setSeriesVisibleInLegend(i, false);

				// Basic nucleus colour
				if(hash.get(key).equals("Nucleus")){
					String name = (String) plot.getDataset(key).getSeriesKey(i);
					int colourIndex = getIndexFromLabel(name);
					
					plot.getRenderer().setSeriesPaint(i, dataset.getSwatch().color(colourIndex));
//					plot.getRenderer().setSeriesPaint(i, ColourSelecter.getOptimisedColor(colourIndex));
				}
				
				
				if(hash.get(key).equals("Tags")){
					plot.getRenderer(key).setSeriesPaint(i, Color.BLACK);
					String name = plot.getDataset(key).getSeriesKey(i).toString().replace("Tag_", "");
					
					if(name.equals(BorderTag.ORIENTATION_POINT.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.BLUE);
					}
					if(name.equals(BorderTag.REFERENCE_POINT.toString())){
						plot.getRenderer(key).setSeriesPaint(i, Color.ORANGE);
					}
						
				}

				// signal colours
				if(hash.get(key).startsWith("SignalGroup_")){
					int colourIndex = getIndexFromLabel(hash.get(key));
					Color colour = dataset.getSignalGroupColour(colourIndex);
					plot.getRenderer(key).setSeriesPaint(i, colour);
				}

				// tail border
				if(hash.get(key).equals("TailBorder")){

					plot.getRenderer(key).setSeriesPaint(i, Color.GREEN);
				}


				// tail skeleton
				if(hash.get(key).equals("TailSkeleton")){

					plot.getRenderer(key).setSeriesPaint(i, Color.BLACK);
				}
			}

		}
		return chart;
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
	public static JFreeChart createModalityChart(Double position, List<AnalysisDataset> list, ProfileCollectionType type) throws Exception {
		
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						"Angle", "Probability", null, PlotOrientation.VERTICAL, true, true,
						false);
		
		XYPlot plot = chart.getXYPlot();
		
		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 360);

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
	
	/**
	 * Create a QQ plot for the given datasets, at the given positon along a profile
	 * @param position
	 * @param list
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart createQQChart(Double position, List<AnalysisDataset> list, ProfileCollectionType type) throws Exception {

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

}
