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
package com.bmskinner.nma.visualisation.charts;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.MeasurementDimension;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nma.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.NucleusDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.ProfileDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Create profile charts. The majority of methods are private, preferring
 * explicit options to have been set in the ChartOptions provided.
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public class ProfileChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(ProfileChartFactory.class.getName());

	private static final String IQR_AXIS_LBL = "IQR";
	private static final String POSITION_AXIS_LBL = "Position";
	private static final double DEFAULT_POSITION_AXIS_MIN = 0;
	private static final double DEFAULT_ANGLE_AXIS_MIN = 0;
	private static final double DEFAULT_ANGLE_AXIS_MAX = 360;

	private static final int DEFAULT_EMPTY_PROFILE_LENGTH = 100;

	public ProfileChartFactory(@NonNull final ChartOptions o) {
		super(o);
	}

	/**
	 * Create an empty chart
	 * 
	 * @return
	 */
	public static synchronized JFreeChart createEmptyChart() {
		return createEmptyChart(ProfileType.ANGLE);
	}

	/**
	 * Create an empty chart to display when no datasets are selected
	 * 
	 * @return
	 */
	public static synchronized JFreeChart createEmptyChart(ProfileType type) {
		if (type == null)
			return createEmptyChart();

		JFreeChart chart = createBaseXYChart();
		XYPlot plot = chart.getXYPlot();

		plot.getDomainAxis().setLabel(POSITION_AXIS_LBL);
		plot.getDomainAxis().setRange(DEFAULT_POSITION_AXIS_MIN, DEFAULT_EMPTY_PROFILE_LENGTH);

		plot.getRangeAxis().setLabel(type.getLabel());

		if (type.getDimension().equals(MeasurementDimension.ANGLE)) {
			plot.getRangeAxis().setRange(DEFAULT_ANGLE_AXIS_MIN, DEFAULT_ANGLE_AXIS_MAX);
			plot.addRangeMarker(ChartComponents.DEGREE_LINE_180);
		}

		return chart;
	}

	/**
	 * Create a profile chart from the stored options
	 * 
	 * @return
	 */
	public JFreeChart createProfileChart() {

		if (!options.hasDatasets())
			return createEmptyChart(options.getType());

		// A single cell is selected
		if (options.isSingleDataset() && options.getCell() != null)
			return makeIndividualNucleusProfileChart();

		if (options.isSingleDataset())
			return makeSingleDatasetProfileChart();

		if (options.isMultipleDatasets())
			return makeMultiDatasetProfileChart();
		return createEmptyChart(options.getType());
	}

	/**
	 * Create a chart showing the effect of a boolean profile on a profile
	 * 
	 * @param p
	 * @param limits
	 * @return
	 */
	public JFreeChart createBooleanProfileChart(IProfile p, BooleanProfile limits) {

		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator(options).createBooleanProfileDataset(p, limits);
		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
		}

		JFreeChart chart = ChartFactory.createXYLineChart(null, POSITION_AXIS_LBL, "Angle", ds,
				PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = chart.getXYPlot();

		plot.setBackgroundPaint(Color.WHITE);

		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, ChartComponents.MARKER_STROKE));

		DefaultXYItemRenderer rend = new DefaultXYItemRenderer();
		rend.setDefaultShapesVisible(true);
		rend.setDefaultShape(ChartComponents.DEFAULT_POINT_SHAPE);
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

	private JFreeChart makeIndividualNucleusProfileChart() {
		Nucleus n = options.getCell().getPrimaryNucleus();
		ProfileChartDataset ds;
		try {
			ds = new ProfileDatasetCreator(options).createProfileDataset(n);
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error creating profile chart", e);
			return createErrorChart();
		}
		JFreeChart chart = makeProfileChart(ds,
				options.getCell().getPrimaryNucleus().getBorderLength());

		// Add markers
		if (options.isShowMarkers())
			addBorderTagMarkers(n, chart.getXYPlot());

		// Add segment name annotations
		if (options.isShowAnnotations()) {
			LOGGER.finest("Adding segment annotations");
			try {
				ISegmentedProfile profile = n.getProfile(options.getType(),
						options.getOrientationMark());
				addSegmentTextAnnotations(profile, chart.getXYPlot());
			} catch (MissingDataException | SegmentUpdateException e) {
				LOGGER.log(Loggable.STACK, "Error adding segment annotations", e);
				return createErrorChart();
			}
		}
		return chart;
	}

	/**
	 * Create a segmented profile chart from a given XYDataset. Set the series
	 * colours for each component. Draw lines on the offset indexes
	 * 
	 * @return a chart
	 */
	private JFreeChart makeSingleDatasetProfileChart() {

		ProfileChartDataset ds = null;
		IAnalysisDataset dataset = options.firstDataset();
		ICellCollection collection = dataset.getCollection();

		try {

			ds = new ProfileDatasetCreator(options).createProfileDataset();

		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error making profile dataset", e);
			return createErrorChart();
		}

		int length = options.isShowProfiles() ? collection.getMaxProfileLength()
				: collection.getMedianArrayLength();
		length = options.isNormalised() ? ds.getMaxDomainValue() : length; // default if normalised

		JFreeChart chart = makeProfileChart(ds, length);

		// mark the reference and orientation points

		XYPlot plot = chart.getXYPlot();
		try {
			if (options.isShowAnnotations()) {

				Landmark rp = dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new)
						.getRuleSetCollection().getLandmark(OrientationMark.REFERENCE)
						.orElseThrow(MissingLandmarkException::new);

				for (Landmark lm : collection.getProfileCollection().getLandmarks()) {

					// Skip RP, it's always at the start of the profile
					if (rp.equals(lm))
						continue;

					int index = collection.getProfileCollection().getLandmarkIndex(lm);

					double indexToDraw = index;

					if (options.isNormalised()) // set to the proportion of the point along the
												// profile
						indexToDraw = ((indexToDraw / collection.getMedianArrayLength())
								* ds.getMaxDomainValue());

					if (options.getAlignment().equals(ProfileAlignment.RIGHT)
							&& !options.isNormalised()) {
						int maxX = DatasetUtils.findMaximumDomainValue(ds.getLines()).intValue();
						int amountToAdd = maxX - collection.getMedianArrayLength();
						indexToDraw += amountToAdd;
					}

					double yVal = collection.getProfileCollection()
							.getProfile(options.getType(), OrientationMark.REFERENCE, Stats.MEDIAN)
							.get(index);

					addDomainMarkerToXYPlot(plot, lm, indexToDraw, yVal);

				}
			}
		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.fine("Landmark not present in profile");
		}
		// Add segment name annotations
		if (options.isShowAnnotations() && collection.getProfileCollection().hasSegments()) {
			try {
				ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(
						options.getType(),
						options.getOrientationMark(), Stats.MEDIAN);
				addSegmentTextAnnotations(profile, plot);
			} catch (MissingDataException | SegmentUpdateException e) {
				LOGGER.log(Loggable.STACK, "Error adding segment annotations", e);
				return createErrorChart();
			}
		}

		applyDefaultAxisOptions(chart);
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
			LOGGER.log(Loggable.STACK, "Unable to create profile dataset", e);
			return createErrorChart();
		}

		// Set x-axis length
		int xLength = profiles.getMaxDomainValue();
		if (!options.isNormalised())
			xLength = options.getDatasets().stream()
					.mapToInt(d -> d.getCollection().getMedianArrayLength()).max()
					.orElse(profiles.getMaxDomainValue());

		JFreeChart chart = makeProfileChart(profiles, xLength);
		applyDefaultAxisOptions(chart);
		return chart;
	}

	/**
	 * Create a profile chart from a given chart dataset. Set the series colours for
	 * each component
	 * 
	 * @param ds      the chart dataset of profiles
	 * @param xLength the maximum of the x-axis
	 * @return a chart
	 */
	protected JFreeChart makeProfileChart(@NonNull ProfileChartDataset ds, int xLength) {

		JFreeChart chart = createEmptyChart(options.getType());
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds.getLines()); // line charts are always in dataset 0

		for (int i = 0; i < ds.getDatasetCount(); i++) { // IQR range charts are added above
			plot.setDataset(i + 1, ds.getRanges(i));
		}

		plot.getRangeAxis().setAutoRange(false);
		plot.getRangeAxis().setRange(DEFAULT_ANGLE_AXIS_MIN,
				options.getType() == ProfileType.ANGLE ? DEFAULT_ANGLE_AXIS_MAX
						: ds.maxRangeValue());

		plot.getDomainAxis().setAutoRange(false);
		// Start the x-axis at -1 so tags can be seen clearly
		plot.getDomainAxis().setRange(DEFAULT_PROFILE_START_INDEX, xLength);
		XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();

		lineRenderer.setDefaultShapesVisible(options.isShowPoints());
		lineRenderer.setDefaultLinesVisible(options.isShowLines());
		lineRenderer.setDefaultShape(ChartComponents.DEFAULT_POINT_SHAPE);
		lineRenderer.setDefaultToolTipGenerator(null);
		plot.setRenderer(0, lineRenderer);

		// Format the line charts
		for (int i = 0; i < ds.getLines().getSeriesCount(); i++) {
			lineRenderer.setSeriesVisibleInLegend(i, false);

			String name = ds.getLines().getSeriesKey(i).toString();
			int index = ds.getLines().getDatasetIndex(name);

			lineRenderer.setSeriesStroke(i, chooseSeriesStroke(name));

			if (name.startsWith(ProfileDatasetCreator.SEGMENT_SERIES_PREFIX)) { // segments must be
																				// coloured separate
																				// to
																				// profiles
				int segIndex = findSegmentIndex(name);
				lineRenderer.setSeriesPaint(i,
						chooseSeriesColour(name, segIndex, options.getSwatch()));
			} else {
				lineRenderer.setSeriesPaint(i,
						chooseSeriesColour(name, index, options.getSwatch()));
			}

			lineRenderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
		}

		// Format the range charts
		for (int i = 0; i < ds.getDatasetCount(); i++) {
			// make a semi-transparent colour
			Paint profileColour = options.getDatasets().get(i).getDatasetColour()
					.orElse(ColourSelecter.getColor(i, options.getSwatch()));
			Paint colour = ColourSelecter.getTransparentColour((Color) profileColour, true, 128);
			XYDifferenceRenderer rangeRenderer = new XYDifferenceRenderer(colour, colour, false);
			rangeRenderer.setDefaultToolTipGenerator(null);
			plot.setRenderer(i + 1, rangeRenderer);
			for (int series = 0; series < ds.getRanges(i).getSeriesCount(); series++) {
				rangeRenderer.setSeriesPaint(series, colour);
				rangeRenderer.setSeriesVisibleInLegend(series, false);

			}
		}
		return chart;
	}

	private int findSegmentIndex(String name) {
		String regex = ProfileDatasetCreator.SEGMENT_SERIES_PREFIX + "(\\d+)_?.*";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(name);
		if (matcher.matches())
			return Integer.parseInt(matcher.group(1));
		return 0;
	}

	/**
	 * Choose a stroke based on the given series name. Defaults to
	 * {@link ChartComponents#PROFILE_STROKE}
	 * 
	 * @param name the series name
	 * @return the stroke
	 */
	private Stroke chooseSeriesStroke(final String name) {
		if (name == null)
			return ChartComponents.PROFILE_STROKE;
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

	/**
	 * Choose a colour based on the given series name. Defaults to
	 * {@link ColourSelecter#getColor(int, ColourSwatch)}
	 * 
	 * @param name   the series name
	 * @param index  the index of the series
	 * @param swatch the colour swatch to select from
	 * @return the colour
	 */
	private Color chooseSeriesColour(final String name, final int index,
			final ColourSwatch swatch) {
		if (name == null)
			return ColourSelecter.getColor(index, swatch);
		if (name.startsWith(ProfileDatasetCreator.SEGMENT_SERIES_PREFIX))
			return ColourSelecter.getColor(index, swatch);
		if (name.startsWith(ProfileDatasetCreator.MEDIAN_SERIES_PREFIX))
			return ColourSelecter.getColor(index, swatch);// .darker();
		if (name.startsWith(ProfileDatasetCreator.NUCLEUS_SERIES_PREFIX))
			return Color.LIGHT_GRAY;
		if (name.startsWith(ProfileDatasetCreator.QUARTILE_SERIES_PREFIX))
			return Color.DARK_GRAY;
		if (name.startsWith(ProfileDatasetCreator.PROFILE_SERIES_PREFIX))
			return Color.LIGHT_GRAY;
		return ColourSelecter.getColor(index, swatch);
	}

	/**
	 * Add domain markers for each tag in the taggable
	 * 
	 * @param n    the taggable object
	 * @param plot the plot to draw the domain markers on
	 */
	protected void addBorderTagMarkers(@NonNull Taggable n, @NonNull XYPlot plot) {
		for (OrientationMark tag : n.getOrientationMarkMap().keySet()) {
			try {
				// get the index of the tag
				int index = n.getBorderIndex(tag);

				// Correct to start from RP
				int offset = n.getBorderIndex(options.getOrientationMark());

				// adjust the index to the offset
				index = n.wrapIndex(index - offset);
				Landmark lm = n.getLandmark(tag);
				addDomainMarkerToXYPlot(plot, lm, index, 180);

			} catch (MissingLandmarkException e) {
				LOGGER.log(Loggable.STACK, "Border tag not available", e);
			}
		}
	}

	/**
	 * Add annotations of segment names from the given profile to the plot
	 * 
	 * @param profile
	 * @param plot
	 * @throws SegmentUpdateException
	 */
	protected void addSegmentTextAnnotations(ISegmentedProfile profile, XYPlot plot)
			throws SegmentUpdateException {
		double range = plot.getRangeAxis().getRange().getLength();
		double minY = plot.getRangeAxis().getRange().getLowerBound();
		double xMax = plot.getDomainAxis().getRange().getUpperBound();

		for (IProfileSegment seg : profile.getOrderedSegments()) {

			int midPoint = seg.getMidpointIndex();

			double x = midPoint;
			if (options.isNormalised())
				x = ((double) midPoint / (double) seg.getProfileLength()) * xMax;
			XYTextAnnotation segmentAnnotation = new XYTextAnnotation(seg.getName(), x,
					minY + (range * 0.9));

			Paint colour = ColourSelecter.getColor(seg.getPosition());

			segmentAnnotation.setPaint(colour);
			plot.addAnnotation(segmentAnnotation);
		}
	}

	/**
	 * Create a chart showing the variability of the profile; i.e the size of the
	 * IQR at each point along the profile.
	 * 
	 * @return
	 */
	public JFreeChart createVariabilityChart() {

		if (!options.hasDatasets())
			return ProfileChartFactory.createEmptyChart(options.getType());

		try {
			if (options.isSingleDataset())
				return makeSingleVariabilityChart();
			return makeMultiVariabilityChart();
		} catch (Exception e) {
			return createErrorChart();
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
			return createErrorChart();
		}

		JFreeChart chart = makeProfileChart(ds, ds.getMaxDomainValue());
		XYPlot plot = chart.getXYPlot();
		plot.getRangeAxis().setLabel(IQR_AXIS_LBL);
		plot.getRangeAxis().setAutoRange(true);
		applyDefaultAxisOptions(chart);
		return chart;
	}

	/**
	 * Create a variability chart showing the IQR for a multiple datasets.
	 * 
	 * @throws ChartDatasetCreationException
	 */
	private JFreeChart makeMultiVariabilityChart() throws ChartDatasetCreationException {
		ProfileChartDataset ds = new ProfileDatasetCreator(options)
				.createProfileVariabilityDataset();

		JFreeChart chart = makeProfileChart(ds, ds.getMaxDomainValue());
		XYPlot plot = chart.getXYPlot();

		plot.getRangeAxis().setAutoRange(true);
		plot.getRangeAxis().setLabel(IQR_AXIS_LBL);
		applyDefaultAxisOptions(chart);
		return chart;
	}
}
