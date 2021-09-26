package com.bmskinner.nuclear_morphology.analysis.classification;

import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;

/**
 * Tests for principal component analysis
 * @author bms41
 * @since 1.16.0
 *
 */
public class PrincipalComponentAnalysisTest extends ComponentTester {
	
	private static final Logger LOGGER = Logger.getLogger(PrincipalComponentAnalysisTest.class.getName());
	
	private IAnalysisDataset dataset;
	
	@Before
	public void setUp() throws Exception {
		dataset = new TestDatasetBuilder(RNG_SEED).cellCount(50)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
	}

	@Test
	public void testAllNucleiGetPrincipalComponents() throws Exception {
		
		// Check first 10 PC stats are empty 
		for(int i=0; i<10; i++) {
			final int j = i;
			boolean isPresent = dataset.getCollection().getNuclei().stream().noneMatch(m->m.hasStatistic(Measurement.makePrincipalComponent(j+1)));
			assertTrue(isPresent);
		}
		
		// Run the PCA on angle profiles
		HashOptions options = new DefaultOptions();
		options.setBoolean(ProfileType.ANGLE.toString(), true);
		options.setDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY, 0.95);
		
		PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis(dataset, options);
		pca.call();
		
		Nucleus n = dataset.getCollection().getNuclei().stream().findFirst().get();
		int nPcs = (int) n.getStatistic(Measurement.PCA_N);		
		
		// Test that PCs have been set
		for(int i=0; i<nPcs; i++) {
			final int j = i;
			boolean isPresent = dataset.getCollection().getNuclei().stream().allMatch(m->m.hasStatistic(Measurement.makePrincipalComponent(j+1)));
			assertTrue(isPresent);
		}
	}

}
