package com.bmskinner.nuclear_morphology.components.generic;

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

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile.BorderSegmentTree;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for profile classes implementing the ISegmentedFloatProfile interface.
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class ISegmentedProfileTester {
	
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

	@Before
    public void setUp() throws Exception {
		profile = createInstance(source);
    }

	/**
	 * Create an instance of the class under test, using the default index parameters.
	 * Generates a 4-segment profile, one of which wraps
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static ISegmentedProfile createInstance(Class source) throws Exception {
		
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		float[] data = new float[comp.getBorderLength()];
		Arrays.fill(data, 1);
		
		List<IBorderSegment> segments = makeTestSegments();

		if(source==DefaultSegmentedProfile.class){
			DefaultSegmentedProfile profile = comp.new DefaultSegmentedProfile(data);
			profile.setSegments(segments);
			return profile;
		}

		if(source==SegmentedFloatProfile.class){
			try {
				return new SegmentedFloatProfile(new FloatProfile(data), segments);
			} catch (ProfileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		throw new Exception("Unable to create instance of "+source);

	}
	
	private static List<IBorderSegment> makeTestSegments() throws ProfileException{
		List<IBorderSegment> list = new ArrayList<>();
		list.add(new DefaultBorderSegment(10, 20, profileLength, UUID.fromString(SEG_0)));
		list.add(new DefaultBorderSegment(20, 100, profileLength, UUID.fromString(SEG_1)));
		list.add(new DefaultBorderSegment(100, 200, profileLength, UUID.fromString(SEG_2)));
		list.add(new DefaultBorderSegment(200, 10, profileLength, UUID.fromString(SEG_3)));
		IBorderSegment.linkSegments(list);
		return list;
	}
		
	@SuppressWarnings("unchecked")
    @Parameters
    public static Iterable<Class> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(
				DefaultSegmentedProfile.class,
				SegmentedFloatProfile.class);
	}
	
	@Test
	public void testReverse() throws Exception {
	    ISegmentedProfile testProfile = createInstance(source);
	    profile.reverse();
	    	    
	    for(UUID id : profile.getSegmentIDs()) {
	    	IBorderSegment fwd = testProfile.getSegment(id);
	    	IBorderSegment rev = profile.getSegment(id);
	    	
	    	assertEquals("Segment length",fwd.length(), rev.length());
	    	
	    	int expStart = profile.size()-1-fwd.getEndIndex();
	    	int expEnd  =  profile.size()-1-fwd.getStartIndex();
	    	
	    	assertEquals("Start index",expStart, rev.getStartIndex());
	    	assertEquals("End index",expEnd, rev.getEndIndex());
	    }
	}
	
	@Test
	public void testGetSegments() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    List<IBorderSegment> result = profile.getSegments();
	    
	    assertEquals(test.size(), result.size());
	    for(int i=0; i<test.size(); i++){
	        assertEquals(test.get(i).toString(), result.get(i).toString());
	    }

	}
	
	@Test
	public void testGetSegmentUUID() throws ProfileException, UnavailableComponentException {
	    List<IBorderSegment> test = makeTestSegments();
	    
	    for(IBorderSegment s : test){
	        IBorderSegment seg = profile.getSegment(s.getID());
	        assertEquals(s.toString(), seg.toString());
	    }
	}
	
	
	@Test
    public void testGetSegmentUUIDExceptsOnInvalidId() throws ProfileException, UnavailableComponentException {
	    UUID notPresent = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(UnavailableComponentException.class);
        IBorderSegment seg = profile.getSegment( notPresent);
    }

	@Test
	public void testHasSegment() throws ProfileException, UnavailableComponentException {
	    List<IBorderSegment> test = makeTestSegments();
        
        for(IBorderSegment s : test){
            IBorderSegment seg = profile.getSegment(s.getID());
            assertEquals(s.toString(), seg.toString());
        }
	}

	@Test
	public void testGetSegmentsFrom() throws Exception {
	    
	    List<IBorderSegment> test = makeTestSegments();
	    List<IBorderSegment> result = profile.getSegmentsFrom(UUID.fromString(SEG_1));
	    assertEquals(test.get(1).toString(), result.get(0).toString());
	}
	
	@Test
    public void testGetSegmentsFromExceptsOnInvalidId() throws Exception {
	    UUID notPresent = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(UnavailableComponentException.class);
        List<IBorderSegment> result = profile.getSegmentsFrom(notPresent);
    }

	@Test
	public void testGetOrderedSegments() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    List<IBorderSegment> result = profile.getOrderedSegments();

	    for(int i=0; i<test.size(); i++){
            assertEquals(test.get(i).toString(), result.get(i).toString());
        }
	}
	
	@Test
	public void testGetOrderedSegmentsSucceedsWhenProfileIsOffsetByOneSegment() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    test.add(test.get(0));
	    test.remove(0);
	    ISegmentedProfile testProfile = profile.offset(20); // Now start on segment 1
	    List<IBorderSegment> result = testProfile.getOrderedSegments();

	    for(int i=0; i<test.size(); i++){
            assertEquals(test.get(i).toString(), result.get(i).toString());
        }
	}

	@Test
	public void testGetSegmentString() throws UnavailableComponentException {
	    IBorderSegment test = new DefaultBorderSegment(10, 20, profileLength, UUID.fromString(SEG_0));
	    String name = "Seg_0";
	    IBorderSegment result = profile.getSegment(name);
	    assertEquals(test.getID(), result.getID());
        assertEquals(test.getStartIndex(), result.getStartIndex());
        assertEquals(test.getEndIndex(), result.getEndIndex());
	}
	
	@Test
    public void testGetSegmentStringExceptsWithInvalidInput() throws UnavailableComponentException{
        exception.expect(UnavailableComponentException.class);
        profile.getSegment("Not present");
    }

	@Test
	public void testGetSegmentIBorderSegment() {
	    IBorderSegment test = new DefaultBorderSegment(10, 20, profileLength, UUID.fromString(SEG_0));
	    IBorderSegment result = profile.getSegment(test);
	    assertEquals(test.getID(), result.getID());
	    assertEquals(test.getStartIndex(), result.getStartIndex());
	    assertEquals(test.getEndIndex(), result.getEndIndex());
	}

	@Test
	public void testGetSegmentAt() {
	    IBorderSegment s = profile.getSegmentAt(1);
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
		IBorderSegment s = profile.getSegmentContaining(0);
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
	    
	    List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(0,  10, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(10, 20, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(20, 60, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(60, 90, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(90, 0,  profile.size(), UUID.randomUUID()));
        IBorderSegment.linkSegments(list);
        System.out.println("Setting segments");
        profile.setSegments(list);
        
        for(int i=0; i<list.size(); i++){
            assertEquals(list.get(i).toString(), profile.getSegmentAt(i).toString());
        }
	}
	
	@Test
	public void testSetWrappingSegments() throws ProfileException {
	    
	    List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(5,  10, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(10, 20, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(20, 60, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(60, 90, profile.size(), UUID.randomUUID()));
        list.add(new DefaultBorderSegment(90, 5,  profile.size(), UUID.randomUUID()));
        IBorderSegment.linkSegments(list);
        System.out.println("Setting segments");
        profile.setSegments(list);
        
        for(int i=0; i<list.size(); i++){
            assertEquals(list.get(i).toString(), profile.getSegmentAt(i).toString());
        }
	}

	@Test
	public void testSetSegmentsExceptsOnEmptyList() {
	    exception.expect(IllegalArgumentException.class);
	    profile.setSegments(new ArrayList<IBorderSegment>(0));
	}
		
	@Test
    public void testSetSegmentsExceptsOnListWithDifferentProfileLength() throws ProfileException {
	    List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(5,  10, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(10, 20, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(20, 60, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(60, 90, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(90, 5,  110, UUID.randomUUID()));
        IBorderSegment.linkSegments(list);
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
	    assertTrue(result.isEmpty());
	}

	@Test
	public void testGetSegmentIDs() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    List<UUID> result = profile.getSegmentIDs();
	    assertEquals(test.size(), result.size());
	    for(int i=0; i<result.size(); i++){
            assertEquals(test.get(i).getID(), result.get(i));
        }
	}
	
	@Test
	public void testGetSegmentCount() throws ProfileException {   
	    List<IBorderSegment> list = makeTestSegments();
	    assertEquals(list.size(), profile.getSegmentCount());  
	}
	
	@Test
	public void testGetDisplacement() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    double d = profile.getDisplacement(s1);
	    assertEquals(0, d, 0);
	}
	
	@Test
    public void testGetDisplacementExceptsOnInvalidSegment() {
	    IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        exception.expect(IllegalArgumentException.class);
        profile.getDisplacement(s1);
    }

	@Test
	public void testContains() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
		assertTrue(profile.contains(s1));
	}
	
	@Test
    public void testContainsReturnsFalseWhenSegmentNotPresent() {
	    IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        assertFalse(profile.contains(s1));
    }
	
//	@Test
//	public void testAdjustSegmentStartPositive() throws SegmentUpdateException {
//	    IBorderSegment s1 = profile.getSegmentAt(1);
//	    int expStart = s1.getStartIndex()+5;
//	    assertTrue(profile.adjustSegmentStart(s1.getID(), 5));  
//	    assertEquals(expStart, s1.getStartIndex());   
//	}
//	
//	@Test
//	public void testAdjustSegmentStartNegative() throws SegmentUpdateException {
//	    IBorderSegment s1 = profile.getSegmentAt(1);
//
//	    int expStart = s1.getStartIndex()-5;
//	    assertTrue(profile.adjustSegmentStart(s1.getID(), -5));
//	    assertEquals(expStart, s1.getStartIndex()); 
//	}
//	
//	@Test
//    public void testAdjustSegmentStartPositiveDoesNothingWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
//        IBorderSegment s1 = profile.getSegmentAt(1);
//        int length = s1.length();
//        int expStart = s1.getStartIndex();
//        exception.expect(SegmentUpdateException.class);
//        profile.adjustSegmentStart(s1.getID(), length-1);
//    }
//	
//	@Test
//    public void testAdjustSegmentStartNegativeDoesNothingWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
//        IBorderSegment s1 = profile.getSegmentAt(1);
//        int length = s1.prevSegment().length();
//        int expStart = s1.getStartIndex();
//        exception.expect(SegmentUpdateException.class);
//        profile.adjustSegmentStart(s1.getID(), -(length-1));
//    }
//		
//	@Test
//    public void testAdjustSegmentStartExceptsWhenIdIsInvalid() throws SegmentUpdateException {
//	    UUID id = UUID.fromString("00000001-1000-1000-1000-100000000000");
//        exception.expect(IllegalArgumentException.class);
//        profile.adjustSegmentStart(id, 5);
//    }
//	
//	@Test
//    public void testAdjustSegmentEndPositive() throws SegmentUpdateException {
//        IBorderSegment s1 = profile.getSegmentAt(1);
//        int exp = s1.getEndIndex()+5;
//        assertTrue(profile.adjustSegmentEnd(s1.getID(), 5));  
//        assertEquals(exp, s1.getEndIndex());   
//    }
//    
//    @Test
//    public void testAdjustSegmentEndNegative() throws SegmentUpdateException {
//        IBorderSegment s1 = profile.getSegmentAt(1);
//
//        int expStart = s1.getEndIndex()-5;
//        assertTrue(profile.adjustSegmentEnd(s1.getID(), -5));
//        assertEquals(expStart, s1.getEndIndex()); 
//    }
//    
//    @Test
//    public void testAdjustSegmentEndPositiveExceptsWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
//        IBorderSegment s1 = profile.getSegmentAt(1);
//        int nextSeglength = s1.nextSegment().length();
//
//        exception.expect(SegmentUpdateException.class);
//        profile.adjustSegmentEnd(s1.getID(), nextSeglength-1);
//    }
//    
//    @Test
//    public void testAdjustSegmentEndNegativeExceptsWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
//        IBorderSegment s1 = profile.getSegmentAt(1);
//        int length = s1.length();
//        int expStart = s1.getEndIndex();
//        exception.expect(SegmentUpdateException.class);
//        profile.adjustSegmentEnd(s1.getID(), -(length-1));
//    }
//        
//    @Test
//    public void testAdjustSegmentEndExceptsWhenIdIsInvalid() throws SegmentUpdateException {
//        UUID id = UUID.fromString("00000001-1000-1000-1000-100000000000");
//        exception.expect(IllegalArgumentException.class);
//        profile.adjustSegmentEnd(id, 5);
//    }
    
    @Test
    public void testNudgeSegmentsWithZeroOffset() throws ProfileException, UnavailableComponentException {
        testSegmentOffset(0);
    }

	@Test
	public void testNudgeSegmentsWithPositiveOffset() throws ProfileException, UnavailableComponentException {
	    testSegmentOffset(10);
	}
	
	@Test
    public void testNudgeSegmentsWithNegativeOffset() throws ProfileException, UnavailableComponentException {
	    testSegmentOffset(-10);
    }
	
	@Test
    public void testNudgeSegmentsWithPositiveOffsetLargerThanProfile() throws ProfileException, UnavailableComponentException {
        testSegmentOffset(profile.size()+1);
    }
	
	@Test
    public void testNudgeSegmentsWithNegativeOffsetLargerThanProfile() throws ProfileException, UnavailableComponentException {
        testSegmentOffset(-(profile.size()+1));
    }
	
	/**
	 * Test if the given offset is successfully applied during a nudge
	 * @param offset
	 * @throws ProfileException
	 * @throws UnavailableComponentException
	 */
	private void testSegmentOffset(int offset) throws ProfileException, UnavailableComponentException{
        List<IBorderSegment> test = makeTestSegments();
        profile.nudgeSegments(offset); 
         for(IBorderSegment s : test){
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
	private boolean segmentHasOffset(IBorderSegment input, IBorderSegment result, int offset){
	    
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
	public void testOffsetOnStatelessProfiles() throws Exception {

		// Test that offsetting the profile offsets each individual segment properly
		for(int i=-profile.size()-1; i<profile.size()*2; i++) {
			System.out.println(String.format("Testing offset of %s", i));
			ISegmentedProfile testProfile = profile.copy().offset(i);

			 List<IBorderSegment> testSegments = testProfile.getSegments();

			 List<IBorderSegment> expectedSegments = makeTestSegments();
			 
			 for(IBorderSegment testSeg : testSegments){
				 UUID segId = testSeg.getID();
				 IBorderSegment expSeg = expectedSegments.stream().filter(s->s.getID().equals(segId)).findFirst().orElseThrow(Exception::new);
				 expSeg.offset(-i);
				 
				 assertEquals(expSeg.toString(), testSeg.toString());
			 }
		}
	}
	
	@Test
	public void testOffsetOnStatefulProfiles() throws Exception {

		// Test that offsetting the profile offsets each individual segment properly.
		// The difference to the test above is that the profile is not explicitly copied.
		// Note that this SHOULD NOT make a difference because ISegmentedProfile::offset()
		// returns a new profile; it does not change the template profile state
		
		for(int i=-profile.size(); i<profile.size()*2; i++) {

			ISegmentedProfile testProfile = profile.offset(i);

			 List<IBorderSegment> testSegments = testProfile.getSegments();

			 List<IBorderSegment> expectedSegments = makeTestSegments();
			 
			 for(IBorderSegment testSeg : testSegments){
				 UUID segId = testSeg.getID();
				 IBorderSegment expSeg = expectedSegments.stream().filter(s->s.getID().equals(segId)).findFirst().orElseThrow(Exception::new);
				 expSeg.offset(-i);
				 
				 if(!expSeg.toString().equals(testSeg.toString())) {
					 System.out.println(String.format("Offset: %s Exp: %s", i, expSeg.getDetail()));
					 System.out.println(String.format("Offset: %s Act: %s", i, testSeg.getDetail()));
				 }
				 assertEquals("Index "+i, expSeg.toString(), testSeg.toString());
			 }

		}
	}
			
	@Test
	public void testFrankenNormaliseToProfileWorksWhenProfilesAreSameLength() throws ProfileException, UnavailableComponentException {
	    
	    ISegmentedProfile test = createTemplateProfile(profile.size());
        ISegmentedProfile result = profile.frankenNormaliseToProfile(test);
        
        assertEquals(result.size(), profile.size());
        
        for(IBorderSegment s : test.getSegments()){
            assertEquals(s.toString(), result.getSegment(s.getID()).toString());
        }
	}
	
	/**
	 * Create a template profile for franken-normalising
	 * @return
	 * @throws ProfileException
	 */
	private ISegmentedProfile createTemplateProfile(int length) throws ProfileException{
        List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(5,  10, length, UUID.fromString(SEG_0)));
        list.add(new DefaultBorderSegment(10, 20, length, UUID.fromString(SEG_1)));
        list.add(new DefaultBorderSegment(20, 60, length, UUID.fromString(SEG_2)));
        list.add(new DefaultBorderSegment(60, 5, length,  UUID.fromString(SEG_3)));
        IBorderSegment.linkSegments(list);
        return new SegmentedFloatProfile(new FloatProfile(10, length), list);
	}
	
	
	@Test
    public void testFrankenNormaliseToProfileExceptsOnDifferentSegmentCountTemplate() throws ProfileException {
        exception.expect(IllegalArgumentException.class);
        profile.frankenNormaliseToProfile( new SegmentedFloatProfile(new FloatProfile(10, 100)));
    }
	
	@Test
    public void testFrankenNormaliseToProfileExceptsOnDifferentSegmentIdTemplate() throws ProfileException {  
	    List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(5,  10, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(10, 20, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(20, 60, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(60, 5, 100,  UUID.randomUUID()));
        IBorderSegment.linkSegments(list);
        exception.expect(IllegalArgumentException.class);
        profile.frankenNormaliseToProfile( new SegmentedFloatProfile(new FloatProfile(10, 100), list));
    }
	
	@Test
	public void testMergeSegments() throws ProfileException, UnavailableComponentException {
	    
	    int expCount = profile.getSegmentCount()-1;
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    IBorderSegment s2 = profile.getSegmentAt(2);
	    
	    profile.mergeSegments(s1, s2, mergedId);

	    IBorderSegment m = profile.getSegment(mergedId);
	    assertEquals("Merged segment start", s1.getStartIndex(), m.getStartIndex());
	    assertEquals("Merged segment end", s2.getEndIndex(), m.getEndIndex());
	    assertEquals("Number of segments", expCount, profile.getSegmentCount());
	}
				
	@Test
    public void testMergeSegmentsExceptsWhenSegment1NotInProfile() throws ProfileException, UnavailableComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        IBorderSegment s2 = profile.getSegmentAt(2);

        exception.expect(IllegalArgumentException.class);
        profile.mergeSegments(s1, s2, mergedId);
    }
	
	@Test
    public void testMergeSegmentsExceptsWhenSegment2NotInProfile() throws ProfileException, UnavailableComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = profile.getSegmentAt(1);
        IBorderSegment s2 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());

        exception.expect(IllegalArgumentException.class);
        profile.mergeSegments(s1, s2, mergedId);
    }

	@Test
	public void testUnmergeSegment() throws ProfileException, UnavailableComponentException {
	    
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = profile.getSegmentAt(1);
        IBorderSegment s2 = profile.getSegmentAt(2);
        
        int expCount = profile.getSegmentCount();
        
        profile.mergeSegments(s1, s2, mergedId);
        IBorderSegment m = profile.getSegment(mergedId);
		profile.unmergeSegment(m);
		
		assertEquals(expCount, profile.getSegmentCount());
		
		assertEquals(s1.toString(), profile.getSegmentAt(1).toString());
		assertEquals(s2.toString(), profile.getSegmentAt(2).toString());
	}

	@Test
	public void testUnmergeSegmentDoesNothingWhenNoMergedSegments() throws UnavailableComponentException, ProfileException {
        IBorderSegment s1 = profile.getSegmentAt(1);   
        profile.unmergeSegment(s1);
	}
		
	@Test
    public void testIsSplittable() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
        assertTrue(profile.isSplittable(s1.getID(), s1.getMidpointIndex()));
    }
	
	@Test
	public void testIsSplittableReturnsFalseWhenTooCloseToStart() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    assertFalse(profile.isSplittable(s1.getID(), s1.getStartIndex()+1));
	}

	@Test
	public void testIsSplittableReturnsFalseWhenTooCloseToEnd() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    assertFalse(profile.isSplittable(s1.getID(), s1.getEndIndex()-1));
	}

	@Test
	public void testIsSplittableReturnsFalseOnOutOfBoundsIndex() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    exception.expect(IllegalArgumentException.class);
	    assertFalse(profile.isSplittable(s1.getID(), s1.getEndIndex()+1));
	}

	@Test
	public void testIsSplittableExceptsOnOutOfBoundsProfileIndex() {
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    exception.expect(IllegalArgumentException.class);
	    profile.isSplittable(s1.getID(), profile.size()+1);
	}

	@Test
	public void testSplitSegment() throws ProfileException {
	    IBorderSegment s1 = profile.getSegmentAt(1);
	    
	    int start = s1.getStartIndex();
	    int mid = s1.getMidpointIndex();
	    int end = s1.getEndIndex();
	    
	    UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
	    
	    profile.splitSegment(s1, mid, id1, id2);
	    
	    IBorderSegment r1 = profile.getSegmentAt(1);
	    IBorderSegment r2 = profile.getSegmentAt(2);
	    
	    assertEquals(id1, r1.getID());
	    assertEquals(id2, r2.getID());
	    
	    assertEquals("r1 start",start, r1.getStartIndex());
	    assertEquals("r1 end", mid, r1.getEndIndex());
	    assertEquals("r2 start", mid, r2.getStartIndex());
	    assertEquals("r2 end", end, r2.getEndIndex());
	}
	
	@Test
    public void testSplitSegmentExceptsOnInvalidIndex() throws ProfileException {
        IBorderSegment s1 = profile.getSegmentAt(1);
        
        int mid = s1.getStartIndex()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        profile.splitSegment(s1, mid, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnIndexOutOfSegmentBounds() throws ProfileException {
        IBorderSegment s1 = profile.getSegmentAt(1);
        
        int mid = profile.size()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        profile.splitSegment(s1, mid, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnInvalidSegment() throws ProfileException {

	    IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
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
	public void testCopy() throws ProfileException {
	    ISegmentedProfile copy = profile.copy();
		assertEquals(profile.toString(), copy.toString());
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

}
