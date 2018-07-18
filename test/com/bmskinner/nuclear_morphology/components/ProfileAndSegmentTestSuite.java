package com.bmskinner.nuclear_morphology.components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.components.generic.IBorderSegmentTester;
import com.bmskinner.nuclear_morphology.components.generic.IProfileTester;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfileTester;
import com.bmskinner.nuclear_morphology.io.conversion.MouseFormatConverterTest;
import com.bmskinner.nuclear_morphology.io.conversion.PigFormatConverterTest;
import com.bmskinner.nuclear_morphology.io.conversion.RoundFormatConverterTest;

/**
 * Test the profile, segmented profile and border segment implementing
 * classes
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	IProfileTester.class, 
    ISegmentedProfileTester.class, 
    IBorderSegmentTester.class })

public class ProfileAndSegmentTestSuite {}