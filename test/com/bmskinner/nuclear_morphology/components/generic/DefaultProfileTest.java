package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.components.cells.SegmentedCellularComponent;
import com.bmskinner.nuclear_morphology.components.profiles.FloatProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Tests for the default profile embedded within a component
 * @author ben
 * @since 1.13.8
 *
 */
public class DefaultProfileTest extends ComponentTester {
	
	protected SegmentedCellularComponent comp;
	protected float[] data;
	private IProfile profile;
	

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		comp = new DummySegmentedCellularComponent();
		data = new float[comp.getBorderLength()];
		for(int i=0; i<data.length; i++) {
			data[i] = (float) ((Math.sin(Math.toRadians(i))+1)*180);
		}
	    profile = comp.new DefaultProfile(data);
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#FloatProfile(float[])}.
	 */
	@Test
	public void testFloatProfileFloatArrayWithNullData() {
		exception.expect(IllegalArgumentException.class);
		comp.new DefaultProfile( (float[]) null);
	}
				
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#FloatProfile(com.bmskinner.nuclear_morphology.components.profiles.IProfile)}.
	 */
	@Test
	public void testFloatProfileIProfile() {
		float[] result = comp.new DefaultProfile(profile).toFloatArray();
		assertTrue (equals(data, result));	
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#FloatProfile(float, int)}.
	 */
	@Test
	public void testFloatProfileFloatInt() {
		
		int value = 1;
		float[] exp = new float[data.length];
		Arrays.fill(exp, value);
		
		IProfile p  = comp.new DefaultProfile(value);
		
		float[] result = p.toFloatArray();
		
		assertTrue (equals(exp, result));	
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		IProfile p2 = comp.new DefaultProfile(data);
		IProfile p3 = comp.new DefaultProfile(data);
		assertTrue(p3.equals(p2));
	}
	
	@Test
    public void testEqualsFalseWithSameDataInDifferentProfileType(){
	    float[] d = new float[data.length];
	    for(int i=0; i<data.length; i++){
	        d[i] = data[i];
	    }
	    IProfile p = new FloatProfile(d);
        assertFalse(profile.equals(p));
    }


	/**
	 * Test float array equality. Not in junit.
	 * @param exp
	 * @param obs
	 */
	public static boolean equals(float[] exp, float[] obs){
	    
	    boolean equal = true;
	    equal &= obs.length==exp.length;
	    assertEquals(exp.length, obs.length);
        
        for(int i=0; i<exp.length; i++){
            equal &= (Float.isNaN(exp[i]) && Float.isNaN(obs[i])) || Math.abs(exp[i] - obs[i])==0f;
            assertEquals("Index "+i, exp[i], obs[i], 0);
        }
        return equal;
	}

}
