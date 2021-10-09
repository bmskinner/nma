package com.bmskinner.nuclear_morphology.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for profile classes implementing the ISegmentedFloatProfile interface.
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class ISegmentedProfileTester extends ComponentTester {
	
	public final static int profileLength = 330;
	
	private static final String SEG_0 = "00000000-0000-0000-0000-000000000000";
	private static final String SEG_1 = "11111111-1111-1111-1111-111111111111";
	private static final String SEG_2 = "22222222-2222-2222-2222-222222222222";
	private static final String SEG_3 = "33333333-3333-3333-3333-333333333333";
	
	private ISegmentedProfile profile;

	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Parameter(0)
	public Class<? extends ISegmentedProfile> source;

	@Override
	@Before
    public void setUp() throws Exception {
		super.setUp();
		profile = createInstance(source);
    }

	/**
	 * Create an instance of the class under test, using the default index parameters.
	 * Generates a 4-segment profile, one of which wraps
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static ISegmentedProfile createInstance(Class<? extends ISegmentedProfile> source) throws Exception {
		
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		float[] data = new float[comp.getBorderLength()];
		Arrays.fill(data, 1);
		
		List<IProfileSegment> segments = makeTestSegments();
		if(source==SegmentedFloatProfile.class)
			return new SegmentedFloatProfile(new FloatProfile(data), segments);

		throw new Exception("Unable to create instance of "+source);
	}
	
	private static List<IProfileSegment> makeTestSegments() throws ProfileException{
		List<IProfileSegment> list = new ArrayList<>();
		list.add(new DefaultProfileSegment(10, 20, profileLength, UUID.fromString(SEG_0)));
		list.add(new DefaultProfileSegment(20, 100, profileLength, UUID.fromString(SEG_1)));
		list.add(new DefaultProfileSegment(100, 200, profileLength, UUID.fromString(SEG_2)));
		list.add(new DefaultProfileSegment(200, 10, profileLength, UUID.fromString(SEG_3)));
		IProfileSegment.linkSegments(list);
		return list;
	}
		
    @Parameters
    public static Iterable<Class> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(
				SegmentedFloatProfile.class);
	}
	
	@Test
	public void testReverse() throws Exception {
	    ISegmentedProfile testProfile = createInstance(source);
	    profile.reverse();
	    	    
	    for(UUID id : profile.getSegmentIDs()) {
	    	IProfileSegment fwd = testProfile.getSegment(id);
	    	IProfileSegment rev = profile.getSegment(id);
	    	
	    	assertEquals("Segment length",fwd.length(), rev.length());
	    	
	    	int expStart = profile.size()-1-fwd.getEndIndex();
	    	int expEnd  =  profile.size()-1-fwd.getStartIndex();
	    	
	    	assertEquals("Start index",expStart, rev.getStartIndex());
	    	assertEquals("End index",expEnd, rev.getEndIndex());
	    }
	}
	
	@Test
	public void testGetSegments() throws ProfileException {
	    List<IProfileSegment> test = makeTestSegments();
	    List<IProfileSegment> result = profile.getSegments();
	    
	    assertEquals(test.size(), result.size());
	    for(int i=0; i<test.size(); i++){
	        assertEquals(test.get(i).toString(), result.get(i).toString());
	    }

	}
	
	@Test
	public void testGetSegmentUUID() throws ProfileException, MissingComponentException {
	    List<IProfileSegment> test = makeTestSegments();
	    
	    for(IProfileSegment s : test){
	        IProfileSegment seg = profile.getSegment(s.getID());
	        assertEquals(s.toString(), seg.toString());
	    }
	}
	
	
	@Test
    public void testGetSegmentUUIDExceptsOnInvalidId() throws ProfileException, MissingComponentException {
	    UUID notPresent = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(MissingComponentException.class);
        IProfileSegment seg = profile.getSegment( notPresent);
    }

	@Test
	public void testHasSegment() throws ProfileException, MissingComponentException {
	    List<IProfileSegment> test = makeTestSegments();
        
        for(IProfileSegment s : test){
            IProfileSegment seg = profile.getSegment(s.getID());
            assertEquals(s.toString(), seg.toString());
        }
	}

	@Test
	public void testGetSegmentsFrom() throws Exception {
	    
	    List<IProfileSegment> test = makeTestSegments();
	    List<IProfileSegment> result = profile.getSegmentsFrom(UUID.fromString(SEG_1));
	    assertEquals(test.get(1).toString(), result.get(0).toString());
	}
	
	@Test
    public void testGetSegmentsFromExceptsOnInvalidId() throws Exception {
	    UUID notPresent = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(MissingComponentException.class);
        List<IProfileSegment> result = profile.getSegmentsFrom(notPresent);
    }
	
	@Test
	public void testGetOrderedSegmentsSucceedsWhenProfileHasOneSegmentStartingAndEndingAtIndexZero() throws ProfileException {
		ISegmentedProfile testProfile = profile.copy();
		List<IProfileSegment> segments = new ArrayList<>();
		segments.add(new DefaultProfileSegment(0, 0, profile.size(), UUID.fromString(SEG_0)));
		testProfile.setSegments(segments);
		assertTrue(testProfile.getSegmentCount()==1);
		
		 List<IProfileSegment> result = testProfile.getOrderedSegments();
		 assertTrue(result.size()==1);
		 assertEquals(SEG_0, result.get(0).getID().toString());
	}
	
	@Test
	public void testGetOrderedSegmentsSucceedsWhenProfileIsOffset() throws ProfileException {
		
		// Get ordered segments should return the segments from the profile start
		// The segments wrap, so an offset of zero begins with Seg_3.
		// NOTE: This is testing the IDs of the segments are in the correct order. It does not
		// check the segment start and end points are correct. 

		for(int i=0; i<10; i++) { // offset up to the end of segment 3
			List<IProfileSegment> test = makeTestSegments();
			test.add(test.get(0));
			test.remove(0);
			test.add(test.get(0));
			test.remove(0);
			test.add(test.get(0));
			test.remove(0);
			
			ISegmentedProfile testProfile = profile.offset(i);
		    List<IProfileSegment> result = testProfile.getOrderedSegments();
		    for(int j=0; j<test.size(); j++){
	            assertEquals("Offset "+i+": segment "+j,test.get(j).getID(), result.get(j).getID());
	        }
		}
		
		for(int i=10; i<20; i++) { // offset up to the end of segment 0
			List<IProfileSegment> test = makeTestSegments();
		   
			ISegmentedProfile testProfile = profile.offset(i);
		    List<IProfileSegment> result = testProfile.getOrderedSegments();
		    for(int j=0; j<test.size(); j++){
	            assertEquals("Offset "+i+": segment "+j,test.get(j).getID(), result.get(j).getID());
	        }
		}
		
		for(int i=20; i<100; i++) { // offset up to the end of segment 1
			List<IProfileSegment> test = makeTestSegments();
		    test.add(test.get(0));
		    test.remove(0);
			ISegmentedProfile testProfile = profile.offset(i);
		    List<IProfileSegment> result = testProfile.getOrderedSegments();
		    for(int j=0; j<test.size(); j++){
	            assertEquals("Offset "+i+": segment "+j,test.get(j).getID(), result.get(j).getID());
	        }
		}   
	    
	}

	@Test
	public void testGetSegmentString() throws MissingComponentException {
	    IProfileSegment test = new DefaultProfileSegment(10, 20, profileLength, UUID.fromString(SEG_0));
	    String name = "Seg_0";
	    IProfileSegment result = profile.getSegment(name);
	    assertEquals(test.getID(), result.getID());
        assertEquals(test.getStartIndex(), result.getStartIndex());
        assertEquals(test.getEndIndex(), result.getEndIndex());
	}
	
	@Test
    public void testGetSegmentStringExceptsWithInvalidInput() throws MissingComponentException{
        exception.expect(MissingComponentException.class);
        profile.getSegment("Not present");
    }

	@Test
	public void testGetSegmentIBorderSegment() {
	    IProfileSegment test = new DefaultProfileSegment(10, 20, profileLength, UUID.fromString(SEG_0));
	    IProfileSegment result = profile.getSegment(test);
	    assertEquals(test.getID(), result.getID());
	    assertEquals(test.getStartIndex(), result.getStartIndex());
	    assertEquals(test.getEndIndex(), result.getEndIndex());
	}

	@Test
	public void testGetSegmentAt() {
	    IProfileSegment s = profile.getSegmentAt(1);
	    assertEquals(UUID.fromString(SEG_1), s.getID());
	}
	
	@Test
	public void testGetSegmentAtExceptsWithInvalidInputLow(){
	    exception.expect(IllegalArgumentException.class);
	    profile.getSegmentAt(-1);
	}
	
	@Test
	public void testGetSegmentAtExceptsWithInvalidInputHigh(){
	    exception.expect(IllegalArgumentException.class);
	    profile.getSegmentAt(5);
	}
	
	@Test
	public void testGetSegmentContaining() throws ProfileException {
//		10-20-45-89-10
		IProfileSegment s = profile.getSegmentContaining(0);
		assertEquals(UUID.fromString(SEG_3), s.getID());

		profile = profile.offset(25);
		// Should now be 85-95-20-64-85
		s = profile.getSegmentContaining(0);
		assertEquals(UUID.fromString(SEG_1), s.getID());
	}
	
	@Test
	public void testGetSegmentContainingStartOutOfBounds(){
		exception.expect(IllegalArgumentException.class);
		profile.getSegmentContaining(-5);
	}
	
	@Test
	public void testGetSegmentContainingEndOutOfBounds(){
		exception.expect(IllegalArgumentException.class);
		profile.getSegmentContaining(profile.size()+1);
	}
	
	@Test
	public void testSetNonWrappingSegments() throws ProfileException {
	    
	    List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(0,  10, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(10, 20, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(20, 60, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(60, 90, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(90, 0,  profile.size(), UUID.randomUUID()));
        IProfileSegment.linkSegments(list);
        profile.setSegments(list);
        
        for(int i=0; i<list.size(); i++){
            assertEquals(list.get(i).toString(), profile.getSegmentAt(i).toString());
        }
	}
	
	@Test
	public void testSetWrappingSegments() throws ProfileException {
	    
	    List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(5,  10, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(10, 20, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(20, 60, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(60, 90, profile.size(), UUID.randomUUID()));
        list.add(new DefaultProfileSegment(90, 5,  profile.size(), UUID.randomUUID()));
        IProfileSegment.linkSegments(list);
        profile.setSegments(list);
        
        for(int i=0; i<list.size(); i++){
            assertEquals(list.get(i).toString(), profile.getSegmentAt(i).toString());
        }
	}

	@Test
	public void testSetSegmentsExceptsOnEmptyList() {
	    exception.expect(IllegalArgumentException.class);
	    profile.setSegments(new ArrayList<IProfileSegment>(0));
	}
		
	@Test
    public void testSetSegmentsExceptsOnListWithDifferentProfileLength() throws ProfileException {
	    List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(5,  10, 110, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(10, 20, 110, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(20, 60, 110, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(60, 90, 110, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(90, 5,  110, UUID.randomUUID()));
        IProfileSegment.linkSegments(list);
        exception.expect(IllegalArgumentException.class);
        profile.setSegments(list);
    }

	

	@Test
	public void testGetSegmentNames() {
	    
	    List<String> test = new ArrayList<>();
	    for(int i=0; i<profile.getSegmentCount(); i++){
	        test.add("Seg_"+i);
	    }
	    
	    List<String> result = profile.getSegmentNames();
	    
	    assertEquals(test.size(), result.size()); 
	    for(int i=0; i<test.size(); i++){
	        assertEquals(test.get(i), result.get(i));
	    }
	}

	@Test
	public void testGetSegmentNamesReturnsEmptyListWhenNoSegments() {
		profile.clearSegments();
	    List<String> result = profile.getSegmentNames();
	    assertTrue(result.size()==1); // A profile always has a default segment
	}

	@Test
	public void testGetSegmentIDs() throws ProfileException {
	    List<IProfileSegment> test = makeTestSegments();
	    List<UUID> result = profile.getSegmentIDs();
	    assertEquals(test.size(), result.size());
	    for(int i=0; i<result.size(); i++){
            assertEquals(test.get(i).getID(), result.get(i));
        }
	}
	
	@Test
	public void testGetSegmentCount() throws ProfileException {   
	    List<IProfileSegment> list = makeTestSegments();
	    assertEquals(list.size(), profile.getSegmentCount());  
	}
	
	@Test
	public void testClearSegments() throws ProfileException {   
	    profile.clearSegments();
	 // A profile always has a default segment
	    assertEquals(1, profile.getSegmentCount());
	    assertEquals(IProfileCollection.DEFAULT_SEGMENT_ID, profile.getSegmentAt(0).getID());
	}
	
	@Test
	public void testGetDisplacement() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    double d = profile.getDisplacement(s1);
	    assertEquals(0, d, 0);
	}
	
	@Test
    public void testGetDisplacementExceptsOnInvalidSegment() {
	    IProfileSegment s1 = new DefaultProfileSegment(5,  10, 110, UUID.randomUUID());
        exception.expect(IllegalArgumentException.class);
        profile.getDisplacement(s1);
    }

	@Test
	public void testContains() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
		assertTrue(profile.contains(s1));
	}
	
	@Test
    public void testContainsReturnsFalseWhenSegmentNotPresent() {
	    IProfileSegment s1 = new DefaultProfileSegment(5,  10, 110, UUID.randomUUID());
        assertFalse(profile.contains(s1));
    }
	    
    @Test
    public void testNudgeSegmentsWithZeroOffset() throws ProfileException, MissingComponentException {
        testSegmentOffset(0);
    }

	@Test
	public void testNudgeSegmentsWithPositiveOffset() throws ProfileException, MissingComponentException {
	    testSegmentOffset(10);
	}
	
	@Test
    public void testNudgeSegmentsWithNegativeOffset() throws ProfileException, MissingComponentException {
	    testSegmentOffset(-10);
    }
	
	@Test
    public void testNudgeSegmentsWithPositiveOffsetLargerThanProfile() throws ProfileException, MissingComponentException {
        testSegmentOffset(profile.size()+1);
    }
	
	@Test
    public void testNudgeSegmentsWithNegativeOffsetLargerThanProfile() throws ProfileException, MissingComponentException {
        testSegmentOffset(-(profile.size()+1));
    }
	
	/**
	 * Test if the given offset is successfully applied during a nudge
	 * @param offset
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	private void testSegmentOffset(int offset) throws ProfileException, MissingComponentException{
        List<IProfileSegment> test = makeTestSegments();
        profile.nudgeSegments(offset); 
         for(IProfileSegment s : test){
             assertTrue(segmentHasOffset(s, profile.getSegment(s.getID()), offset));
         }
    }
	
	/**
	 * Test if the result segment is the input segment offset by the given amount 
	 * @param input the initial segment 
	 * @param result the segment after of setting
	 * @param offset the expected offset
	 * @return true if the result segment is the input segment offset by the given amount
	 */
	private boolean segmentHasOffset(IProfileSegment input, IProfileSegment result, int offset){
	    
	    if(input.getProfileLength()!=result.getProfileLength())
	        return false;
	    
	    if(input.length()!=result.length())
	        return false;

	    int start = input.getStartIndex();
	    int end   = input.getEndIndex();
	    
	    int startOffset = CellularComponent.wrapIndex(start+offset, input.getProfileLength());
	    int endOffset   = CellularComponent.wrapIndex(end+offset, input.getProfileLength());
	    
	    if(startOffset!=result.getStartIndex())
	        return false;
	    
	    if(endOffset!=result.getEndIndex())
	        return false;
	    
	    return true;
	}
	
	@Test
	public void testPositiveAndNegativeOffsets() throws Exception {
		for(int offset=-profile.size()*2; offset<profile.size()*2; offset++) {
			ISegmentedProfile originalProfile = createInstance(source);
			for(UUID id : profile.getSegmentIDs()) {
				testSegmentOffset(id, offset, originalProfile);
			}
		}
	}
		
	/**
	 * Test a segment offset.
	 * 
	 * When profiles are offset, the segments must take a negative offset. 
	 * @param segId
	 * @param offset
	 * @throws Exception
	 */
	private void testSegmentOffset(UUID segId, int offset, ISegmentedProfile p) throws Exception {
		int oldStart = p.getSegment(segId).getStartIndex();
		int oldEnd   = p.getSegment(segId).getEndIndex();
		int expStart = p.wrap(oldStart-offset);
		int expEnd   = p.wrap(oldEnd-offset);
		ISegmentedProfile offsetProfile = p.offset(offset);
		int newStart = offsetProfile.getSegment(segId).getStartIndex();
		int newEnd   = offsetProfile.getSegment(segId).getEndIndex();
		assertEquals("Old start "+oldStart+" + "+offset, expStart, newStart);
		assertEquals("Old end "+oldEnd+" + "+offset, expEnd, newEnd);
	}
	
	@Test
	public void testOffsetOfMultiSegmentProfile() throws Exception {

		// Test that offsetting the profile offsets each individual segment properly.
		// The difference to the test above is that the profile is not explicitly copied.
		// Note that this SHOULD NOT make a difference because ISegmentedProfile::offset()
		// returns a new profile; it does not change the template profile state
		// This test was added to diagnose a bug in the SegmentedCellularComponent
		
		for(int i=-profile.size(); i<profile.size()*2; i++) {

			ISegmentedProfile testProfile = profile.offset(i);

			 List<IProfileSegment> testSegments = testProfile.getSegments();

			 List<IProfileSegment> expectedSegments = makeTestSegments();
			 
			 for(IProfileSegment testSeg : testSegments){
				 UUID segId = testSeg.getID();
				 IProfileSegment expSeg = expectedSegments.stream().filter(s->s.getID().equals(segId)).findFirst().orElseThrow(Exception::new);
				 expSeg.offset(-i);
				 assertEquals("Index "+i, expSeg.toString(), testSeg.toString());
			 }

		}
	}
	
	@Test
	public void testOffsetOfMultiSegmentProfileIsReversible() throws Exception {
		
		for(int i=-profile.size(); i<profile.size()*2; i++) {

			ISegmentedProfile testProfile = profile.offset(i).offset(-i);
			 List<IProfileSegment> testSegments = testProfile.getSegments();

			 List<IProfileSegment> expectedSegments = makeTestSegments();
			 
			 for(IProfileSegment testSeg : testSegments){
				 UUID segId = testSeg.getID();
				 IProfileSegment expSeg = expectedSegments.stream().filter(s->s.getID().equals(segId)).findFirst().orElseThrow(Exception::new);
				 assertEquals("Index "+i, expSeg.toString(), testSeg.toString());
			 }

		}
	}
	
	@Test
	public void testOffsetOfSingleSegmentProfile() throws Exception {

		for(int i=-profile.size(); i<profile.size()*2; i++) {
			profile.clearSegments();
			assertEquals(1, profile.getSegmentCount());
			
			ISegmentedProfile testProfile = profile.offset(i);

			IProfileSegment testSegment = testProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);

			assertEquals("Offset profile by "+i, CellularComponent.wrapIndex(-i, testProfile.size()), testSegment.getStartIndex());
		}
	}
	
	@Test
	public void testOffsetOfSingleSegmentProfileIsReversible() throws Exception {

		for(int i=-profile.size(); i<profile.size()*2; i++) {
			profile.clearSegments();
			assertEquals(1, profile.getSegmentCount());
			int exp = profile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID).getStartIndex();
			ISegmentedProfile testProfile = profile.offset(i).offset(-i);
			IProfileSegment testSegment = testProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
			assertEquals("Offset of "+i+" applied and reversed", exp, testSegment.getStartIndex());
		}
	}
		
	@Test
	public void testSegmentTotalLengthsMatchProfileSize() {
		for(IProfileSegment s : profile.getSegments()){
            assertEquals(s.getName(), profile.size(), s.getProfileLength());
        }
	}
	
	/**
	 * Create a new profile with the same size, segment lengths and positions
	 * as the template. Test that franken-profiling has no effect.
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	@Test
	public void testFrankenNormaliseToProfileHsaNoEffectWhenProfilesAreIdentical() throws ProfileException, MissingComponentException {
		List<IProfileSegment> list = makeTestSegments();
        ISegmentedProfile test = new SegmentedFloatProfile(new FloatProfile(10, profileLength), list);

        ISegmentedProfile result = profile.frankenNormaliseToProfile(test);
        
        assertEquals(result.size(), profileLength);
        assertEquals(test.size(), profileLength);

        for(IProfileSegment s : test.getSegments()){
            assertEquals(s.getName(), s.getDetail(), result.getSegment(s.getID()).getDetail());
        }
	}
			
	/**
	 * Create a new profile with the same size and segment lengths as the template, 
	 * but different positions. Test that franken-profiling generates equal
	 * segment boundaries
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	@Test
	public void testFrankenNormaliseToProfileWorksWhenProfilesAreSameLength() throws ProfileException, MissingComponentException {

		List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(5,  10, profileLength, UUID.fromString(SEG_0)));
        list.add(new DefaultProfileSegment(10, 20, profileLength, UUID.fromString(SEG_1)));
        list.add(new DefaultProfileSegment(20, 60, profileLength, UUID.fromString(SEG_2)));
        list.add(new DefaultProfileSegment(60, 5, profileLength,  UUID.fromString(SEG_3)));
        IProfileSegment.linkSegments(list);
        ISegmentedProfile test = new SegmentedFloatProfile(new FloatProfile(10, profileLength), list);

        ISegmentedProfile result = profile.frankenNormaliseToProfile(test);
        
        assertEquals(result.size(), profileLength);
        assertEquals(test.size(), profileLength);

        for(IProfileSegment s : test.getSegments()){
            assertEquals(s.getName(), s.getDetail(), result.getSegment(s.getID()).getDetail());
        }
	}
	
	/**
	 * Create a new profile as the template, longer than the test profile. Test that 
	 * franken-profiling generates equivalent segment boundaries
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	@Test
	public void testFrankenNormaliseToProfileWorksWhenTargetProfileIsLonger() throws ProfileException, MissingComponentException {
		int targetLength = profileLength+50;
		List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(5,  10, targetLength, UUID.fromString(SEG_0)));
        list.add(new DefaultProfileSegment(10, 20, targetLength, UUID.fromString(SEG_1)));
        list.add(new DefaultProfileSegment(20, 60, targetLength, UUID.fromString(SEG_2)));
        list.add(new DefaultProfileSegment(60, 5, targetLength,  UUID.fromString(SEG_3)));
        IProfileSegment.linkSegments(list);
        ISegmentedProfile test = new SegmentedFloatProfile(new FloatProfile(10, targetLength), list);

        ISegmentedProfile result = profile.frankenNormaliseToProfile(test);
        
        assertEquals(result.size(), targetLength);
        assertEquals(test.size(), targetLength);

        for(IProfileSegment s : test.getSegments()){
            assertEquals(s.getName(), s.getDetail(), result.getSegment(s.getID()).getDetail());
        }
	}
	
	/**
	 * Create a new profile as the template, shorter than the test profile. Test that 
	 * franken-profiling generates equivalent segment boundaries
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	@Test
	public void testFrankenNormaliseToProfileWorksWhenTargetProfileIsShorter() throws ProfileException, MissingComponentException {
		int targetLength = profileLength-50;
		List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(5,  10, targetLength, UUID.fromString(SEG_0)));
        list.add(new DefaultProfileSegment(10, 20, targetLength, UUID.fromString(SEG_1)));
        list.add(new DefaultProfileSegment(20, 60, targetLength, UUID.fromString(SEG_2)));
        list.add(new DefaultProfileSegment(60, 5, targetLength,  UUID.fromString(SEG_3)));
        IProfileSegment.linkSegments(list);
        ISegmentedProfile test = new SegmentedFloatProfile(new FloatProfile(10, targetLength), list);

        ISegmentedProfile result = profile.frankenNormaliseToProfile(test);
        
        assertEquals(result.size(), targetLength);
        assertEquals(test.size(), targetLength);

        for(IProfileSegment s : test.getSegments()){
            assertEquals(s.getName(), s.getDetail(), result.getSegment(s.getID()).getDetail());
        }
	}
		
	
	@Test
    public void testFrankenNormaliseToProfileExceptsOnDifferentSegmentCountTemplate() throws ProfileException {
        exception.expect(IllegalArgumentException.class);
        profile.frankenNormaliseToProfile( new SegmentedFloatProfile(new FloatProfile(10, 100)));
    }
	
	@Test
    public void testFrankenNormaliseToProfileExceptsOnDifferentSegmentIdTemplate() throws ProfileException {  
	    List<IProfileSegment> list = new ArrayList<>();
        list.add(new DefaultProfileSegment(5,  10, 100, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(10, 20, 100, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(20, 60, 100, UUID.randomUUID()));
        list.add(new DefaultProfileSegment(60, 5, 100,  UUID.randomUUID()));
        IProfileSegment.linkSegments(list);
        exception.expect(IllegalArgumentException.class);
        profile.frankenNormaliseToProfile( new SegmentedFloatProfile(new FloatProfile(10, 100), list));
    }
	
	@Test
	public void testMergeSegments() throws ProfileException, MissingComponentException {
	    
	    int expCount = profile.getSegmentCount()-1;
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    IProfileSegment s2 = profile.getSegmentAt(2);
	    
	    profile.mergeSegments(s1, s2, mergedId);
	    
        List<UUID> segIds = profile.getSegmentIDs();
        
        assertFalse(segIds.contains(s1.getID()));
        assertFalse(segIds.contains(s2.getID()));
        assertTrue(segIds.contains(mergedId));
        
        IProfileSegment m = profile.getSegment(mergedId);
        assertTrue(m.hasMergeSources());
        assertTrue(m.hasMergeSource(s1.getID()));
        assertTrue(m.hasMergeSource(s2.getID()));

	    
	    assertEquals("Merged segment start", s1.getStartIndex(), m.getStartIndex());
	    assertEquals("Merged segment end", s2.getEndIndex(), m.getEndIndex());
	    assertEquals("Number of segments", expCount, profile.getSegmentCount());
	}
	
	@Test
	public void testMergeSegmentsWorksForAllPairsNotSpanningRP() throws Exception {
		int expCount = profile.getSegmentCount()-1;
		
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    
	    for(int i=0, j=1; j<profile.getSegmentCount(); i++, j++) {
	    	profile = createInstance(source);
	    	IProfileSegment s1 = profile.getSegmentAt(i);
		    IProfileSegment s2 = profile.getSegmentAt(j);
		    profile.mergeSegments(s1, s2, mergedId);
		    
	        List<UUID> segIds = profile.getSegmentIDs();
	        
	        assertFalse("Pair "+i+"-"+j, segIds.contains(s1.getID()));
	        assertFalse("Pair "+i+"-"+j, segIds.contains(s2.getID()));
	        assertTrue("Pair "+i+"-"+j, segIds.contains(mergedId));
	        
	        IProfileSegment m = profile.getSegment(mergedId);
	        assertTrue("Pair "+i+"-"+j, m.hasMergeSources());
	        assertTrue("Pair "+i+"-"+j, m.hasMergeSource(s1.getID()));
	        assertTrue("Pair "+i+"-"+j, m.hasMergeSource(s2.getID()));

		    
		    assertEquals("Merged segment start", s1.getStartIndex(), m.getStartIndex());
		    assertEquals("Merged segment end", s2.getEndIndex(), m.getEndIndex());
		    assertEquals("Number of segments", expCount, profile.getSegmentCount());
	    }
	    
	}
					
	@Test
    public void testMergeSegmentsExceptsWhenSegment1NotInProfile() throws ProfileException, MissingComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IProfileSegment s1 = new DefaultProfileSegment(5,  10, 110, UUID.randomUUID());
        IProfileSegment s2 = profile.getSegmentAt(2);

        exception.expect(IllegalArgumentException.class);
        profile.mergeSegments(s1, s2, mergedId);
    }
	
	@Test
    public void testMergeSegmentsExceptsWhenSegment2NotInProfile() throws ProfileException, MissingComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IProfileSegment s1 = profile.getSegmentAt(1);
        IProfileSegment s2 = new DefaultProfileSegment(5,  10, 110, UUID.randomUUID());

        exception.expect(IllegalArgumentException.class);
        profile.mergeSegments(s1, s2, mergedId);
    }

	@Test
	public void testUnmergeSegment() throws ProfileException, MissingComponentException {
	    
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IProfileSegment s1 = profile.getSegmentAt(1);
        IProfileSegment s2 = profile.getSegmentAt(2);
        
        int expCount = profile.getSegmentCount();
        
        profile.mergeSegments(s1, s2, mergedId);
        
        assertEquals(expCount-1, profile.getSegmentCount());
                        
		profile.unmergeSegment(mergedId);
		
		assertEquals(expCount, profile.getSegmentCount());
		
		List<UUID> newIds = profile.getSegmentIDs();
		assertTrue(newIds.contains(s1.getID()));
		assertTrue(newIds.contains(s2.getID()));
		assertFalse(newIds.contains(mergedId));
		
		assertEquals(s1.toString(), profile.getSegmentAt(1).toString());
		assertEquals(s2.toString(), profile.getSegmentAt(2).toString());
		
		assertEquals("Original segment start", s1.getStartIndex(), profile.getSegmentAt(1).getStartIndex());
	    assertEquals("Original segment end", s2.getEndIndex(), profile.getSegmentAt(2).getEndIndex());
	    assertEquals("Number of segments", expCount, profile.getSegmentCount());
	}

	@Test
	public void testUnmergeSegmentDoesNothingWhenNoMergedSegments() throws MissingComponentException, ProfileException {
        IProfileSegment s1 = profile.getSegmentAt(1);   
        profile.unmergeSegment(s1);
	}
		
	@Test
    public void testIsSplittable() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
        assertTrue(profile.isSplittable(s1.getID(), s1.getMidpointIndex()));
    }
	
	@Test
	public void testIsSplittableReturnsFalseWhenTooCloseToStart() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    assertFalse(profile.isSplittable(s1.getID(), s1.getStartIndex()+1));
	}

	@Test
	public void testIsSplittableReturnsFalseWhenTooCloseToEnd() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    assertFalse(profile.isSplittable(s1.getID(), s1.getEndIndex()-1));
	}

	@Test
	public void testIsSplittableReturnsFalseOnOutOfBoundsIndex() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    assertFalse(profile.isSplittable(s1.getID(), s1.getEndIndex()+1));
	}

	@Test
	public void testIsSplittableExceptsOnOutOfBoundsProfileIndex() {
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    assertFalse(profile.isSplittable(s1.getID(), profile.size()+1));
	}

	@Test
	public void testSplitSegment() throws ProfileException {
	    IProfileSegment s1 = profile.getSegmentAt(1);
	    
	    int start = s1.getStartIndex();
	    int mid = s1.getMidpointIndex();
	    int end = s1.getEndIndex();
	    
	    UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
	    
	    profile.splitSegment(s1, mid, id1, id2);
	    
	    IProfileSegment r1 = profile.getSegmentAt(1);
	    IProfileSegment r2 = profile.getSegmentAt(2);
	    
	    assertEquals(id1, r1.getID());
	    assertEquals(id2, r2.getID());
	    
	    assertEquals("r1 start",start, r1.getStartIndex());
	    assertEquals("r1 end", mid, r1.getEndIndex());
	    assertEquals("r2 start", mid, r2.getStartIndex());
	    assertEquals("r2 end", end, r2.getEndIndex());
	}
	
	@Test
    public void testSplitSegmentExceptsOnInvalidIndex() throws ProfileException {
        IProfileSegment s1 = profile.getSegmentAt(1);
        
        int indexTooCloseToStart = s1.getStartIndex()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        profile.splitSegment(s1, indexTooCloseToStart, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnIndexOutOfSegmentBounds() throws ProfileException {
        IProfileSegment s1 = profile.getSegmentAt(1);
        
        int mid = profile.size()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        profile.splitSegment(s1, mid, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnInvalidSegment() throws ProfileException {

	    IProfileSegment s1 = new DefaultProfileSegment(5,  10, 110, UUID.randomUUID());
        int mid = s1.getStartIndex()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        profile.splitSegment(s1, mid, id1, id2);
    }
	
	@Test
	public void testValueString() { 
	    StringBuilder builder = new StringBuilder();
        for (int i = 0; i < profile.size(); i++) {
            builder.append("Index " + i + "\t" + profile.get(i) + "\r\n");
        }
        assertEquals(builder.toString(), profile.valueString());
	}

	@Test
	public void testCopy() throws ProfileException, MissingComponentException {
	    ISegmentedProfile copy = profile.copy();
	    assertEquals(profile.getSegmentCount(), copy.getSegmentCount());
	    for (UUID id : profile.getSegmentIDs()) {
	        assertEquals(profile.getSegment(id).getDetail(), copy.getSegment(id).getDetail());
	    }  
		assertEquals(profile.valueString(), copy.valueString());
	}

	@Test
	public void testInterpolate() throws ProfileException {
	    
	    ISegmentedProfile result = profile.interpolate(profile.size()*2);
	    
	    assertEquals(profile.getSegmentCount(), result.getSegmentCount());
	    for (int i=0; i < profile.getSegmentCount(); i++) {
	        assertEquals(profile.getSegmentAt(i).getStartIndex()*2, result.getSegmentAt(i).getStartIndex());
	    }  
	}
	
	@Test
    public void testInterpolateExceptsWhenLessThanZero() throws ProfileException {
	    exception.expect(IllegalArgumentException.class);
        profile.interpolate(-1);
    }
	
	@Test
    public void testInterpolateInSegmentWithNoMergeSources() throws ProfileException {
		profile.clearSegments();
		assertEquals(1, profile.getSegmentCount());
		ISegmentedProfile result = profile.interpolate(profile.size()*2);
		assertEquals(profile.getSegmentCount(), result.getSegmentCount());
	    for (int i=0; i<profile.getSegmentCount(); i++) {
	        assertEquals("New start index",profile.getSegmentAt(i).getStartIndex()*2, result.getSegmentAt(i).getStartIndex());
	    }
	}
	
	@Test
    public void testUpdateStartAndEnd() throws SegmentUpdateException, MissingComponentException {
		IProfileSegment seg0 = profile.getSegment(UUID.fromString(SEG_0));
		
		int oldStart = seg0.getStartIndex();
		int newStart = oldStart+10;
		
		int oldEnd = seg0.getEndIndex();
		int newEnd = oldEnd+10;
		assertTrue(profile.update(seg0, newStart, newEnd));
		
		IProfileSegment updated = profile.getSegment(UUID.fromString(SEG_0));
		assertEquals(newStart, updated.getStartIndex());
		assertEquals(newEnd, updated.getEndIndex());
				
		IProfileSegment prev = updated.prevSegment();
		IProfileSegment next = updated.nextSegment();
		
		assertEquals("Previous segmented updated", newStart, prev.getEndIndex());
		assertEquals("Next segmented updated", newEnd, next.getStartIndex());
		
	}
	
	@Test
    public void testUpdateStartOnly() throws SegmentUpdateException, MissingComponentException {
		IProfileSegment seg0 = profile.getSegment(UUID.fromString(SEG_0));
		
		int oldStart = seg0.getStartIndex();
		int newStart = oldStart+10;
		
		int oldEnd = seg0.getEndIndex();

		assertTrue(profile.update(seg0, newStart, oldEnd));
		
		IProfileSegment updated = profile.getSegment(UUID.fromString(SEG_0));
		assertEquals(newStart, updated.getStartIndex());
		assertEquals(oldEnd, updated.getEndIndex());
		
		IProfileSegment prev = updated.prevSegment();
		IProfileSegment next = updated.nextSegment();
		
		assertEquals("Previous segmented updated", newStart, prev.getEndIndex());
		assertEquals("Next segmented updated", oldEnd, next.getStartIndex());
	}
	
	@Test
    public void testUpdateEndOnly() throws SegmentUpdateException, MissingComponentException {
		IProfileSegment seg0 = profile.getSegment(UUID.fromString(SEG_0));
		
		int oldStart = seg0.getStartIndex();
		
		int oldEnd = seg0.getEndIndex();
		int newEnd = oldEnd+10;
		assertTrue(profile.update(seg0, oldStart, newEnd));
		
		IProfileSegment updated = profile.getSegment(UUID.fromString(SEG_0));
		assertEquals(oldStart, updated.getStartIndex());
		assertEquals(newEnd, updated.getEndIndex());
		
		IProfileSegment prev = updated.prevSegment();
		IProfileSegment next = updated.nextSegment();
		
		assertEquals("Previous segmented updated", oldStart, prev.getEndIndex());
		assertEquals("Next segmented updated", newEnd, next.getStartIndex());
	}

	@Test
    public void testUpdateFailsOnOutOfBoundsStart() throws SegmentUpdateException, MissingComponentException {
		IProfileSegment seg0 = profile.getSegment(UUID.fromString(SEG_0));
		
		int oldStart = seg0.getStartIndex();
		int oldEnd = seg0.getEndIndex();
		
		// Too close to prev start for update to succeed
		try {
			int newStart = CellularComponent.wrapIndex(seg0.prevSegment().getStartIndex()+1, profile.size());
			profile.update(seg0, newStart, oldEnd);
			fail("Invalid segment update did not except");
		} catch(SegmentUpdateException e) {
//			System.out.println("Expected exception caught: "+e.getMessage());
		}
		
		// Confirm nothing happened
		IProfileSegment updated = profile.getSegment(UUID.fromString(SEG_0));
		assertEquals(oldStart, updated.getStartIndex());
		assertEquals(oldEnd, updated.getEndIndex());
		
		IProfileSegment prev = updated.prevSegment();
		IProfileSegment next = updated.nextSegment();
		
		assertEquals("Previous segmented updated", oldStart, prev.getEndIndex());
		assertEquals("Next segmented updated", oldEnd, next.getStartIndex());
	}	
	
	@Test
    public void testUpdateExceptsOnOutOfBoundsEnd() throws SegmentUpdateException, MissingComponentException {
		IProfileSegment seg0 = profile.getSegment(UUID.fromString(SEG_0));
		
		int oldStart = seg0.getStartIndex();
		int oldEnd = seg0.getEndIndex();
		
		// Too close to next end for update to succeed
		try {
			int newEnd = CellularComponent.wrapIndex(seg0.nextSegment().getEndIndex()-1, profile.size());
			profile.update(seg0, oldStart, newEnd);
			fail("Invalid segment update did not except");
		} catch(SegmentUpdateException e) {
//			System.out.println("Expected exception caught: "+e.getMessage());
		}
		
		// Confirm nothing happened
		IProfileSegment updated = profile.getSegment(UUID.fromString(SEG_0));
		assertEquals(oldStart, updated.getStartIndex());
		assertEquals(oldEnd, updated.getEndIndex());
		
		IProfileSegment prev = updated.prevSegment();
		IProfileSegment next = updated.nextSegment();
		
		assertEquals("Previous segmented updated", oldStart, prev.getEndIndex());
		assertEquals("Next segmented updated", oldEnd, next.getStartIndex());
	}
}
