/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.TextAnchor;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.stats.DipTester;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Create profile charts. The majority of methods are private, preferring explicit options
 * to have been set in the ChartOptions provided.
 * @author bms41
 * @since 1.14.0
 *
 */
public class ProfileChartFactory extends AbstractChartFactory {

	public ProfileChartFactory(ChartOptions o) {
		super(o);
	}

	/**
	 * Create an empty chart
	 * 
	 * @return
	 */
	public synchronized static JFreeChart createEmptyChart() {
		return makeEmptyProfileChart(ProfileType.ANGLE);
	}


	/**
	 * Create an empty chart to display when no datasets are selected
	 * 
	 * @return
	 */
	public synchronized static JFreeChart makeEmptyChart(ProfileType type) {
		if(type==null)
			return createEmptyChart();
		return makeEmptyProfileChart(type);
	}

	/**
	 * Create an empty chart to display when no datasets are selected
	 * 
	 * @return a chart
	 */
	private synchronized static JFreeChart makeEmptyProfileChart(ProfileType type) {

		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();

		plot.getDomainAxis().setLabel("Position");
		plot.getDomainAxis().setRange(0, ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH);

		plot.getRangeAxis().setLabel(type.getLabel());

		if (type.getDimension().equals(StatisticDimension.ANGLE)) {
			plot.getRangeAxis().setRange(0, 360);
			plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
		}

		return chart;
	}

	/**
	 * Create a profile chart for the given options
	 * 
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public JFreeChart createProfileChart() {

		if (!options.hasDatasets())
			return makeEmptyProfileChart(options.getType());

		// A single cell is selected
		if(options.isSingleDataset() && options.getCell() != null)
			return makeIndividualNucleusProfileChart();
		
		if (options.isSingleDataset())
			return makeSingleDatasetProfileChart();

		if (options.isMultipleDatasets())
			return makeMultiDatasetProfileChart();
		return makeEmptyProfileChart(options.getType());
	}
	
	/**
	 * Create a profile chart for the given profile.
	 * @param profile
	 * @return
	 */
	public JFreeChart createProfileChart(@NonNull IProfile profile) {
		ProfileChartDataset ds;
		try {
			ds = new ProfileDatasetCreator(options).createProfileDataset(profile);
		} catch (ChartDatasetCreationException e) {
			fine("Error creating profile chart", e);
			return makeErrorChart();
		}

		JFreeChart chart = makeProfileChart(ds, profile.size());
		// Add segment name annotations

		if(profile instanceof ISegmentedProfile) {
			ISegmentedProfile segProfile = (ISegmentedProfile)profile;
			if (options.isShowAnnotations())
				addSegmentTextAnnotations(segProfile, chart.getXYPlot());
		}
		return chart;
	}
	
	private JFreeChart makeIndividualNucleusProfileChart() {
		Nucleus n = options.getCell().getNucleus();
		ProfileChartDataset ds;
		try {
			ds = new ProfileDatasetCreator(options).createProfileDataset(n);
		} catch (ChartDatasetCreationException e) {
			fine("Error creating profile chart", e);
			return makeErrorChart();
		}
		JFreeChart chart = makeProfileChart(ds, options.getCell().getNucleus().getBorderLength());
		
		// Add markers
		if (options.isShowMarkers())
			addBorderTagMarkers(n, chart.getXYPlot());
		
		// Add segment name annotations
		if (options.isShowAnnotations()) {
			finest("Adding segment annotations");
			try {
				ISegmentedProfile profile = n.getProfile(options.getType(), options.getTag());
				addSegmentTextAnnotations(profile, chart.getXYPlot());
			} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
				fine("Error adding segment annotations", e);
				return makeErrorChart();
			}
		}
		return chart;
	}

	/**
	 * Make a profile chart for a single nucleus. If the profile is segmented,
	 * the segments are drawn
	 * 
	 * @param options
	 * @return
	 */
	private JFreeChart makeProfileChart(@NonNull ProfileChartDataset ds) {

		JFreeChart chart = makeEmptyProfileChart(options.getType());

		
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
			renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);

			String name = ds.getLines().getSeriesKey(i).toString();

			// segments along the median profile
			if (name.startsWith(ProfileDatasetCreator.SEGMENT_SERIES_PREFIX)) {
				int colourIndex = getIndexFromLabel(name);
				renderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);

				Paint colour = ColourSelecter.getColor(colourIndex);

				renderer.setSeriesPaint(i, colour);
				
			}
			
		}

		applyAxisOptions(chart);
		return chart;
	}

	/**
	 * Create a segmented profile chart from a given XYDataset. Set the series
	 * colours for each component. Draw lines on the offset indexes
	 * @return a chart
	 */
	private JFreeChart makeSingleDatasetProfileChart() {

		ProfileChartDataset ds = null;
		IAnalysisDataset dataset = options.firstDataset();
		ICellCollection collection = dataset.getCollection();

		try {
			if (options.getType().equals(ProfileType.FRANKEN)) {
				ds = new ProfileDatasetCreator(options).createProfileDataset(); //TODO: replace if needed
			} else {
				ds = new ProfileDatasetCreator(options).createProfileDataset();
			}
		} catch (ChartDatasetCreationException e) {
			stack("Error making profile dataset", e);
			return makeErrorChart();
		}

		
		int length = options.isShowProfiles() ? collection.getMaxProfileLength() : collection.getMedianArrayLength();
		length = options.isNormalised() ? ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH : length; // default if normalised

		JFreeChart chart = makeProfileChart(ds, length);

		// mark the reference and orientation points

		XYPlot plot = chart.getXYPlot();
		
		if (options.isShowMarkers()) {

			for (Tag tag : collection.getProfileCollection().getBorderTags()) {

				try {
					int index = collection.getProfileCollection().getIndex(tag);

					// get the offset from to the current draw point
					int offset = collection.getProfileCollection().getIndex(options.getTag());

					// adjust the index to the offset
					index = CellularComponent.wrapIndex(index - offset, collection.getProfileCollection().length());

					double indexToDraw = index; // convert to a double to allow normalised positioning

					if (options.isNormalised()) // set to the proportion of the point along the profile
						indexToDraw = ((indexToDraw / collection.getProfileCollection().length()) * ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH);

					if (options.getAlignment().equals(ProfileAlignment.RIGHT) && !options.isNormalised()) {
						int maxX = DatasetUtilities.findMaximumDomainValue(ds.getLines()).intValue();
						int amountToAdd = maxX - collection.getProfileCollection().length();
						indexToDraw += amountToAdd;
					}

					addMarkerToXYPlot(plot, tag, indexToDraw);

				} catch (UnavailableBorderTagException e) {
					fine("Tag not present in profile: " + tag);
				}

			}
		}
		
		// Add segment name annotations
		if (options.isShowAnnotations() && collection.getProfileCollection().hasSegments()) {
			try {
				ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(options.getType(), options.getTag(), Stats.MEDIAN);
				addSegmentTextAnnotations(profile, plot);
			} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
				fine("Error adding segment annotations", e);
				return makeErrorChart();
			}
		}
				
		applyAxisOptions(chart);
		return chart;
	}
	
	 /**
     * Create a profile chart for multiple datasets. Shows medians and iqrs for each
     * dataset.
     * 
     * @return a chart
     */
    private JFreeChart makeMultiDatasetProfileChart() {

    	ProfileChartDataset profiles;
    	try {
    		profiles = new ProfileDatasetCreator(options).createProfileDataset();
    	} catch (ChartDatasetCreationException e) {
    		fine("Unable to create profile dataset", e);
    		return makeErrorChart();
    	}
    	    	
    	// Set x-axis length
    	int xLength = ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH;
    	if (!options.isNormalised())	
    		xLength = options.getDatasets().stream()
    		.mapToInt(d->d.getCollection() .getMedianArrayLength())
    		.max().orElse(ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH);

		JFreeChart chart = makeProfileChart(profiles, xLength);
		applyAxisOptions(chart);
		return chart;
    }

	/**
	 * Create a profile chart from a given chart dataset. Set the series colours for
	 * each component
	 * 
	 * @param ds the chart dataset of profiles
	 * @param xLength the maximum of the x-axis
	 * @return a chart
	 */
	private JFreeChart makeProfileChart(@NonNull ProfileChartDataset ds, int xLength) {

		JFreeChart chart = makeEmptyProfileChart(options.getType());
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds.getLines()); // line charts are always in dataset 0

		for(int i=0; i<ds.getDatasetCount(); i++) { // IQR range charts are added above
			plot.setDataset(i+1, ds.getRanges(i));
		}
		
		plot.getRangeAxis().setAutoRange(false);
		plot.getDomainAxis().setRange(0, options.getType()==ProfileType.ANGLE?360:ds.maxRangeValue());
		
		plot.getDomainAxis().setAutoRange(false);
		// Start the x-axis at -1 so tags can be seen clearly
		plot.getDomainAxis().setRange(DEFAULT_PROFILE_START_INDEX, xLength);
		XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();

		lineRenderer.setBaseShapesVisible(options.isShowPoints());
		lineRenderer.setBaseLinesVisible(options.isShowLines());
		lineRenderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		lineRenderer.setBaseToolTipGenerator(null);
		plot.setRenderer(0, lineRenderer);

		// Format the line charts
		for (int i=0; i<ds.getLines().getSeriesCount(); i++) {
			lineRenderer.setSeriesVisibleInLegend(i, false);

			String name = ds.getLines().getSeriesKey(i).toString();
			int index   = ds.getLines().getDatasetIndex(name);
			
			lineRenderer.setSeriesStroke(i, chooseSeriesStroke(name));
			
			if (name.startsWith(ProfileDatasetCreator.SEGMENT_SERIES_PREFIX)) { // segments must be coloured separate to profiles
				int segIndex = findSegmentIndex(name);
				lineRenderer.setSeriesPaint(i,  chooseSeriesColour(name, segIndex, options.getSwatch()));
			} else {
				lineRenderer.setSeriesPaint(i,  chooseSeriesColour(name, index, options.getSwatch()));
			}
			
			lineRenderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
		}
		
		// Format the range charts
		for (int i = 0; i<ds.getDatasetCount(); i++) {
			// make a semi-transparent colour
			Paint profileColour = options.getDatasets().get(i).getDatasetColour().orElse(ColourSelecter.getColor(i, options.getSwatch()));
			Paint colour = ColourSelecter.getTransparentColour((Color) profileColour, true, 128);
			XYDifferenceRenderer rangeRenderer = new XYDifferenceRenderer(colour, colour, false);
			rangeRenderer.setBaseToolTipGenerator(null);
			plot.setRenderer(i+1, rangeRenderer);
			for (int series = 0; series<ds.getRanges(i).getSeriesCount(); series++) {
				rangeRenderer.setSeriesPaint(series, colour);
				rangeRenderer.setSeriesVisibleInLegend(series, false);

			}
		}		
		return chart;
	}
	
	private int findSegmentIndex(String name) {
		String regex = ProfileDatasetCreator.SEGMENT_SERIES_PREFIX+"(\\d+)_?.*";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(name);
		if(matcher.matches())
			return Integer.parseInt(matcher.group(1));
		return 0;
	}
	
	private Stroke chooseSeriesStroke(String name) {
		if (name.startsWith(ProfileDatasetCreator.SEGMENT_SERIES_PREFIX))
			return ChartComponents.SEGMENT_STROKE;
		if (name.startsWith(ProfileDatasetCreator.MEDIAN_SERIES_PREFIX))
			return ChartComponents.SEGMENT_STROKE;
		if (name.startsWith(ProfileDatasetCreator.NUCLEUS_SERIES_PREFIX))
			return ChartComponents.PROFILE_STROKE;
		if (name.startsWith(ProfileDatasetCreator.QUARTILE_SERIES_PREFIX))
			return ChartComponents.QUARTILE_STROKE;
		if (name.startsWith(ProfileDatasetCreator.PROFILE_SERIES_PREFIX))
			return ChartComponents.PROFILE_STROKE;
		return ChartComponents.PROFILE_STROKE;
	}
	
	private Color chooseSeriesColour(String name, int index, ColourSwatch swatch) {
		if (name.startsWith(ProfileDatasetCreator.SEGMENT_SERIES_PREFIX))
			return ColourSelecter.getColor(index, swatch);
		if (name.startsWith(ProfileDatasetCreator.MEDIAN_SERIES_PREFIX))
			return ColourSelecter.getColor(index, swatch);//.darker();
		if (name.startsWith(ProfileDatasetCreator.NUCLEUS_SERIES_PREFIX))
			return Color.LIGHT_GRAY;
		if (name.startsWith(ProfileDatasetCreator.QUARTILE_SERIES_PREFIX))
			return Color.DARK_GRAY;
		if (name.startsWith(ProfileDatasetCreator.PROFILE_SERIES_PREFIX))
			return Color.LIGHT_GRAY;
		return ColourSelecter.getColor(index, swatch);
	}

	/**
	 * Add markers for tag indexes
	 * @param n
	 * @param plot
	 */
	private void addBorderTagMarkers(@NonNull Taggable n, @NonNull XYPlot plot) {
		for (Tag tag : n.getBorderTags().keySet()) {
			try {
				// get the index of the tag
				int index = n.getBorderIndex(tag);

				// Correct to start from RP
				int offset = n.getBorderIndex(options.getTag());

				// adjust the index to the offset
				index = n.wrapIndex(index - offset);
				addMarkerToXYPlot(plot, tag, (double) index);

			} catch (UnavailableBorderTagException e) {
				stack("Border tag not available", e);
			}
		}
	}

	/**
	 * Add annotations of segment names from the given profile to the plot
	 * @param profile
	 * @param plot
	 */
	private void addSegmentTextAnnotations(ISegmentedProfile profile, XYPlot plot) {
		for (IBorderSegment seg : profile.getOrderedSegments()) {

			int midPoint = seg.getMidpointIndex();

			double x = midPoint;
			if (options.isNormalised())
				x = ((double) midPoint / (double) seg.getProfileLength()) * ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH;
			XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), x, 320);

			Paint colour = ColourSelecter.getColor(seg.getPosition());

			segmentAnnotation.setPaint(colour);
			plot.addAnnotation(segmentAnnotation);
		}
	}
	
	
    /**
     * Create a chart showing the variability of the profile; i.e
     * the size of the IQR at each point along the profile.
     * @return
     */
    public JFreeChart makeVariabilityChart() {

        if (!options.hasDatasets())
            return ProfileChartFactory.makeEmptyChart(options.getType());

        try {
            if (options.isSingleDataset())
                return makeSingleVariabilityChart();
            return makeMultiVariabilityChart();
        } catch (Exception e) {
            return makeErrorChart();
        }
    }

    /**
     * Create a variabillity chart showing the IQR for a single dataset. Segment
     * colours are applied.
     */
    private JFreeChart makeSingleVariabilityChart() {
    	ProfileChartDataset ds;
        try {
            ds = new ProfileDatasetCreator(options).createProfileVariabilityDataset();
        } catch (ChartDatasetCreationException e) {
            return makeErrorChart();
        }

        JFreeChart chart = makeProfileChart(ds, ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH);
        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setLabel("IQR");
        plot.getRangeAxis().setAutoRange(true);

        if (options.isShowMarkers()) { // add the bimodal regions
            ICellCollection collection = options.firstDataset().getCollection();

            // dip test the profiles

            double significance = options.getModalityPosition();
            BooleanProfile modes = new DipTester(collection).testCollectionIsUniModal(options.getTag(), significance,
                    options.getType());

            // add any regions with bimodal distribution to the chart
            float[] xPositions = new float[modes.size()];
            for (int i = 0; i < xPositions.length; i++) {
            	xPositions[i] = (float) i / (float) xPositions.length * ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH;
            }

            for (int i = 0; i < modes.size(); i++) {
                double x = xPositions[i];
                if (modes.get(i) == true) {
                    ValueMarker marker = new ValueMarker(x, Color.black, new BasicStroke(2f));
                    plot.addDomainMarker(marker);
                }
            }

            try {

                double ymax = DatasetUtilities.findMaximumRangeValue(plot.getDataset()).doubleValue();
                DecimalFormat df = new DecimalFormat("#0.000");
                XYTextAnnotation annotation = new XYTextAnnotation(
                        String.format("Markers for non-unimodal positions (p<%s)", df.format(significance)), 1, ymax);
                annotation.setTextAnchor(TextAnchor.TOP_LEFT);
                plot.addAnnotation(annotation);
            } catch (IllegalArgumentException ex) {
                fine("Missing data in variability chart");
            }
        }
        applyAxisOptions(chart);
        return chart;
    }

    /**
     * Create a variability chart showing the IQR for a multiple datasets.
     */
    private JFreeChart makeMultiVariabilityChart() throws Exception {
    	ProfileChartDataset ds = new ProfileDatasetCreator(options).createProfileVariabilityDataset();

    	JFreeChart chart = makeProfileChart(ds, ProfileDatasetCreator.DEFAULT_PROFILE_LENGTH);
        XYPlot plot = chart.getXYPlot();

        plot.getRangeAxis().setAutoRange(true);
        plot.getRangeAxis().setLabel("IQR");
        applyAxisOptions(chart);
        return chart;
    }
}
