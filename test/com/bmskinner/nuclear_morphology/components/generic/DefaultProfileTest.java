package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Tests for the default profile embedded within a component
 * @author ben
 * @since 1.13.8
 *
 */
public class DefaultProfileTest {
	
	protected SegmentedCellularComponent comp;
	protected float[] data;
	private IProfile profile;
	

	@Before
	public void setUp() throws UnavailableProfileTypeException{
		comp = new DummySegmentedCellularComponent();
	    profile = comp.new DefaultProfile(comp.getProfile(ProfileType.ANGLE));
	    data = profile.toFloatArray();
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
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#FloatProfile(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
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
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#size()}.
	 */
	@Test
	public void testSize() {
		assertEquals(profile.size(), data.length);
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

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#get(int)}.
	 */
	@Test
	public void testGetInt() {
		double d = profile.get(4);
		assertEquals( data[4] , d, 0);
	}
	
	@Test
	public void testGetIntExceptsOnNegativeIndex() {
	    exception.expect(IndexOutOfBoundsException.class);
        profile.get(-1);
	}
	
	@Test
    public void testGetIntExceptsOnOutOfBoundsIndex() {
        exception.expect(IndexOutOfBoundsException.class);
        profile.get(profile.size()+1);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#get(double)}.
	 */
	@Test
	public void testGetDouble() {
		double d = profile.get(0.5);
		int mid = data.length/2;
		double exp = profile.get(mid);
		assertEquals(exp, d, 0);
	}
	
	@Test
    public void testGetDoubleExceptsWhenProportionOutOfLowerBounds() {
	    exception.expect(IndexOutOfBoundsException.class);
        profile.get(-0.1);
    }
	
	@Test
	public void testGetDoubleExceptsWhenProportionOutOfUpperBounds() {
	    exception.expect(IndexOutOfBoundsException.class);
	    profile.get(1.1);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getMax()}.
	 */
	@Test
	public void testGetMax() {
		DoubleStream ds = IntStream.range(0, data.length)
                .mapToDouble(i -> data[i]);
		assertEquals( ds.max().getAsDouble(), profile.getMax(), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getIndexOfMax(com.bmskinner.nuclear_morphology.components.generic.BooleanProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMaxBooleanProfile() throws ProfileException {

	    // Restrict to first half of array
	    BooleanProfile b = new BooleanProfile(data.length, false);
	    for(int i=0; i<data.length/2; i++){
	        b.set(i, true);
	    }
	    
	    int exp = IntStream.range(0, data.length/2)
	    		.boxed().max(Comparator.comparing(profile::get))
	    		.get();
 
	    assertEquals( exp , profile.getIndexOfMax(b));
	}
		
	@Test
    public void testGetIndexOfMaxBooleanProfileExceptsOnAllFalseProfile() throws ProfileException{
	    BooleanProfile b = new BooleanProfile(data.length, false);
        exception.expect(ProfileException.class);
        profile.getIndexOfMax(b);
    }
	
	@Test
    public void testGetIndexOfMaxBooleanProfileExceptsOnDifferentLength() throws ProfileException{
        BooleanProfile b = new BooleanProfile(data.length/2, false);
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfMax(b);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getIndexOfMax()}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMax() throws ProfileException {
		int exp = IntStream.range(0, data.length)
	    		.boxed().max(Comparator.comparing(profile::get))
	    		.get();
		assertEquals( exp , profile.getIndexOfMax());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getIndexOfFraction(double)}.
	 */
	@Test
	public void testGetIndexOfFraction() {
		
		double fraction = 0.5;
		int exp = data.length/2;
		int i = profile.getIndexOfFraction(fraction);
		assertEquals( exp, i);
		
	}
	
	@Test
    public void testGetIndexOfFractionExceptsOnLessThanZero() {
	    exception.expect(IllegalArgumentException.class);
	    profile.getIndexOfFraction(-0.1);
	}
	
	@Test
    public void testGetIndexOfFractionExceptsOnGreaterThanOne() {
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfFraction(1.1);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getFractionOfIndex(int)}.
	 */
	@Test
	public void testGetFractionOfIndex() {
		assertEquals( 0, profile.getFractionOfIndex(0), 0 );
		
		double f =  (double)(profile.size()-1)/ (double)profile.size();
		assertEquals( f, profile.getFractionOfIndex(profile.size()-1), 0 );
	}
	
	@Test
    public void testGetFractionOfIndexExceptsOnIndexBelowZero() {
        exception.expect(IllegalArgumentException.class);
        profile.getFractionOfIndex(-1);
    }
	
	@Test
    public void testGetFractionOfIndexExceptsOnIndexAboveBounds() {
        exception.expect(IllegalArgumentException.class);
        profile.getFractionOfIndex(profile.size()+1);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getMin()}.
	 */
	@Test
	public void testGetMin() {
		DoubleStream ds = IntStream.range(0, data.length)
                .mapToDouble(i -> data[i]);
		assertEquals( ds.min().getAsDouble(), profile.getMin(), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getIndexOfMin(com.bmskinner.nuclear_morphology.components.generic.BooleanProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMinBooleanProfile() throws ProfileException {
	    // Restrict to second half of array
	    BooleanProfile b = new BooleanProfile(data.length, true);
	    for(int i=0; i<data.length/2; i++){
	        b.set(i, false);
	    }
	    int exp = IntStream.range(data.length/2, data.length-1)
	    		.boxed().min(Comparator.comparing(profile::get))
	    		.get();
 
	    assertEquals( exp , profile.getIndexOfMin(b));
	}
	    
    @Test
    public void testGetIndexOfMinBooleanProfileExceptsOnAllFalseProfile() throws ProfileException{
        BooleanProfile b = new BooleanProfile(data.length, false);
        exception.expect(ProfileException.class);
        profile.getIndexOfMin(b);
    }
	
	@Test
    public void testGetIndexOfMinBooleanProfileExceptsOnDifferentLength() throws ProfileException{
        BooleanProfile b = new BooleanProfile(data.length/2, false);
        exception.expect(IllegalArgumentException.class);
        profile.getIndexOfMin(b);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getIndexOfMin()}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetIndexOfMin() throws ProfileException {
		
		int exp = IntStream.range(0, data.length-1)
	    		.boxed().min(Comparator.comparing(profile::get))
	    		.get();
 
		assertEquals( exp , profile.getIndexOfMin());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#toDoubleArray()}.
	 */
	@Test
	public void testToDoubleArray() {
	    
	    double[] d = new double[data.length];
	    for(int i=0; i<data.length; i++){
	        d[i] = data[i];
	    }
	    
	    double[] res = profile.toDoubleArray();
	    
	    assertTrue(Arrays.equals(d, res));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#absoluteSquareDifference(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testAbsoluteSquareDifferenceIsZeroWhenSameProfile() throws ProfileException {
		assertEquals(0, profile.absoluteSquareDifference(profile), 0);
	}
	
	private void testAbsoluteSquareDifferenceOnSameLengthProfiles(IProfile template, float diff) throws ProfileException{
	    float[] test =  Arrays.copyOf(template.toFloatArray(), template.size());
        
        test[0] = test[0]+diff;
        
        IProfile p = comp.new DefaultProfile(test);
        
        double expDiff = diff*diff;
        
        assertEquals(expDiff, template.absoluteSquareDifference(p), 0);
	}
	
	@Test
    public void testAbsoluteSquareDifferenceOnSameLengthProfilesPositive() throws ProfileException {
	    testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, 2);
    }
	
	@Test
    public void testAbsoluteSquareDifferenceOnSameLengthProfilesNegative() throws ProfileException {
	    testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, -2);
    }
	
	private void testAbsoluteSquareDifferenceOnDifferentLengthProfiles(IProfile template, int newLength, float diff) throws ProfileException{

	    IProfile t = template.interpolate(newLength);
	    
	    float[] arr = t.toFloatArray();
	    arr[0] = arr[0]+diff;
        
	    IProfile p = new FloatProfile(arr);        
        double expDiff = diff*diff;
        
        assertEquals(expDiff, template.absoluteSquareDifference(p), 0);
	}
	
	@Test
    public void testAbsoluteSquareDifferenceOnLongerProfilesPositive() throws ProfileException {
	    testAbsoluteSquareDifferenceOnDifferentLengthProfiles(profile, profile.size()*2, 2);
    }
	
	@Test
    public void testAbsoluteSquareDifferenceOnShorterProfilesPositive() throws ProfileException {
        testAbsoluteSquareDifferenceOnDifferentLengthProfiles(profile, profile.size()/2, 2);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#copy()}.
	 */
	@Test
	public void testCopy() {
		IProfile p = comp.new DefaultProfile(data);
		float[] result = p.copy().toFloatArray();

		assertTrue(Arrays.equals(data, result));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#offset(int)}.
	 */
	@Test
	public void testOffsetByOne() throws ProfileException {
	    float[] exp1 = new float[data.length];
	    for(int i=0; i<data.length-1; i++){
	    	exp1[i] = data[i+1];
	    }
	    exp1[data.length-1] = data[0];
	    
	    float[] result = profile.offset(1).toFloatArray();
	    assertTrue( equals(exp1, result));
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#offset(int)}.
	 */
	@Test
	public void testOffsetByFive() throws ProfileException {
		int offset = 5;
	    float[] exp1 = new float[data.length];
	    for(int i=0; i<data.length-offset; i++){
	    	exp1[i] = data[i+offset];
	    }
	    
	    for(int i=offset; i>0; i--){
	    	exp1[data.length-i] = data[offset-i];
	    }
	    	    
	    float[] result = profile.offset(offset).toFloatArray();
	    assertTrue( equals(exp1, result));
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#offset(int)}.
	 */
	@Test
	public void testOffsetByNegativeOne() throws ProfileException {

	    float[] exp1 = new float[data.length];
	    for(int i=1; i<data.length; i++){
	    	exp1[i] = data[i-1];
	    }
	    exp1[0] = data[data.length-1];
	    
	    float[] result = profile.offset(-1).toFloatArray();
	    assertTrue( equals(exp1, result));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#smooth(int)}.
	 */
	@Test
	public void testSmooth() {
	    IProfile p = profile.smooth(2);
	    fail("Not implemented");
	    float[] exp = p.toFloatArray(); // TODO
	    assertTrue( equals(exp, p.toFloatArray()));
	}
	
	@Test
    public void testSmoothExceptsOnZeroWindowSize() {
        exception.expect(IllegalArgumentException.class);
        profile.smooth(0);
    }
	
	@Test
    public void testSmoothExceptsOnNegativeWindowSize() {
	    exception.expect(IllegalArgumentException.class);
	    profile.smooth(-1);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#reverse()}.
	 */
	@Test
	public void testReverse() {
	    
	    float[] arr = Arrays.copyOf(data, data.length);
	    
	    for(int i = 0; i < arr.length / 2; i++) {
	        float temp = arr[i];
	        arr[i] = arr[arr.length - i - 1];
	        arr[arr.length - i - 1] = temp;
	    }
	    	    
	    profile.reverse();
	    
	    float[] res = profile.toFloatArray();
	    
	    assertTrue( equals(arr, res));		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSlidingWindowOffsetPositive() throws ProfileException {
		int exp = 10;
		IProfile test = profile.offset(exp);
		int offset = profile.getSlidingWindowOffset(test);
		assertEquals(exp, offset);
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSlidingWindowOffsetNegative() throws ProfileException {
		int exp = -10;
		IProfile test = profile.offset(exp);
		int offset = profile.getSlidingWindowOffset(test);
		assertEquals(exp, offset);
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSlidingWindowOffseZero() throws ProfileException {
		int exp = 0;
		IProfile test = profile.offset(exp);
		int offset = profile.getSlidingWindowOffset(test);
		assertEquals(exp, offset);
	}

	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getLocalMinima(int)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetLocalMinimaInt() throws ProfileException {
	    
	    BooleanProfile b = profile.getLocalMinima(3);
	    assertFalse(b.get(183));
	    assertTrue(b.get(184));
	    assertFalse(b.get(185));
	}
	
	@Test
    public void testGetLocalMinimaIntExceptsOnZeroWindowSize() {
        exception.expect(IllegalArgumentException.class);
        profile.getLocalMinima(0);
    }
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getLocalMinima(int, double)}.
	 */
	@Test
	public void testGetLocalMinimaIntDoubleWithBothPassingThreshold() {
	    BooleanProfile b = profile.getLocalMinima(3, 180);
	    System.out.println(b);
	    assertFalse(b.get(183));
	    assertTrue(b.get(184));
	    assertFalse(b.get(185));
	    
	    assertFalse(b.get(6));
	    assertFalse(b.get(12));
	    assertFalse(b.get(14));
	}
	
	@Test
    public void testGetLocalMinimaIntDoubleWithOnePassingThreshold() {
	    BooleanProfile b = profile.getLocalMinima(3, 6);

        assertTrue(b.get(5));
        assertFalse(b.get(13));
        assertFalse(b.get(4));
        assertFalse(b.get(6));
        assertFalse(b.get(12));
        assertFalse(b.get(14));
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getLocalMaxima(int)}.
	 */
	@Test
    public void testGetLocalMaximaInt() {
                
        BooleanProfile b = profile.getLocalMaxima(3);
        
        assertTrue(b.get(5));
        assertFalse(b.get(4));
        assertFalse(b.get(6));
    }
    
    @Test
    public void testGetLocalMaximaIntExceptsOnZeroWindowSize() {
        exception.expect(IllegalArgumentException.class);
        profile.getLocalMaxima(0);
    }
        
    @Test
    public void testGetLocalMaximaIntDoubleWithBothPassingThreshold() {
    	System.out.println(Arrays.toString(profile.toFloatArray()));
        BooleanProfile b = profile.getLocalMaxima(3, 180);
        System.out.println(b);

        assertFalse(b.get(302));
        assertTrue(b.get(303));
        assertFalse(b.get(304));
        
        assertFalse(b.get(210));
        assertTrue(b.get(211));
        assertFalse(b.get(212));
    }
    
    @Test
    public void testGetLocalMaximaIntDoubleWithOnePassingThreshold() {
    	BooleanProfile b = profile.getLocalMaxima(3, 210);

    	assertFalse(b.get(302));
        assertFalse(b.get(303));
        assertFalse(b.get(304));
        
        assertFalse(b.get(210));
        assertTrue(b.get(211));
        assertFalse(b.get(212));
    }


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSubregion(int, int)}.
	 */
	@Test
	public void testGetSubregionIntInt() {		
		int start = 0;
		int stop  = 3;
		IProfile p = profile.getSubregion(start, stop);
		
		for(int i=start; i<stop; i++){
		    assertEquals(data[i], p.get(i), 0);
		}
	}
	
	@Test
    public void testGetSubregionIntIntWraps() {      
        int start = 8;
        int stop  = 2;
        IProfile p = profile.getSubregion(start, stop);
        
        System.out.println(p);
        
        for(int i=start; i<profile.size(); i++){
            System.out.println(i+" : "+data[i]);
            assertEquals(data[i], p.get(i-start), 0);
        }
        for(int i=0; i<stop; i++){
            System.out.println(i+" : "+data[i]);
            assertEquals(data[i], p.get(p.size()-stop+i), 0);
        }

    }
	
	@Test
    public void testGetSubregionIntIntExceptsOnLowerIndexOutOfBounds() {  
	    exception.expect(IllegalArgumentException.class);
	    profile.getSubregion(-1, 3);
	}
	
	@Test
    public void testGetSubregionIntIntExceptsOnUpperIndexOutOfBounds() {  
        exception.expect(IllegalArgumentException.class);
        profile.getSubregion(-1, profile.size()+1);
    }
	
	@Test
    public void testGetSubregionIntIntExceptsOnWrappingLowerIndexOutOfBounds() {  
        exception.expect(IllegalArgumentException.class);
        profile.getSubregion(profile.size()+1, 3);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSubregion(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSubregionIBorderSegment() throws ProfileException {
	    int start = 0;
        int stop  = 3;
        IBorderSegment s = new DefaultBorderSegment(start, stop, data.length);
        IProfile p = profile.getSubregion(s);
        
        for(int i=start; i<stop; i++){
            assertEquals(data[i], p.get(i), 0);
        }
	}
			
	@Test
    public void testGetSubregionIBorderSegmentExceptsOnSegmentOutOfUpperBounds() throws ProfileException{
        IBorderSegment s = new DefaultBorderSegment(0, 100, 200);
        exception.expect(IllegalArgumentException.class);
        profile.getSubregion(s);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#calculateDeltas(int)}.
	 */
	@Test
	public void testCalculateDeltasSucceedsWithWindowSizeOne() {
	    
	    IProfile res = profile.calculateDeltas(1);
//	    { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 }; // template data for a profile
	    float[] exp = { 1f, -9f, -3f,  6f, 17f, 5f, -16f, -3f, 17f, 4f, -14f, -9f, 4f };	    
		assertTrue( equals(exp, res.toFloatArray()));
	}
	
	@Test
	public void testCalculateDeltasSucceedsWithWindowSizeTwo() {

	    IProfile res = profile.calculateDeltas(2);
	    //	      { 10, 5, 1, 2, 7, 19, 12, 3, 9, 20, 13, 6, 4 }; // template data for a profile
	    float[] exp = { -5f, -2f, -3f,  14f, 11f, 1f, 2f, 1f, 1f, 3f, -5f, -10f, -8f };

	    System.out.println(res.toString());
	    assertTrue( equals(exp, res.toFloatArray()));
	}
	
	@Test
    public void testCalculateDeltasExceptsWithWindowSizeZero() {
	    exception.expect(IllegalArgumentException.class);
	    profile.calculateDeltas(0);
	}


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#power(double)}.
	 */
	@Test
	public void testPower() {

		double d = 2;
		float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = (float) Math.pow(data[i],d);
		}
		
		float[] result = profile.power(d).toFloatArray();
		
		assertTrue( equals(exp, result) );
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#absolute()}.
	 */
	@Test
	public void testAbsolute() {
		
		float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = Math.abs(data[i]-180);
		}
		IProfile p = profile.subtract(180);
		float[] result = p.absolute().toFloatArray();
		
		assertTrue( equals(exp, result) );
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#cumulativeSum()}.
	 */
	@Test
	public void testCumulativeSum() {
		float[] exp = new float[data.length];
		exp[0] = data[0];
		for(int i=0, j=1; j<data.length; i++, j++){
			exp[j] = data[j]+exp[i];
		}
				
		float[] result = profile.cumulativeSum().toFloatArray();
		
		assertTrue( equals(exp, result) );
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#multiply(double)}.
	 */
	@Test
	public void testMultiplyDouble() {
		double   constant = 2;

		IProfile result = profile.multiply(constant);
		float[] exp = new float[data.length];

		for(int i=0; i<data.length; i++){
			exp[i] = (float) (data[i]*constant);
		}

		assertTrue( equals(exp, result.toFloatArray()) );
	}
	
	@Test
	public void testMultiplyDoubleNanInputFails() {
	    exception.expect(IllegalArgumentException.class);
	    profile.multiply(Double.NaN);
	}


	@Test
	public void testMultiplyDoublePositiveInfinityInputFails() {
	    exception.expect(IllegalArgumentException.class);
	    profile.multiply(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testMultiplyDoubleNegativeInfinityInputFails() {
	    exception.expect(IllegalArgumentException.class);
	    profile.multiply(Double.NEGATIVE_INFINITY);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#multiply(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testMultiplyIProfile() {

		float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = data[i]*data[i];
		}

		IProfile result = profile.multiply(profile);

		assertTrue( equals(exp, result.toFloatArray()) );
	}
	
	@Test
    public void testMultiplyProfileExceptsOnDifferentLength() {
        float[] f    = { 10, 10 };
        IProfile p2 = new FloatProfile(f);
        exception.expect(IllegalArgumentException.class);
        profile.multiply(p2);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#divide(double)}.
	 */
	@Test
	public void testDivideDouble() {

		double   constant   = 2;		
		float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = (float) (data[i]/constant);
		}

		IProfile result = profile.divide(constant);
		assertTrue( equals(exp, result.toFloatArray()) );	
	}
	
	@Test
    public void testDivideDoubleNegative() {

        double   constant   = -2;
        float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = (float) (data[i]/constant);
		}

        IProfile result = profile.divide(constant);
        assertTrue( equals(exp, result.toFloatArray()) );         
    }
	
	@Test
    public void testDivideDoubleNanInputFails() {
        exception.expect(IllegalArgumentException.class);
        profile.divide(Double.NaN);
    }

	
	@Test
    public void testDivideDoublePositiveInfinityInputFails() {
        exception.expect(IllegalArgumentException.class);
        profile.divide(Double.POSITIVE_INFINITY);
    }
	
	@Test
    public void testDivideDoubleNegativeInfinityInputFails() {
        exception.expect(IllegalArgumentException.class);
        profile.divide(Double.NEGATIVE_INFINITY);
    }
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#divide(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testDivideIProfile() {
		
		float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = data[i]/data[i];
		}

		IProfile result = profile.divide(profile);

		assertTrue( equals(exp, result.toFloatArray()) ); 
	}
	
	@Test
    public void testDivideProfileExceptsOnDifferentLength() {
        float[] f    = { 10, 10 };
        IProfile divider = new FloatProfile(f);
        exception.expect(IllegalArgumentException.class);
        profile.divide(divider);
    }
	
	@Test
    public void testDivideIProfileFailsOnSizeMismatch() {

        float[] div   = {1, 2, 0.5f, 3,  0.25f };
        IProfile divider = new FloatProfile(div);
        exception.expect(IllegalArgumentException.class);
        profile.divide(divider);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#add(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testAddIProfile() {

		float[] exp= new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = data[i]*2;
		}
		
		IProfile p1 = comp.new DefaultProfile(data);
		IProfile p2 = comp.new DefaultProfile(data);
		
		IProfile p3 = p1.add(p2);
		float[] result = p3.toFloatArray();
		
		assertTrue( equals(exp, result) ); 
	}

	@Test
	public void testAddProfileExceptsOnDifferentLength() {
	    float[] f    = { 10, 10 };
	    IProfile p2 = new FloatProfile(f);
	    exception.expect(IllegalArgumentException.class);
	    profile.add(p2);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#add(double)}.
	 */
	@Test
	public void testAddDouble() {

		double   constant   = 2;
		float[] exp = new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = (float) (data[i]+constant);
		}

		IProfile result = profile.add(constant);

		assertTrue( equals( exp, result.toFloatArray()));
	}
	
	@Test
    public void testAddDoubleExceptsOnNan() {
	    exception.expect(IllegalArgumentException.class);
        profile.add(Double.NaN);
	}
	
	@Test
    public void testAddDoubleExceptsOnPositiveInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.add(Double.POSITIVE_INFINITY);
    }
	
	@Test
    public void testAddDoubleExceptsOnNegativeInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.add(Double.NEGATIVE_INFINITY);
    }

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#subtract(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 */
	@Test
	public void testSubtract() {
		
		IProfile p = profile.add(profile);
		p = p.subtract(profile);
		assertTrue( equals(data, p.toFloatArray()) );    		
	}
	
	@Test
    public void testSubtractProfileExceptsOnDifferentLength() {
        float[] f    = { 10, 10 };
        IProfile p2 = new FloatProfile(f);
        exception.expect(IllegalArgumentException.class);
        profile.subtract(p2);
    }
	
	@Test
    public void testSubtractDouble() {

		double sub = 1;
		float[] exp = new float[data.length];
        for(int i=0; i<data.length; i++){
            exp[i] = (float) (data[i]-sub);
        }
        
        IProfile p = profile.subtract(sub);         
        assertTrue( equals(exp, p.toFloatArray()) );
    }
	
	@Test
    public void testSubtractDoubleExceptsOnNan() {
        exception.expect(IllegalArgumentException.class);
        profile.subtract(Double.NaN);
    }
    
    @Test
    public void testSubtractDoubleExceptsOnPositiveInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.subtract(Double.POSITIVE_INFINITY);
    }
    
    @Test
    public void testSubtractDoubleExceptsOnNegativeInfinity() {
        exception.expect(IllegalArgumentException.class);
        profile.subtract(Double.NEGATIVE_INFINITY);
    }

			
	@Test
	public void interpolationShouldLinearExtend() throws Exception{
		float[] data       = { 10, 11, 12, 13, 14, 15 };
		float[] expected   = { 10, 10.5f, 11, 11.5f, 12, 12.5f, 13, 13.5f, 14, 14.5f, 15, 12.5f };
		
		IProfile tester = comp.new DefaultProfile(data);
		IProfile result = tester.interpolate(12);
		float[] output = result.toFloatArray();	
		
		assertTrue( equals(expected, output) );
		
	}
	
	@Test
	public void interpolationShouldShrinkWhenGivenLowerLength() throws ProfileException {
		float[] data       = { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 };
		float[] expected   = { 10, 12, 14, 16, 18, 20 };
		
		IProfile tester = comp.new DefaultProfile(data);
		IProfile result =  tester.interpolate(6);
		
		float[] output = result.toFloatArray();	
		
		assertTrue( equals(expected, output) );

	}
	
	@Test
    public void interpolateExceptsOnLengthZero() throws ProfileException {
	    exception.expect(IllegalArgumentException.class);
        profile.interpolate(0);
	}
	
	@Test
    public void interpolateExceptsOnLengthNegative() throws ProfileException {
        exception.expect(IllegalArgumentException.class);
        profile.interpolate(-1);
    }
	
	
	@Test
	public void squareDiffsAreCalculatedCorrectly() throws ProfileException{
		
		IProfile dataProfile = comp.new DefaultProfile(data);
		
		float[] arr = dataProfile.toFloatArray();
		arr[0] = arr[0]+2;
		IProfile templateProfile = comp.new DefaultProfile(arr);
				
		double expectedDiff = 4;
		double value = dataProfile.absoluteSquareDifference(templateProfile);		
		assertEquals(expectedDiff, value,0);
	}
	
	@Test
	public void testGetWindowWithinCentreOfProfile(){
		int mid = data.length/2;
		IProfile r = profile.getWindow(mid, 2);
        float[] exp = { data[mid-2], data[mid-1], data[mid], data[mid+1], data[mid+2] };
        assertTrue( equals(exp, r.toFloatArray()));   
	}
	
	@Test
    public void testGetWindowAtStartOfProfile(){
        float[] exp = { data[data.length-1], data[0], data[1], data[2], data[3] };
        IProfile r = profile.getWindow(1, 2);        
        assertTrue( equals(exp, r.toFloatArray()));
    }
	
	@Test
    public void testGetWindowAtEndOfProfile(){

        IProfile r = profile.getWindow(data.length-1, 2);
        float[] exp = { data[data.length-3], data[data.length-2], data[data.length-1], data[0], data[1] };
        assertTrue( equals(exp, r.toFloatArray()));        
    }
	
	@Test
    public void testEqualsWithSameObjectRef(){
        assertTrue(profile.equals(profile));
    }
		
	@Test
    public void testEqualsFalseWithNull(){
        assertFalse(profile.equals(null));
    }
	
	@Test
    public void testEqualsFalseWithNonProfile(){
	    Object o = new Object();
        assertFalse(profile.equals(o));
    }
	
	@Test
    public void testEqualsFalseWithDifferentData(){
        float[] d = new float[data.length];
        for(int i=0; i<data.length; i++){
            d[i] = data[i]+1;
        }
        IProfile p = comp.new DefaultProfile(d);
        assertFalse(profile.equals(p));
    }
	
	@Test
    public void testEqualsFalseWithSameDataInDifferentProfileType(){
	    double[] d = new double[data.length];
	    for(int i=0; i<data.length; i++){
	        d[i] = data[i];
	    }
	    IProfile p = new DoubleProfile(d);
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
            equal &= exp[i] == obs[i];
            assertEquals(exp[i], obs[i], 0);
        }
        return equal;
	}

}
