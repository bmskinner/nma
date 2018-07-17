package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultProfile;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile.BorderSegmentTree;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for segment classes implementing the IBorderSegment interface.
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class IProfileTester {
	
	private IProfile profile;
	private static float[] data;

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Parameter(0)
	public Class<? extends IProfile> source;

	@Before
    public void setUp() throws Exception {
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		data = new float[comp.getBorderLength()];
		for(int i=0; i<data.length; i++) {
			data[i] = (float) ((Math.sin(Math.toRadians(i))+1)*180);
		}
        profile = createInstance(source);
    }

	/**
	 * Create an instance of the class under test, using the default index parameters.
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static IProfile createInstance(Class source) throws Exception {

		if(source==DefaultProfile.class) {
			DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
			return comp.new DefaultProfile(data);
		}
		
		if(source==FloatProfile.class)
			return new FloatProfile(data);
		
		if(source==DoubleProfile.class) {
			double[] d = new double[data.length];
			for(int i=0; i<data.length; i++) {
				d[i] = data[i];
			}
			return new DoubleProfile(d);
		}
		
		throw new Exception("Unable to create instance of "+source);
	}
	
	@SuppressWarnings("unchecked")
    @Parameters
    public static Iterable<Class> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(
				DefaultProfile.class,
				FloatProfile.class,
				DoubleProfile.class);
	}
	
	@Test
	public void testSize() {
		assertEquals(profile.size(), data.length);
	}
	
	
	@Test
	public void testGetInt() {
		for(int i=0; i<data.length; i++){
			assertEquals( data[i] , profile.get(i), 0);
		}
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

	@Test
	public void testGetMax() {
		DoubleStream ds = IntStream.range(0, data.length)
                .mapToDouble(i -> data[i]);
		assertEquals( ds.max().getAsDouble(), profile.getMax(), 0);
	}

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

	@Test
	public void testGetIndexOfMax() throws ProfileException {
		int exp = IntStream.range(0, data.length)
	    		.boxed().max(Comparator.comparing(profile::get))
	    		.get();
		assertEquals( exp , profile.getIndexOfMax());
	}


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
	
	private void testAbsoluteSquareDifferenceOnSameLengthProfiles(IProfile template, float diff) throws Exception{
	    float[] test =  Arrays.copyOf(template.toFloatArray(), template.size());
        
        test[0] = test[0]+diff;
        
        IProfile p = createInstance(source);
        
        double expDiff = diff*diff;
        
        assertEquals(expDiff, template.absoluteSquareDifference(p), 0);
	}
	
	@Test
    public void testAbsoluteSquareDifferenceOnSameLengthProfilesPositive() throws Exception {
	    testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, 2);
    }
	
	@Test
    public void testAbsoluteSquareDifferenceOnSameLengthProfilesNegative() throws Exception {
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
	 * @throws Exception 
	 */
	@Test
	public void testCopy() throws Exception {
		IProfile p = createInstance(source);
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
		
		float[] exp = new float[data.length];
		
		exp[0] = (data[0] + data[1] + data[2] + data[data.length-1] + data[data.length-2])/5;
		exp[1] = (data[1] + data[2] + data[3] + data[0] + data[data.length-1])/5;

		for(int i=2; i<=data.length-3; i++) {
			
			float r1 = data[i-2];
			float r0 = data[i-1];
			
			float f0 = data[i+1];
			float f1 = data[i+2];
			
			exp[i] = (r1+r0+data[i]+f0+f1)/5;
		}
		
		exp[data.length-2] = (data[data.length-2] + data[data.length-1] + data[0] + data[data.length-3] + data[data.length-4])/5;
		exp[data.length-1] = (data[data.length-3] + data[data.length-2] + data[data.length-1] + data[0] + data[1])/5;
		
	    IProfile p = profile.smooth(2);
	    float[] obs = p.toFloatArray();
	    assertTrue( equals(exp, obs));
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
	

	@Test
	public void testGetSlidingWindowOffsetZero() throws ProfileException {
		int exp = 0;
		IProfile test = profile.offset(exp);
		int offset = profile.getSlidingWindowOffset(test);
		assertEquals(exp, offset);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSlidingWindowOffsetPositive() throws ProfileException {
		
		for(int exp=1; exp<profile.size(); exp++){
			IProfile test = profile.offset(exp);
			int offset = profile.getSlidingWindowOffset(test);
			IProfile recovered = test.offset(-offset);
			assertEquals(exp, offset);
			assertEquals(profile, recovered);
		}
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSlidingWindowOffset(com.bmskinner.nuclear_morphology.components.generic.IProfile)}.
	 * @throws ProfileException 
	 */
	@Test
	public void testGetSlidingWindowOffsetNegative() throws ProfileException {
		
		for(int exp=-1; exp>-profile.size(); exp--){
			IProfile test = profile.offset(exp);
			int offset = profile.getSlidingWindowOffset(test);
			IProfile recovered = test.offset(-offset);
			
			int posExp = profile.size()+exp;
			assertEquals(posExp, offset);
			assertEquals(profile, recovered);
		}
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
	    for(int i=0; i<data.length; i++) {
        	boolean isMax = ((Math.sin(Math.toRadians(i))+1)*180)==0f;
        	assertEquals(isMax, b.get(i));
        }
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
	public void testGetLocalMinimaIntDouble() {
	    BooleanProfile b = profile.getLocalMinima(3, 180);
	    for(int i=0; i<data.length; i++) {
        	boolean isMax = ((Math.sin(Math.toRadians(i))+1)*180)==0f;
        	assertEquals(isMax, b.get(i));
        }
	    
	    b = profile.getLocalMinima(3, -1);
	    for(int i=0; i<data.length; i++) {
        	assertFalse(b.get(i));
        }
	    
	}
	

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getLocalMaxima(int)}.
	 */
	@Test
    public void testGetLocalMaximaInt() {
                
        BooleanProfile b = profile.getLocalMaxima(3);
        
        for(int i=0; i<data.length; i++) {
        	boolean isMax = ((Math.sin(Math.toRadians(i))+1)*180)==360f;
        	assertEquals(isMax, b.get(i));
        }
    }
    
    @Test
    public void testGetLocalMaximaIntExceptsOnZeroWindowSize() {
        exception.expect(IllegalArgumentException.class);
        profile.getLocalMaxima(0);
    }
        
    @Test
    public void testGetLocalMaximaIntDouble() {
        BooleanProfile b = profile.getLocalMaxima(3, 180);
        
        for(int i=0; i<data.length; i++) {
        	boolean isMax = ((Math.sin(Math.toRadians(i))+1)*180)==360f;
        	assertEquals(isMax, b.get(i));
        }
        
	    b = profile.getLocalMaxima(3, 400);
	    for(int i=0; i<data.length; i++) {
        	assertFalse(b.get(i));
        }
    }
    
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultProfile#getSubregion(int, int)}.
	 */
	@Test
	public void testGetSubregionIntInt() {		
		int start = 0;
		int stop  = 3;
		IProfile p = profile.getSubregion(start, stop);
		
		assertEquals(4, p.size());
		
		for(int i=start; i<stop; i++){
		    assertEquals(data[i], p.get(i), 0);
		}
	}
	
	@Test
    public void testGetSubregionIntIntWraps() {      
        int start = data.length-2;
        int stop  = 2;
        IProfile p = profile.getSubregion(start, stop);
        
        assertEquals(5, p.size());
        
        for(int i=0; i<p.size(); i++){
        	int original = i<2 ? start+i : i-2;
        	 assertEquals(data[original], p.get(i), 0);
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
	    
	    double expAt0 = (data[0] - data[data.length-1]) + (data[1] - data[0]);
	    double expAt1 = (data[2] - data[1])  + (data[1] - data[0]);
	    assertEquals(expAt0, res.get(0),0);
	    assertEquals(expAt1, res.get(1),0);
	}
	
	@Test
	public void testCalculateDeltasSucceedsWithWindowSizeTwo() {

		IProfile res = profile.calculateDeltas(2);

		double expAt0 = (data[data.length-1] - data[data.length-2]) 
	    		+ (data[0] - data[data.length-1]) 
	    		+ (data[1] - data[0])
	    		+ (data[2] - data[1]);
	    
	    double expAt1 = (data[0] - data[data.length-1]) 
	    		+ (data[1] - data[0])
	    		+ (data[2] - data[1])
	    		+ (data[3] - data[2]);
	    
	    double expAt2 = (data[1] - data[0]) 
	    		+ (data[2] - data[1])
	    		+ (data[3] - data[2])
	    		+ (data[4] - data[3]);
	    
	    assertEquals(expAt0, res.get(0),0);		
	    assertEquals(expAt1, res.get(1),0);
	    assertEquals(expAt2, res.get(2),0);
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
	 * @throws Exception 
	 */
	@Test
	public void testAddIProfile() throws Exception {

		float[] exp= new float[data.length];
		for(int i=0; i<data.length; i++){
			exp[i] = data[i]*2;
		}
		
		IProfile p1 = createInstance(source);
		IProfile p2 = createInstance(source);
		
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
	public void interpolationShouldLinearExtend() throws Exception {
		IProfile test = profile.interpolate(profile.size()*2);
		assertEquals(profile.size()*2, test.size());
	}
	
	@Test
	public void interpolationShouldShrinkWhenGivenLowerLength() throws ProfileException {
		IProfile test = profile.interpolate(profile.size()/2);
		assertEquals(profile.size()/2, test.size());
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
	public void squareDiffsAreCalculatedCorrectly() throws Exception{
		
		IProfile dataProfile = createInstance(source);
		
		float[] arr = dataProfile.toFloatArray();
		arr[0] = arr[0]+2;
		IProfile templateProfile = createInstance(source);
				
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
    public void testEqualsFalseWithDifferentData() throws Exception{
        float[] d = new float[data.length];
        for(int i=0; i<data.length; i++){
            d[i] = data[i]+1;
        }
        IProfile p = createInstance(source);
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
	
	@Test
	public void testWrapsDoesNotAffectIndexWhenWithinProfile(){
		for(int i=0; i<profile.size(); i++){
			assertEquals(i, profile.wrap(i));
		}
	}
	
	@Test
	public void testWrapsAssignsNegativeIndexesToCorrectProfileIndex(){
		for(int i=-1; i>-profile.size(); i--){
			assertEquals("Testing "+i+" against profile size "+profile.size(), profile.size()+i, profile.wrap(i));
		}
	}
	
	@Test
	public void testWrapsAssignsPositiveIndexesToCorrectProfileIndex(){
		for(int i=profile.size(); i<profile.size()*2; i++){
			assertEquals("Testing "+i+" against profile size "+profile.size(), i-profile.size(), profile.wrap(i));
		}
	}
	
	@Test
	public void testWrapsHasContigouousRange(){
		for(int i=-profile.size(); i<profile.size()*2; i++){
			System.out.println(i+"\t"+profile.wrap(i));
//			assertEquals("Testing "+i+" against profile size "+profile.size(), i-profile.size(), profile.wrap(i));
		}
	}
	
	
	/**
	 * Test float array equality. Not in junit.
	 * @param exp
	 * @param obs
	 */
	public static boolean equals(float[] exp, float[] obs){
	    float epsilon = 0.0001f;
	    boolean equal = true;
	    equal &= obs.length==exp.length;
	    assertEquals(exp.length, obs.length);
        
        for(int i=0; i<exp.length; i++){
            equal &= (Float.isNaN(exp[i]) && Float.isNaN(obs[i])) || Math.abs(exp[i] - obs[i])<=epsilon;
            assertEquals("Index "+i, exp[i], obs[i], epsilon);
        }
        return equal;
	}

}
