package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Tests for implementations of the Taggable interface
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class TaggableTest {
	
	private static final long SEED = 1234;
	
	private Logger logger;

	private Taggable taggable;

	@Parameter(0)
	public Class<? extends Taggable> source;

	@Before
	public void setUp() throws Exception {
		taggable = createInstance(source);
		logger = Logger.getLogger(Loggable.CONSOLE_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}

	/**
	 * Create an instance of the class under test
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static Taggable createInstance(Class<? extends Taggable> source) throws Exception {

		if(source==DefaultNucleus.class){
			IAnalysisDataset d = new TestDatasetBuilder(SEED).cellCount(1)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(true)
					.profiled().segmented().build();
			return d.getCollection().getCells().stream().findFirst().get().getNucleus();
//			return TestComponentFactory.rectangularNucleus(100, 100, 20, 20, 0, 20);
		}

		throw new Exception("Unable to create instance of "+source);
	}

	@SuppressWarnings("unchecked")
	@Parameters
	public static Iterable<Class<? extends Nucleus>> arguments() {
		return Arrays.asList(DefaultNucleus.class);
	}

	@Test
	public void testGetProfileTypeTag() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		ISegmentedProfile rawProfile = taggable.getProfile(ProfileType.ANGLE);
		ISegmentedProfile tagProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		assertEquals(rawProfile.offset(taggable.getBorderIndex(Tag.REFERENCE_POINT)).toString(), tagProfile.toString());
	}

	
	@Test
	public void testUpdatingSegmentsInProfile() throws Exception {
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		IBorderSegment seg0 = oldProfile.getSegmentAt(0);
		UUID segId = seg0.getID();
		
		int oldStart = seg0.getStartIndex();
		int newStart = oldStart+10;
		
		int oldEnd = seg0.getEndIndex();
		int newEnd = oldEnd+10;
		System.out.println("Old profile: "+oldProfile);
		assertTrue(oldProfile.update(seg0, newStart, newEnd));
		System.out.println("Updated profile: "+oldProfile);
		
		taggable.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, oldProfile);
		
		// Confirm everything was saved properly
		ISegmentedProfile newProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		assertEquals(oldProfile.toString(), newProfile.toString());
	}
	
	

}
