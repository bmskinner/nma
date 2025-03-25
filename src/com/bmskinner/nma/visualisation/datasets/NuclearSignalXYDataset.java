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
import com.bmskinner.nma.components.cells.CellularComponent;
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
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * An XY dataset mapping signals and nuclei to their XY coordinates
 * 
 * @author Ben Skinner
 *
 */
public class NuclearSignalXYDataset extends ComponentXYDataset<Nucleus> {

	private static final Logger LOGGER = Logger.getLogger(NuclearSignalXYDataset.class.getName());

	private List<List<INuclearSignal>> signalList = new ArrayList<>();

	private IAnalysisDataset d;
	private ChartOptions options;

	public NuclearSignalXYDataset(@NonNull ChartOptions options)
			throws ChartDatasetCreationException {
		super();
		this.d = options.firstDataset();
		this.options = options;

		ICellCollection collection = d.getCollection();
		try {
			Nucleus consensus = collection.getConsensus();

			if (collection.getSignalManager().hasSignals()) {
				for (UUID uuid : collection.getSignalManager().getSignalGroupIDs()) {

					if (d.getCollection().getSignalGroup(uuid).get().isVisible()) {

						List<INuclearSignal> signals = collection.getSignalManager()
								.getSignals(uuid);
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

						for (INuclearSignal n : signals) {

							IPoint p = getXYCoordinatesForSignal(n, consensus);

							xpoints[signalCount] = p.getX();
							ypoints[signalCount] = p.getY();
							signalCount++;

						}
						double[][] data = { xpoints, ypoints };

						addSeries(CellularComponent.NUCLEAR_SIGNAL + "_" + uuid, data, signalList,
								nucleusList);

					}
				}
			}
		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			throw new ChartDatasetCreationException("Unable to create signal dataset", e);
		}
	}

	public List<Shape> createSignalRadii(@NonNull UUID signalGroup)
			throws ChartDatasetCreationException {

		ICellCollection collection = d.getCollection();
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
					double offset = n.getMeasurement(Measurement.RADIUS, options.getScale());

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
	 * Get the XY coordinates of a given signal centre of mass on a nuclear outline
	 * 
	 * @param n       the signal to plot
	 * @param outline the nucleus outline to draw the signal on
	 * @return the point of the signal centre of mass
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 * @throws ChartDatasetCreationException
	 */
	private IPoint getXYCoordinatesForSignal(@NonNull INuclearSignal n, @NonNull Nucleus outline)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		// the clockwise angle from the below the CoM, through the CoM, to the signal
		// CoM
		double angle = n.getMeasurement(Measurement.ANGLE);
		double fractionalDistance = n.getMeasurement(Measurement.FRACT_DISTANCE_FROM_COM);

		// determine the distance to the border at this angle
		IPoint borderPoint = ComponentMeasurer.getDistanceFromCoMToBorderAtAngle(outline,
				angle);
		double distanceToBorder = borderPoint.getLengthTo(outline.getCentreOfMass());

		// Adjust for scale if needed
		if (MeasurementScale.MICRONS.equals(options.getScale()))
			distanceToBorder /= outline.getScale();

		// convert to fractional distance to signal
		double distanceFromCoM = distanceToBorder * fractionalDistance;

		// adjust angle because we are counting angles from the negative y axis
		// i.e. 90 degrees clockwise to the positive x axis
		// Angles are also plotted anti-clockwise, so subtract our clockwise angle from
		// 360
		double signalX = Math.cos(Math.toRadians(360 - angle - 90)) * distanceFromCoM; // x
																						// component
		double signalY = Math.sin(Math.toRadians(360 - angle - 90)) * distanceFromCoM;// y component
		return new FloatPoint(signalX, signalY);
	}

	private void addSeries(Comparable<?> seriesKey, double[][] data, List<INuclearSignal> signals,
			List<Nucleus> nuclei) {
		super.addSeries(seriesKey, data, nuclei);
		signalList.add(signals);
	}

	public INuclearSignal getSignal(Comparable<?> seriesKey, int item) {
		int seriesIndex = indexOf(seriesKey);
		return signalList.get(seriesIndex).get(item);
	}
}
