package com.bmskinner.nma.visualisation.datasets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.visualisation.datasets.VennCounter.VennCircle;

public class VennCounterTest {

	@Test
	public void testCounter0010() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openDataset(TestResources.MOUSE_CLUSTERS_DATASET);

		List<IAnalysisDataset> cluster = new ArrayList<>();
		cluster.add(d);
		cluster.add(d.copy());

		VennCounter vc = new VennCounter(cluster);

		assertEquals("0010", vc.getType());

		assertEquals(63, vc.getCount(VennCircle.AB));
		assertEquals(0, vc.getCount(VennCircle.A));
		assertEquals(0, vc.getCount(VennCircle.B));

	}

	@Test
	public void testCounter0011() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openDataset(TestResources.MOUSE_CLUSTERS_DATASET);

		List<IAnalysisDataset> cluster = new ArrayList<>();
		cluster.add(d);
		cluster.add(d.getAllChildDatasets().get(0));

		VennCounter vc = new VennCounter(cluster);

		assertEquals("0011", vc.getType());
		assertEquals(56, vc.getCount(VennCircle.AB));
		assertEquals(7, vc.getCount(VennCircle.A));
		assertEquals(0, vc.getCount(VennCircle.B));
	}

	@Test
	public void testCounter0012() throws Exception {
		fail("Not yet implemented");
	}

	@Test
	public void testCounter0020() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openDataset(TestResources.MOUSE_CLUSTERS_DATASET);

		List<IAnalysisDataset> cluster = new ArrayList<>();
		cluster.add(d);
		cluster.addAll(d.getChildDatasets());

		VennCounter vc = new VennCounter(cluster);

		assertEquals("0020", vc.getType());
		assertEquals(0, vc.getCount(VennCircle.ABC));
		assertEquals(56, vc.getCount(VennCircle.AB));
		assertEquals(0, vc.getCount(VennCircle.AC));
		assertEquals(7, vc.getCount(VennCircle.BC));
		assertEquals(0, vc.getCount(VennCircle.A));
		assertEquals(0, vc.getCount(VennCircle.B));
		assertEquals(0, vc.getCount(VennCircle.C));
	}

}
