package com.bmskinner.nuclear_morphology.analysis.mesh;

import static org.junit.Assert.fail;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Tests for the default mesh
 * @author ben
 * @since 1.18.3
 *
 */
public class DefaultMeshTest {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testMouseDatasetGeneratesMeshForAllNuclei() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		testDatasetGeneratesMeshForAllNuclei(d);
	}
	
	@Test
	public void testPigDatasetGeneratesMeshForAllNuclei() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestPigDataset();
		testDatasetGeneratesMeshForAllNuclei(d);
	}
	
	@Test
	public void testRoundDatasetGeneratesMeshForAllNuclei() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRoundDataset();
		testDatasetGeneratesMeshForAllNuclei(d);
	}
	
	@Test
	public void testMouseDatasetGeneratesMeshForAllVerticalNuclei() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		testDatasetGeneratesMeshForAllVerticalNuclei(d);
	}
	
	@Test
	public void testPigDatasetGeneratesMeshForAllVerticalNuclei() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestPigDataset();
		testDatasetGeneratesMeshForAllVerticalNuclei(d);
	}
	
	@Test
	public void testRoundDatasetGeneratesMeshForAllVerticalNuclei() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRoundDataset();
		testDatasetGeneratesMeshForAllVerticalNuclei(d);
	}
	
	/**
	 * Attempt to create a mesh for every nucleus in the given dataset based on 
	 * the consensus nucleus
	 * @param d
	 */
	private void testDatasetGeneratesMeshForAllNuclei(@NonNull final IAnalysisDataset d) {
		try {
			Mesh<Nucleus> consensusMesh = new DefaultMesh<>(d.getCollection().getConsensus());

			for(Nucleus n: d.getCollection().getNuclei()) {
				try {
					Mesh<Nucleus> m = new DefaultMesh<>(n, consensusMesh);
				} catch (MeshCreationException e) {
					fail("Unable to create mesh for "+n.getNameAndNumber()+": "+e.getMessage());
				}
			}
		} catch (MeshCreationException e) {
			fail("Unable to create consensus mesh: "+e.getMessage());
		}
	}
	
	/**
	 * Attempt to create a mesh for every nucleus in the given dataset based on 
	 * the consensus nucleus
	 * @param d
	 */
	private void testDatasetGeneratesMeshForAllVerticalNuclei(@NonNull final IAnalysisDataset d) {
		try {
			Mesh<Nucleus> consensusMesh = new DefaultMesh<>(d.getCollection().getConsensus());

			for(Nucleus n: d.getCollection().getNuclei()) {
				try {
					Mesh<Nucleus> m = new DefaultMesh<>(n.getVerticallyRotatedNucleus(), consensusMesh);
				} catch (MeshCreationException e) {
					fail("Unable to create mesh for "+n.getNameAndNumber()+": "+e.getMessage());
				}
			}
		} catch (MeshCreationException e) {
			fail("Unable to create consensus mesh: "+e.getMessage());
		}
	}
	
	
}
