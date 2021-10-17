package com.bmskinner.nuclear_morphology.components.options;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class OptionsFactoryTest {

	@Test
	public void testClustersAdded() {
		HashOptions o = OptionsFactory.makeNucleusDetectionOptions(new File(""));
		
		o.setSubOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY, OptionsFactory.makeDefaultClusteringOptions());

		HashOptions t = o.getSubOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY);
		assertEquals(OptionsFactory.makeDefaultClusteringOptions(), t);
	}

}
