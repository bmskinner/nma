package com.bmskinner.nma.io.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.io.DatasetConverter;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.SampleDatasetReader;

public class DatasetConverterTest {

	private static final double DELTA = 0.05;

	/**
	 * Tests that array conversions of image histograms succeed
	 * 
	 * @throws Exception
	 */
	@Test
	public void test220to230DatasetHistogramArrayMeasurementsConverted() throws Exception {

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

		// Save to file with old format measurements
		new DatasetExportMethod(d0, tmpFile).call();

		final IAnalysisDataset d = backdateVersion(tmpFile, Version.V_2_2_0);

		DatasetConverter.convert(d);

		// Check histogram measurements were removed and replaced with an array
		// measurement
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

	}

	/**
	 * Tests that array conversions of PCA values succeed
	 * 
	 * @throws Exception
	 */
	@Test
	public void test220to230DatasetPCAArrayMeasurementsConverted() throws Exception {

		// Make a dataset with measurements in the 2.2.0 format
		final IAnalysisDataset d0 = SampleDatasetReader.openTestMouseClusterDataset();
		final File tmpFile = new File(TestResources.DATASET_FOLDER, "220to230conversion.nmd");

		assertTrue("The sample dataset should already be clustered", d0.hasClusters());

		// Find the measurements that should be present in each nucleus
		final List<Measurement> pcaMeasurements = new ArrayList<>();

		final List<UUID> clusterIDs = new ArrayList<>();
		for (final IClusterGroup cg : d0.getClusterGroups()) {
			clusterIDs.add(cg.getId());

			cg.getOptions().get().setInt(HashOptions.CLUSTER_NUM_PCS_KEY, 15);

			for (int i = 0; i < 15; i++) {
				pcaMeasurements.add(Measurement.makePrincipalComponent(i, cg.getId()));
			}
			// Remove the existing PCs in 2.3.0 format
			for (final Nucleus n : d0.getCollection().getNuclei()) {
				n.clearMeasurement(Measurement.makePrincipalComponent(cg.getId()));
				assertFalse("Array measurement PCAs should be removed",
						n.hasMeasurement(Measurement.makePrincipalComponent(cg.getId())));
			}
		}

		assertTrue("PCA measurements should be detected", pcaMeasurements.size() > 0);
		// Create PCA values in 2.2.0 format
		for (final Nucleus n : d0.getCollection().getNuclei()) {
			int i = 0;
			for (final Measurement m : pcaMeasurements) {
				n.setMeasurement(m, i++);
			}

		}

		// Confirm PCA values were created
		for (final Nucleus n : d0.getCollection().getNuclei()) {
			int i = 0;
			for (final Measurement m : pcaMeasurements) {
				assertTrue(n.hasMeasurement(m));
				assertEquals("Measurement should be in order", n.getMeasurement(m), i++, DELTA);
			}
		}

		// Save to file with old format measurements
		new DatasetExportMethod(d0, tmpFile).call();

		final IAnalysisDataset d = backdateVersion(tmpFile, Version.V_2_2_0);

		boolean hasPCAMeasurements = d.getCollection().stream()
				.flatMap(c -> c.getNuclei().stream())
				.flatMap(n -> n.getMeasurements().stream())
				.filter(m -> !m.isArrayMeasurement())
				.distinct()
				.anyMatch(m -> m.name().startsWith(Measurement.Names.PC));
		assertTrue("Dataset should have 2.2.0 format PCA measurements", hasPCAMeasurements);

		DatasetConverter.convert(d);

		// Get all the measurements in the dataset that should be array measurements
		hasPCAMeasurements = d.getCollection().stream()
				.flatMap(c -> c.getNuclei().stream())
				.flatMap(n -> n.getMeasurements().stream())
				.filter(m -> !m.isArrayMeasurement())
				.distinct()
				.anyMatch(m -> m.name().startsWith(Measurement.Names.PC));
		assertFalse("Dataset should not have 2.2.0 format PCA measurements", hasPCAMeasurements);

		// Check PCA measurements were removed and replaced with an array
		// measurement
		for (final IClusterGroup cg : d0.getClusterGroups()) {

			final Measurement pcaMeasure = Measurement.makePrincipalComponent(cg.getId());
			for (final Nucleus n : d.getCollection().getNuclei()) {

				// Check array measurements are in the correct order
				final List<Double> arr = n
						.getArrayMeasurement(pcaMeasure);
				int i = 0;
				for (final Double dbl : arr) {
					assertEquals("Measurement should be in order", dbl, i++, DELTA);
				}

				// Check original PCA measurements are no longer present
				for (final Measurement m : pcaMeasurements) {
					assertFalse(n.hasMeasurement(m));
				}
			}


		}
	}

	/**
	 * Tests that array conversions of TSNE values succeed
	 * 
	 * @throws Exception
	 */
	@Test
	public void test220to230DatasetTSNEArrayMeasurementsConverted() throws Exception {

		// Make a dataset with measurements in the 2.2.0 format
		final IAnalysisDataset d0 = SampleDatasetReader.openTestMouseClusterDataset();
		final File tmpFile = new File(TestResources.DATASET_FOLDER, "220to230conversion.nmd");

		assertTrue("The sample dataset should already be clustered", d0.hasClusters());

		for (final IClusterGroup cg : d0.getClusterGroups()) {

			for (final Nucleus n : d0.getCollection().getNuclei()) {
				// Remove any existing TSNE in 2.3.0 format
				n.clearMeasurement(Measurement.makeTSNE(cg.getId()));
				assertFalse("Array measurement PCAs should be removed",
						n.hasMeasurement(Measurement.makeTSNE(cg.getId())));

				// Add TSNE in 2.2.0 format
				final Measurement tsne1 = Measurement.makeTSNE(1, cg.getId());
				final Measurement tsne2 = Measurement.makeTSNE(2, cg.getId());
				n.setMeasurement(tsne1, 1);
				n.setMeasurement(tsne2, 2);

				// Confirm values were created
				assertTrue(n.hasMeasurement(tsne1));
				assertTrue(n.hasMeasurement(tsne2));
			}
		}

		// Save to file with old format measurements
		new DatasetExportMethod(d0, tmpFile).call();

		final IAnalysisDataset d = backdateVersion(tmpFile, Version.V_2_2_0);

		DatasetConverter.convert(d);

		// Check measurements were removed and replaced with an array
		// measurement
		for (final IClusterGroup cg : d0.getClusterGroups()) {
			final Measurement tsne = Measurement.makeTSNE(cg.getId());
			final Measurement tsne1 = Measurement.makeTSNE(1, cg.getId());
			final Measurement tsne2 = Measurement.makeTSNE(2, cg.getId());

			for (final Nucleus n : d.getCollection().getNuclei()) {

				// Check array measurements are in the correct order
				final List<Double> arr = n
						.getArrayMeasurement(tsne);
				int i = 1;
				for (final Double dbl : arr) {
					assertEquals("Measurement should be in order", dbl, i++, DELTA);
				}

				// Check original PCA measurements are no longer present
				assertFalse(n.hasMeasurement(tsne1));
				assertFalse(n.hasMeasurement(tsne2));
			}

		}
	}

	/**
	 * Tests that array conversions of UMAP values succeed
	 * 
	 * @throws Exception
	 */
	@Test
	public void test220to230DatasetUMAPArrayMeasurementsConverted() throws Exception {

		// Make a dataset with measurements in the 2.2.0 format
		final IAnalysisDataset d0 = SampleDatasetReader.openTestMouseClusterDataset();
		final File tmpFile = new File(TestResources.DATASET_FOLDER, "220to230conversion.nmd");

		assertTrue("The sample dataset should already be clustered", d0.hasClusters());

		for (final IClusterGroup cg : d0.getClusterGroups()) {

			for (final Nucleus n : d0.getCollection().getNuclei()) {
				// Remove any existing TSNE in 2.3.0 format
				n.clearMeasurement(Measurement.makeUMAP(cg.getId()));
				assertFalse("Array measurement PCAs should be removed",
						n.hasMeasurement(Measurement.makeUMAP(cg.getId())));

				// Add TSNE in 2.2.0 format
				final Measurement tsne1 = Measurement.makeUMAP(1, cg.getId());
				final Measurement tsne2 = Measurement.makeUMAP(2, cg.getId());
				n.setMeasurement(tsne1, 1);
				n.setMeasurement(tsne2, 2);

				// Confirm values were created
				assertTrue(n.hasMeasurement(tsne1));
				assertTrue(n.hasMeasurement(tsne2));
			}
		}

		// Save to file with old format measurements
		new DatasetExportMethod(d0, tmpFile).call();

		final IAnalysisDataset d = backdateVersion(tmpFile, Version.V_2_2_0);

		DatasetConverter.convert(d);

		// Check measurements were removed and replaced with an array
		// measurement
		for (final IClusterGroup cg : d0.getClusterGroups()) {
			final Measurement tsne = Measurement.makeUMAP(cg.getId());
			final Measurement tsne1 = Measurement.makeUMAP(1, cg.getId());
			final Measurement tsne2 = Measurement.makeUMAP(2, cg.getId());

			for (final Nucleus n : d.getCollection().getNuclei()) {

				// Check array measurements are in the correct order
				final List<Double> arr = n
						.getArrayMeasurement(tsne);
				int i = 1;
				for (final Double dbl : arr) {
					assertEquals("Measurement should be in order", dbl, i++, DELTA);
				}

				// Check original PCA measurements are no longer present
				assertFalse(n.hasMeasurement(tsne1));
				assertFalse(n.hasMeasurement(tsne2));
			}

		}
	}

	/**
	 * Read the dataset in the given file, and change the version string to a
	 * desired version
	 * 
	 * @param nmdFile
	 * @param targetVersion
	 * @return
	 * @throws Exception
	 */
	private static IAnalysisDataset backdateVersion(File nmdFile, Version targetVersion) throws Exception {
		// Conversion will not run on open because versionLastSaved will be latest
		// version. Replace and manually run the conversion
		final IAnalysisDataset d = SampleDatasetReader.openDataset(nmdFile);

		for (final Field f : d.getClass().getSuperclass().getDeclaredFields()) {
			if (f.getName().equals("versionLastSaved")) {
				f.setAccessible(true);
				f.set(d, targetVersion);
			}
		}

		assertEquals("Version should be backdated", targetVersion, d.getVersionLastSaved());
		return d;

	}

}
