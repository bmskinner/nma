package com.bmskinner.nuclear_morphology.components.options;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class OptionsFactoryTest {

	@Test
	public void testClustersAdded() {
		HashOptions o = OptionsFactory.makeNucleusDetectionOptions(new File("")).build();
		
		o.setSubOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY, OptionsFactory.makeDefaultClusteringOptions().build());

		HashOptions t = o.getSubOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY);
		assertEquals(OptionsFactory.makeDefaultClusteringOptions(), t);
	}

}
