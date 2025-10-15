package com.bmskinner.nma.visualisation.datasets;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.ComponentMeasurer;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.visualisation.datasets.AbstractDatasetCreator.SignalNameKey;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * An XY dataset mapping signals and nuclei to their XY coordinates
 * 
 * @author Ben Skinner
 *
 */
public class NuclearSignalXYDataset extends ComponentXYDataset<Nucleus> {

	private static final Logger LOGGER = Logger.getLogger(NuclearSignalXYDataset.class.getName());

	private final List<List<INuclearSignal>> signalList = new ArrayList<>();

	private final IAnalysisDataset d;
	private final ChartOptions options;

	public NuclearSignalXYDataset(@NonNull ChartOptions options)
			throws ChartDatasetCreationException {
		super();
		this.d = options.firstDataset();
		this.options = options;

		final ICellCollection collection = d.getCollection();

		if (!collection.getSignalManager().hasSignals())
			return;

		try {
			final Nucleus consensus = collection.getConsensus();


			for (final UUID uuid : collection.getSignalManager().getSignalGroupIDs()) {

				final ISignalGroup sg = d.getCollection().getSignalGroup(uuid).get();

				// Only display signals marked visible
				if (!sg.isVisible()) {
					continue;
				}

				final List<INuclearSignal> signalList = new ArrayList<>();
				final List<Nucleus> nucleusList = new ArrayList<>();

				// Identify the nuclei to be included, one-to-one mapped with signals
				for (final Nucleus n : collection.getNuclei()) {
					if (n.getSignalCollection().hasSignal(uuid)) {
						for (final INuclearSignal s : n.getSignalCollection().getSignals(uuid)) {
							signalList.add(s);
							nucleusList.add(n);
						}
					}
				}

				// Populate the data array with signal coordinates relative to consensus
				final List<INuclearSignal> signals = collection.getSignalManager()
						.getSignals(uuid);

				// Store coordinates for the signal location
				final double[] xpoints = new double[signals.size()];
				final double[] ypoints = new double[signals.size()];

				int signalCount = 0;

				for (final INuclearSignal n : signals) {

					final IPoint p = getXYCoordinatesForSignal(n, consensus);

					xpoints[signalCount] = p.getX();
					ypoints[signalCount] = p.getY();
					signalCount++;

				}
				final double[][] data = { xpoints, ypoints };

				final Comparable<?> seriesKey = new SignalNameKey(sg, uuid);
				addSeries(seriesKey, data, signalList, nucleusList);
//				addSeries(CellularComponent.NUCLEAR_SIGNAL + "_" + uuid, data, signalList,
//						nucleusList);

			}


		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			throw new ChartDatasetCreationException("Unable to create signal dataset: %s".formatted(e.getMessage()), e);
		}
	}

	public List<Shape> createSignalRadii(@NonNull UUID signalGroup)
			throws ChartDatasetCreationException {

		final ICellCollection collection = d.getCollection();
		final List<Shape> result = new ArrayList<>();
		if (!collection.getSignalManager().hasSignals(signalGroup))
			return result;

		if (collection.getSignalGroup(signalGroup).get().isVisible()) {
			try {
				final Nucleus consensus = collection.getConsensus();
				for (final INuclearSignal n : collection.getSignalManager().getSignals(signalGroup)) {
					final IPoint p = getXYCoordinatesForSignal(n, consensus);

					// ellipses are drawn starting from x y at upper left.
					// Provide an offset from the centre
					final double offset = n.getMeasurement(Measurement.RADIUS, options.getScale());

					result.add(new Ellipse2D.Double(p.getX() - offset, p.getY() - offset,
							offset * 2, offset * 2));
				}
			} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
				throw new ChartDatasetCreationException("Unable to create radius chart", e);
			}

		}
		return result;
	}

	/**
	 * Get the XY coordinates of a given signal centre of mass on a nuclear outline.
	 * This is based on angle and fractional distance from CoM of the signal.
	 * 
	 * @param signal  the signal to plot
	 * @param outline the nucleus outline to draw the signal on
	 * @return the point of the signal centre of mass
	 * @throws MissingDataException
	 * @throws ComponentCreationException
	 * @throws SegmentUpdateException
	 */
	private IPoint getXYCoordinatesForSignal(@NonNull INuclearSignal signal, @NonNull Nucleus outline)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		// the clockwise angle from the below the CoM, through the CoM, to the signal
		// CoM
		final double angle = signal.getMeasurement(Measurement.ANGLE);
		final double fractionalDistance = signal.getMeasurement(Measurement.FRACT_DISTANCE_FROM_COM);

		// determine the distance to the border at this angle
		final IPoint borderPoint = ComponentMeasurer.getDistanceFromCoMToBorderAtAngle(outline,
				angle);
		double distanceToBorder = borderPoint.getLengthTo(outline.getCentreOfMass());

		// Adjust for scale if needed
		if (MeasurementScale.MICRONS.equals(options.getScale())) {
			distanceToBorder /= outline.getScale();
		}

		// convert to fractional distance to signal
		final double distanceFromCoM = distanceToBorder * fractionalDistance;

		// adjust angle because we are counting angles from the negative y axis
		// i.e. 90 degrees clockwise to the positive x axis
		// Angles are also plotted anti-clockwise, so subtract our clockwise angle from
		// 360
		final double signalX = Math.cos(Math.toRadians(360 - angle - 90)) * distanceFromCoM; // x
		// component
		final double signalY = Math.sin(Math.toRadians(360 - angle - 90)) * distanceFromCoM;// y component
		return new FloatPoint(signalX, signalY);
	}

	private void addSeries(Comparable<?> seriesKey, double[][] data, List<INuclearSignal> signals,
			List<Nucleus> nuclei) {
		super.addSeries(seriesKey, data, nuclei);
		signalList.add(signals);
	}

	public INuclearSignal getSignal(Comparable<?> seriesKey, int item) {
		final int seriesIndex = indexOf(seriesKey);
		return signalList.get(seriesIndex).get(item);
	}
}
