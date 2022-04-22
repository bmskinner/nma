package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

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
import com.bmskinner.nuclear_morphology.utility.AngleTools;

/**
 * An XY dataset mapping signals and nuclei to their XY coordinates
 * 
 * @author ben
 *
 */
public class NuclearSignalXYDataset extends ComponentXYDataset<Nucleus> {

	private List<List<INuclearSignal>> signalList = new ArrayList<>();

	private IAnalysisDataset d;

	public NuclearSignalXYDataset(@NonNull IAnalysisDataset d) throws ChartDatasetCreationException {
		super();
		this.d = d;
		ICellCollection collection = d.getCollection();
		try {
			Nucleus consensus = collection.getConsensus();

			if (collection.getSignalManager().hasSignals()) {
				for (UUID uuid : collection.getSignalManager().getSignalGroupIDs()) {

					if (d.getCollection().getSignalGroup(uuid).get().isVisible()) {

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

						for (INuclearSignal n : signals) {

							IPoint p = getXYCoordinatesForSignal(n, consensus);

							xpoints[signalCount] = p.getX();
							ypoints[signalCount] = p.getY();
							signalCount++;

						}
						double[][] data = { xpoints, ypoints };

						addSeries(CellularComponent.NUCLEAR_SIGNAL + "_" + uuid, data, signalList, nucleusList);

					}
				}
			}
		} catch (MissingLandmarkException | ComponentCreationException e) {
			throw new ChartDatasetCreationException(e);
		}
	}

	public List<Shape> createSignalRadii(@NonNull UUID signalGroup) throws ChartDatasetCreationException {

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
	 * Get the XY coordinates of a given signal centre of mass on a nuclear outline
	 * 
	 * @param n       the signal to plot
	 * @param outline the nucleus outline to draw the signal on
	 * @return the point of the signal centre of mass
	 * @throws ChartDatasetCreationException
	 */
	private IPoint getXYCoordinatesForSignal(@NonNull INuclearSignal n, @NonNull Nucleus outline) {

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
