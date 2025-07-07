package com.bmskinner.nma.io.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.io.DatasetConverter;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.SampleDatasetReader;

public class DatasetConverterTest {

	private static final double DELTA = 0.05;

	/**
	 * Tests that array conversions succeed
	 * 
	 * @throws Exception
	 */
	@Test
	public void test220to230DatasetArrayMeasurementsConverted() throws Exception {

		// Make a dataset with measurements in the 2.2.0 format
		final IAnalysisDataset d0 = SampleDatasetReader.openTestMouseDataset();
		final File tmpFile = new File(TestResources.DATASET_FOLDER, "220to230conversion.nmd");


		// Add arbitrary image histogram measurements - we don't care about the value,
		// only that order is preserved on conversion
		final List<Measurement> histogram = Measurement.getPixelHistogramMeasurements(
				d0.getAnalysisOptions().get().getNucleusDetectionOptions().get().getInt(HashOptions.CHANNEL));

		for (final Nucleus n : d0.getCollection().getNuclei()) {
			int i=0;
			for (final Measurement m : histogram) {
				n.setMeasurement(m, i++);
			}
		}

		// Check pixel histogram measurements were created properly
		for (final Nucleus n : d0.getCollection().getNuclei()) {
			int i = 0;
			for (final Measurement m : histogram) {
				assertTrue(n.hasMeasurement(m));
				assertEquals("Measurement should be in order", n.getMeasurement(m), i++, DELTA);
			}
		}

		// Add arbitrary PCA values for 10 principal components
		// TODO

		// Save to file with old format measurements
		new DatasetExportMethod(d0, tmpFile).call();

		// Conversion will not run on open because versionLastSaved will be latest
		// version. Replace and manually run the conversion
		final IAnalysisDataset d = SampleDatasetReader.openDataset(tmpFile);

		for (final Field f : d.getClass().getSuperclass().getDeclaredFields()) {
			if (f.getName().equals("versionLastSaved")) {
				f.setAccessible(true);
				f.set(d, Version.V_2_2_0);
			}
		}

		assertEquals("Version should be 2.2.0", Version.V_2_2_0, d.getVersionLastSaved());

		DatasetConverter.convert(d);

		// Check measurements were removed and replaced with an array measurement
		for (final Nucleus n : d.getCollection().getNuclei()) {

			for (final Measurement m : histogram) {
				assertFalse(n.hasMeasurement(m));
			}

			// Check array measurements are in the correct order
			final List<Double> arr = n
					.getArrayMeasurement(Measurement.makeImageHistogram(CellularComponent.NUCLEUS));
			int i = 0;
			for (final Double dbl : arr) {
				assertEquals("Measurement should be in order", dbl, i++, DELTA);
			}
		}

//		fail("Not yet implemented");

	}

}
