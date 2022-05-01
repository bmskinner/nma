package com.bmskinner.nma.analysis.signals;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nma.analysis.signals.PairedSignalGroups.DatasetSignalId;

public class PairedSignalGroupsTest {

	@Test
	public void testSignalGroupsWithSameUUIDFromDifferentDatasetsAreAdded() {
		UUID sId = UUID.randomUUID();
		UUID d1 = UUID.randomUUID();
		UUID d2 = UUID.randomUUID();
		UUID d3 = UUID.randomUUID();
		UUID sId2 = UUID.randomUUID();

		// Assign signal groups with the same id
		// and one with different id
		PairedSignalGroups groups = new PairedSignalGroups();
		groups.add(d1, sId, d2, sId);
		groups.add(d1, sId, d3, sId2);

		for (DatasetSignalId id : groups.keySet()) {
			assertEquals(id.datasetId(), d1);
			assertEquals(id.signalId(), sId);
		}

	}

}
