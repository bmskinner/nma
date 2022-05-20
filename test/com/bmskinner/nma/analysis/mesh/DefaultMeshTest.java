package com.bmskinner.nma.analysis.mesh;

import static org.junit.Assert.fail;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.mesh.DefaultMesh;
import com.bmskinner.nma.components.mesh.Mesh;
import com.bmskinner.nma.components.mesh.MeshCreationException;
import com.bmskinner.nma.io.SampleDatasetReader;

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
		IAnalysisDataset d = SampleDatasetReader.openTestMouseDataset();
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
		IAnalysisDataset d = SampleDatasetReader.openTestMouseDataset();
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
	 * @throws MissingLandmarkException 
	 */
	private void testDatasetGeneratesMeshForAllNuclei(@NonNull final IAnalysisDataset d) throws MissingLandmarkException {
		try {
			Mesh consensusMesh = new DefaultMesh(d.getCollection().getConsensus());

			for(Nucleus n: d.getCollection().getNuclei()) {
				try {
					Mesh m = new DefaultMesh(n, consensusMesh);
				} catch (MeshCreationException e) {
					fail("Unable to create mesh for "+n.getNameAndNumber()+": "+e.getMessage());
				}
			}
		} catch (MeshCreationException | ComponentCreationException e) {
			fail("Unable to create consensus mesh: "+e.getMessage());
		}
	}
	
	/**
	 * Attempt to create a mesh for every nucleus in the given dataset based on 
	 * the consensus nucleus
	 * @param d
	 * @throws MissingLandmarkException 
	 */
	private void testDatasetGeneratesMeshForAllVerticalNuclei(@NonNull final IAnalysisDataset d) throws MissingLandmarkException {
		try {
			Mesh consensusMesh = new DefaultMesh(d.getCollection().getConsensus());

			for(Nucleus n: d.getCollection().getNuclei()) {
				try {
					Mesh m = new DefaultMesh(n.getOrientedNucleus(), consensusMesh);
				} catch (MeshCreationException e) {
					fail("Unable to create mesh for "+n.getNameAndNumber()+": "+e.getMessage());
				}
			}
		} catch (MeshCreationException | ComponentCreationException e) {
			fail("Unable to create consensus mesh: "+e.getMessage());
		}
	}
	
	
}
