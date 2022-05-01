package com.bmskinner.nma.analysis.profiles;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.profiles.ProfileCreator;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;

public class ProfileCreatorTest {

	@Test
	public void testProfilesAreConsistentOnUnmarshallingInTestData() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED).cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40).baseWidth(40)
				.segmented().build();
		
		
		for(ICell c : d.getCollection()) {
			Element e = c.toXmlElement();

			Map<ProfileType, IProfile> exp = new HashMap<>();
			for(ProfileType t : ProfileType.values())
				exp.put(t, new DefaultProfile(c.getPrimaryNucleus().getProfile(t)));
			
			ICell dup = new DefaultCell(e);
			for(ProfileType t : ProfileType.values())
				assertEquals(t+" should match", exp.get(t), 
						ProfileCreator.createProfile(dup.getPrimaryNucleus(), t)
						.startFrom(dup.getPrimaryNucleus().getBorderIndex(OrientationMark.REFERENCE)));
			
		}
	}
	
	@Test
	public void testProfilesAreConsistentOnUnmarshallingInRealData() throws Exception {
		
		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(TestResources.IMAGE_FOLDER, op, false);
		
		
		for(ICell c : d.getCollection()) {
			Element e = c.toXmlElement();
			
			Nucleus n = c.getPrimaryNucleus();

			// Store the actual profiles from the object
			Map<ProfileType, IProfile> exp = new HashMap<>();
			
			// Create the profiles new from the object
			Map<ProfileType, IProfile> denovo = new HashMap<>();
			
			for(ProfileType t : ProfileType.values()) {
				exp.put(t, new DefaultProfile(n.getProfile(t)));
				denovo.put(t, ProfileCreator.createProfile(n, t)
						.startFrom(n.getBorderIndex(OrientationMark.REFERENCE)));
			}
			
			Nucleus dup = new DefaultCell(e).getPrimaryNucleus();
			
			// Confirm both nuclei have the same outlines
			assertEquals(n.getBorderList(), dup.getBorderList());
						
			for(ProfileType t : ProfileType.values()) {

				assertEquals(t+" creation on duplicated object", ProfileCreator.createProfile(n, t),
						ProfileCreator.createProfile(dup, t));
				
				assertEquals(t+" should match denovo", denovo.get(t), 
						ProfileCreator.createProfile(dup, t)
						.startFrom(dup.getBorderIndex(OrientationMark.REFERENCE)));
				assertEquals(t+" should match stored", exp.get(t), 
						ProfileCreator.createProfile(dup, t)
						.startFrom(dup.getBorderIndex(OrientationMark.REFERENCE)));
			}
			
		}
	}

}
