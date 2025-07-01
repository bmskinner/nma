package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
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

		final Measurement keyR = Measurement.makeImageHistogram(0);
		final Measurement keyG = Measurement.makeImageHistogram(1);
		final Measurement keyB = Measurement.makeImageHistogram(2);

		// Calculate the data
		for (final IAnalysisDataset d : datasets) {

			final List<Double> log2BR = new ArrayList<>();
			final List<Double> log2BG = new ArrayList<>();
			final List<Double> log2RG = new ArrayList<>();
			final ICellCollection c = d.getCollection();

			try {

				for (final Nucleus n : c.getNuclei()) {
					// Each pixel intensity
					double totalR = 0;
					double totalG = 0;
					double totalB = 0;

					if (n.hasMeasurement(keyR)) {
						final List<Double> valsR = n.getArrayMeasurement(keyR);
						for (int px = 0; px < 256; px++) {
							totalR += valsR.get(px) * px;
						}
					}

					if (n.hasMeasurement(keyG)) {
						final List<Double> valsG = n.getArrayMeasurement(keyG);
						for (int px = 0; px < 256; px++) {
							totalG += valsG.get(px) * px;
						}
					}

					if (n.hasMeasurement(keyB)) {
						final List<Double> valsB = n.getArrayMeasurement(keyB);
						for (int px = 0; px < 256; px++) {
							totalB += valsB.get(px) * px;
						}
					}

					if (totalB > 0 && totalR > 0) {
						final double br = Stats.calculateLog2Ratio(totalR / totalB);
						log2BR.add(br);
					}

					if (totalB > 0 && totalG > 0) {
						final double bg = Stats.calculateLog2Ratio(totalG / totalB);
						log2BG.add(bg);
					}

					if (totalR > 0 && totalG > 0) {
						final double rg = Stats.calculateLog2Ratio(totalR / totalG);
						log2RG.add(rg);

					}
				}

			} catch (final SegmentUpdateException | MissingDataException | ComponentCreationException e) {
				LOGGER.log(Level.SEVERE, "Unable to calculate pixel ratio: %s".formatted(e.getMessage()), e);
				throw new ChartDatasetCreationException(e);
			}

			// Add the values to the chart dataset. Ensure dataset names are not unique
			ds.add(log2BR, d.getName() + "_" + d.getId(), " Red / Blue");
			ds.add(log2BG, d.getName() + "_" + d.getId(), " Green / Blue");
			ds.add(log2RG, d.getName() + "_" + d.getId(), " Red / Green");
			LOGGER.finer("Added pixel dataset for dataset %s".formatted(d.getName()));
		}

		return ds;

	}

}
