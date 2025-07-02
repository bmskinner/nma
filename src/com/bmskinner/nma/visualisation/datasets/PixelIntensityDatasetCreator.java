package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Creator for charting datasets involving pixel intensities within cells
 * 
 */
public class PixelIntensityDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	private static final Logger LOGGER = Logger.getLogger(PixelIntensityDatasetCreator.class.getName());


	/**
	 * Create with options
	 * 
	 * @param options
	 */
	public PixelIntensityDatasetCreator(@NonNull final ChartOptions options) {
		super(options);
	}

	/**
	 * Create a dataset that contains CGH log2ratios for all channel combinations in
	 * a dataset. i.e. blue-red, blue-green, red-green (for whichever combinations
	 * are present).
	 * 
	 * @return a violin dataset
	 */
	public ExportableBoxAndWhiskerCategoryDataset createNucleusCGHDataset() throws ChartDatasetCreationException {

		final ExportableBoxAndWhiskerCategoryDataset ds = new ViolinCategoryDataset();

		final List<IAnalysisDataset> datasets = options.getDatasets();

		// determine the pixel measurements available in the datasets
		final List<Measurement> measurements = new ArrayList<>();

		measurements.add(Measurement.makeImageHistogram(CellularComponent.NUCLEUS));

		for (final IAnalysisDataset d : datasets) {
			for (final ISignalGroup sg : d.getCollection().getSignalGroups()) {
				measurements.add(Measurement.makeImageHistogram(CellularComponent.NUCLEAR_SIGNAL + "_" + sg.getId()));
			}
		}

		// Calculate the data
		for (final IAnalysisDataset d : datasets) {

			final Map<String, List<Double>> log2Values = new HashMap<>();

			final ICellCollection c = d.getCollection();

			try {

				for (final Nucleus n : c.getNuclei()) {
					// Each pixel intensity
					final Map<Measurement, Double> totalValues = new HashMap<>();

					// Calculate per-nucleus totals for each channel
					for (final Measurement m : measurements) {

						if (n.hasMeasurement(m)) {
							double total = 0;
							final List<Double> vals = n.getArrayMeasurement(m);
							for (int px = 0; px < 256; px++) {
								total += vals.get(px) * px;
							}
							totalValues.put(m, total);
						}
					}

					// Calculate log2 ratios for valid channel pairs
					for (final Measurement m0 : measurements) {
						if (!n.hasMeasurement(m0)) {
							continue;
						}
						for (final Measurement m1 : measurements) {

							if (!n.hasMeasurement(m1)) {
								continue;
							}

							// Skip self comparisons
							if (m0 == m1 | m0.name().equals(m1.name())) {
								continue;
							}

							// Only put nucleus channel in denominator
							if (m0.name().contains(CellularComponent.NUCLEUS)) {
								continue;
							}


							final double total0 = totalValues.get(m0);
							final double total1 = totalValues.get(m1);
							if (total0 > 0 && total1 > 0) {
								final double log2 = Stats.calculateLog2Ratio(total0 / total1);

								// Make friendly labels with the signal group names where needed
								String m0Name = m0.name().replace(Measurement.Names.PIXEL_HISTOGRAM + "_", "");
								String m1Name = m1.name().replace(Measurement.Names.PIXEL_HISTOGRAM + "_", "");

								m0Name = m0Name.replace(CellularComponent.NUCLEAR_SIGNAL + "_", "");
								m1Name = m1Name.replace(CellularComponent.NUCLEAR_SIGNAL + "_", "");

								// If what is left is a UUID, we now get the signal group name
								try {
									m0Name = c.getSignalManager().getSignalGroupName(UUID.fromString(m0Name));
								} catch (final IllegalArgumentException e) {
									// was not a UUID, ignore, name will be unchanged
								}

								try {
									m1Name = c.getSignalManager().getSignalGroupName(UUID.fromString(m1Name));
								} catch (final IllegalArgumentException e) {
									// was not a UUID, ignore, name will be unchanged
								}


								final String key = m0Name + " / " + m1Name;
								final List<Double> log2List = log2Values.computeIfAbsent(key, k -> new ArrayList<>());
								log2List.add(log2);
							}

						}
					}

				}

			} catch (final SegmentUpdateException | MissingDataException | ComponentCreationException e) {
				LOGGER.log(Level.SEVERE, "Unable to calculate pixel ratio: %s".formatted(e.getMessage()), e);
				throw new ChartDatasetCreationException(e);
			}

			// Add the values to the chart dataset. Ensure dataset names are unique
			for (final Entry<String, List<Double>> entry : log2Values.entrySet()) {
				LOGGER.fine("Added pixel dataset for  %s".formatted(entry.getKey()));
				ds.add(entry.getValue(), d.getName() + "_" + d.getId(), entry.getKey());
			}
			LOGGER.finer("Added pixel dataset for dataset %s".formatted(d.getName()));
		}

		return ds;

	}

}
