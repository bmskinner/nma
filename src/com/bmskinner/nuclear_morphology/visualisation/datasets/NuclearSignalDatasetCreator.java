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
package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.components.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.AngleTools;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

public class NuclearSignalDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	private static final Logger LOGGER = Logger.getLogger(NuclearSignalDatasetCreator.class.getName());

	public NuclearSignalDatasetCreator(@NonNull final ChartOptions o) {
		super(o);
	}

	/**
	 * Get the XY coordinates of a given signal centre of mass on a nuclear outline
	 * 
	 * @param n       the signal to plot
	 * @param outline the nucleus outline to draw the signal on
	 * @return the point of the signal centre of mass
	 * @throws ChartDatasetCreationException
	 */
	public IPoint getXYCoordinatesForSignal(@NonNull INuclearSignal n, @NonNull Nucleus outline) {

		// the clockwise angle from the below the CoM, through the CoM, to the signal
		// CoM
		double angle = n.getMeasurement(Measurement.ANGLE);
		double fractionalDistance = n.getMeasurement(Measurement.FRACT_DISTANCE_FROM_COM);

		// determine the distance to the border at this angle
		double distanceToBorder = ComponentMeasurer.getDistanceFromCoMToBorderAtAngle(outline, angle);

		// convert to fractional distance to signal
		double distanceFromCoM = distanceToBorder * fractionalDistance;

		// adjust X and Y because we are counting angles from the vertical axis
		// but the angle tools returns angles against the x-axis
		double signalX = AngleTools.getXComponentOfAngle(distanceFromCoM, angle - 90);
		double signalY = AngleTools.getYComponentOfAngle(distanceFromCoM, angle - 90);
		return new FloatPoint(signalX, signalY);
	}

	/**
	 * Create a chart dataset for the centres of mass of signals in the dataset
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createSignalCoMDataset() throws ChartDatasetCreationException {

		NuclearSignalXYDataset ds = new NuclearSignalXYDataset();
		ICellCollection collection = options.firstDataset().getCollection();

		if (collection.getSignalManager().hasSignals()) {
			for (UUID uuid : collection.getSignalManager().getSignalGroupIDs()) {

				if (options.firstDataset().getCollection().getSignalGroup(uuid).get().isVisible()) {

					List<INuclearSignal> signals = collection.getSignalManager().getSignals(uuid);
					double[] xpoints = new double[signals.size()];
					double[] ypoints = new double[signals.size()];

					int signalCount = 0;

					List<INuclearSignal> signalList = new ArrayList<>();
					List<Nucleus> nucleusList = new ArrayList<>();
					for (Nucleus n : collection.getNuclei()) {
						if (n.getSignalCollection().hasSignal(uuid)) {
							for (INuclearSignal s : n.getSignalCollection().getSignals(uuid)) {
								signalList.add(s);
								nucleusList.add(n);
							}
						}
					}

					try {
						Nucleus consensus = collection.getConsensus();

						for (INuclearSignal n : signals) {

							IPoint p = getXYCoordinatesForSignal(n, consensus);

							xpoints[signalCount] = p.getX();
							ypoints[signalCount] = p.getY();
							signalCount++;

						}
						double[][] data = { xpoints, ypoints };

						ds.addSeries(CellularComponent.NUCLEAR_SIGNAL + "_" + uuid, data, signalList, nucleusList);
					} catch (MissingLandmarkException | ComponentCreationException e) {
						throw new ChartDatasetCreationException(e);
					}
				}
			}
		}
		return ds;
	}

	public List<Shape> createSignalRadiusDataset(@NonNull IAnalysisDataset dataset, @NonNull UUID signalGroup)
			throws ChartDatasetCreationException {

		ICellCollection collection = dataset.getCollection();
		List<Shape> result = new ArrayList<>();
		if (!collection.getSignalManager().hasSignals(signalGroup))
			return result;

		if (collection.getSignalGroup(signalGroup).get().isVisible()) {
			try {
				Nucleus consensus = collection.getConsensus();
				for (INuclearSignal n : collection.getSignalManager().getSignals(signalGroup)) {
					IPoint p = getXYCoordinatesForSignal(n, consensus);

					// ellipses are drawn starting from x y at upper left.
					// Provide an offset from the centre
					double offset = n.getMeasurement(Measurement.RADIUS);

					result.add(new Ellipse2D.Double(p.getX() - offset, p.getY() - offset, offset * 2, offset * 2));
				}
			} catch (MissingLandmarkException | ComponentCreationException e) {
				throw new ChartDatasetCreationException(e);
			}

		}
		return result;
	}

	/**
	 * Create a boxplot dataset for signal statistics
	 * 
	 * @return a boxplot dataset
	 * @throws Exception
	 */
	public NuclearSignalBoxAndWhiskerDataset createSignalStatisticBoxplotDataset() {
		return createMultiDatasetSignalStatisticBoxplotDataset();
	}

	/**
	 * Create a boxplot dataset for signal statistics for a single analysis dataset
	 * 
	 * @return a boxplot dataset
	 * @throws ChartDatasetCreationException
	 */
	private NuclearSignalBoxAndWhiskerDataset createMultiDatasetSignalStatisticBoxplotDataset() {

		NuclearSignalBoxAndWhiskerDataset result = new NuclearSignalBoxAndWhiskerDataset();
		Measurement stat = options.getMeasurement();

		for (IAnalysisDataset d : options.getDatasets()) {

			ICellCollection collection = d.getCollection();

			for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

				double[] values = collection.getSignalManager().getSignalStatistics(stat, options.getScale(),
						signalGroup);
				/*
				 * For charting, use offset angles, otherwise the boxplots will fail on wrapped
				 * signals
				 */
				if (stat.equals(Measurement.ANGLE)) {
					values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);
				}

				List<Double> list = new ArrayList<>();
				for (double value : values) {
					list.add(value);
				}

				result.add(signalGroup, list, CellularComponent.NUCLEAR_SIGNAL, collection.getName());
			}
		}
		return result;
	}

	/**
	 * Create a list of shell result datasets for each analysis dataset in the given
	 * options
	 * 
	 * @param options
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public List<CategoryDataset> createShellBarChartDataset() {

		List<CategoryDataset> result = new ArrayList<>();

		for (IAnalysisDataset dataset : options.getDatasets()) {

			ShellResultDataset ds = new ShellResultDataset();

			ICellCollection collection = dataset.getCollection();

			if (collection.hasSignalGroup(IShellResult.RANDOM_SIGNAL_ID)) {
				if (options.isShowAnnotations())
					addRandomShellData(ds, collection);
			}

			for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {
				if (collection.getSignalGroup(signalGroup).get().isVisible())
					addRealShellData(ds, collection, signalGroup);
			}
			result.add(ds);
		}
		return result;
	}

	/**
	 * Show shell results as a heatmap of shell on the x versus dataset on the y
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public XYZDataset createMultipleDatasetShellHeatMapDataset() {

		DefaultXYZDataset result = new DefaultXYZDataset();

		int yValue = 0;
		for (IAnalysisDataset dataset : options.getDatasets()) {

			ICellCollection collection = dataset.getCollection();

			for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {
				if (collection.getSignalGroup(signalGroup).get().isVisible()) {

					Aggregation agg = options.getAggregation();
					Normalisation norm = options.getNormalisation();

					Optional<ISignalGroup> g = collection.getSignalGroup(signalGroup);
					if (!g.isPresent())
						continue;
					Optional<IShellResult> r = g.get().getShellResult();
					if (!r.isPresent())
						continue;
					IShellResult shellResult = r.get();

					double[] zVals = shellResult.getProportions(agg, norm);
					double[] yVals = new double[zVals.length];
					Arrays.fill(yVals, yValue);
					double[] xVals = new double[zVals.length];
					for (int shell = 0; shell < shellResult.getNumberOfShells(); shell++) {
						xVals[shell] = shell;
					}

					double[][] data = { xVals, yVals, zVals };

					String series = g.get().getGroupName() + " in " + collection.getName() + "_Series_" + yValue;

					result.addSeries(series, data);
					yValue++;
				}

			}

		}
		return result;
	}

	/**
	 * Create a consensus nucleus dataset overlaid with shells. Requires a single
	 * dataset in the options.
	 * 
	 * @param options the options
	 * @return a chart dataset
	 * @throws ChartDatasetCreationException if the IAnalysisDataset has no shell
	 *                                       results or the dataset count is not 1
	 */
	public XYDataset createShellConsensusDataset() throws ChartDatasetCreationException {

		if (!options.isSingleDataset()) {
			throw new ChartDatasetCreationException("Single dataset required");
		}

		DefaultXYDataset ds = new DefaultXYDataset();

		// Make the shells from the consensus nucleus
		ShellDetector c;
		try {

			int shellCount = options.firstDataset().getCollection().getSignalManager().getShellCount();

			if (shellCount == 0) {
				throw new ChartDatasetCreationException("Cannot make dataset for zero shells");
			}

			ShrinkType type = options.firstDataset().getCollection().getSignalManager().getShrinkType()
					.orElse(ShrinkType.AREA);

			c = new ShellDetector(options.firstDataset().getCollection().getConsensus(), shellCount, type);

		} catch (ShellAnalysisException | MissingLandmarkException | ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Error making shells in consensus", e);
			throw new ChartDatasetCreationException("Error making shells", e);
		}

		// Draw the shells
		int shellNumber = 0;
		for (Shell shell : c.getShells()) {

			Polygon p = shell.toPolygon();

			double[] xpoints = new double[p.npoints + 1];
			double[] ypoints = new double[p.npoints + 1];

			for (int i = 0; i < p.npoints; i++) {

				xpoints[i] = p.xpoints[i];
				ypoints[i] = p.ypoints[i];
			}
			// complete the line
			xpoints[p.npoints] = xpoints[0];
			ypoints[p.npoints] = ypoints[0];

			double[][] data = { xpoints, ypoints };
			ds.addSeries("Shell_" + shellNumber, data);
			shellNumber++;

		}

		return ds;

	}

	/**
	 * Add the simulated random data from the given collection to the result dataset
	 * 
	 * @param ds         the dataset to add values to
	 * @param collection the cell collection to take random shell data from
	 * @param options    the chart options
	 */
	private void addRandomShellData(@NonNull final ShellResultDataset ds, @NonNull final ICellCollection collection) {

		UUID signalGroup = IShellResult.RANDOM_SIGNAL_ID;

		Aggregation agg = options.getAggregation();
		Normalisation norm = options.getNormalisation();
		Optional<ISignalGroup> g = collection.getSignalGroup(signalGroup);

		if (!g.isPresent())
			return;

		Optional<IShellResult> r = g.get().getShellResult();

		if (!r.isPresent())
			return;

		if (options.getNormalisation().equals(Normalisation.DAPI)) {

			for (int shell = 0; shell < r.get().getNumberOfShells(); shell++) {
				double d = -100d / r.get().getNumberOfShells();

				ds.add(signalGroup, d, 0, "Group_" + signalGroup + "_" + collection.getName(), String.valueOf(shell));
			}
			return;
		}

		// otherwise use raw counts
		double[] arr = r.get().getProportions(agg, norm);
		for (int shell = 0; shell < r.get().getNumberOfShells(); shell++) {
			double d = -arr[shell] * 100;

			ds.add(signalGroup, d, 0, "Group_" + signalGroup + "_" + collection.getName(), String.valueOf(shell));
		}
	}

	/**
	 * Add the real shell data from the given collection to the result dataset
	 * 
	 * @param ds         the dataset to add values to
	 * @param collection the cell collection to take shell data from
	 * @param options    the chart options
	 */
	private void addRealShellData(@NonNull final ShellResultDataset ds, @NonNull final ICellCollection collection,
			@NonNull final UUID signalGroup) {

		Aggregation agg = options.getAggregation();
		Normalisation norm = options.getNormalisation();

		Optional<ISignalGroup> g = collection.getSignalGroup(signalGroup);

		if (!g.isPresent())
			return;

		Optional<IShellResult> r = g.get().getShellResult();

		if (!r.isPresent())
			return;

		double[] arr = r.get().getProportions(agg, norm);
		for (int shell = 0; shell < r.get().getNumberOfShells(); shell++) {
			double d = arr[shell] * 100;

			ds.add(signalGroup, d, 0, "Group_" + g.get().getGroupName() + "_" + collection.getName(),
					String.valueOf(shell));

		}
	}

}
