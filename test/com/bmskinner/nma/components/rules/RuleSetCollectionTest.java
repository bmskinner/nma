package com.bmskinner.nma.components.rules;
//package com.bmskinner.nma.components.rules;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.List;
//
//import org.junit.Before;
//import org.junit.Test;
//
//import com.bmskinner.nma.components.profiles.DefaultLandmark;
//import com.bmskinner.nma.components.profiles.Landmark;
//import com.bmskinner.nma.components.profiles.LandmarkType;
//
///**
// * Test class for rule set collections
// * @author Ben Skinner
// * @since 1.18.3
// *
// */
//public class RuleSetCollectionTest {
//
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@Test
//	public void testBorderTagStoredCorrectly() {
//		RuleSetCollection r = RuleSetCollection.roundRuleSetCollection();
//		
//		for(Landmark l : Landmark.defaultValues()) {
//			List<RuleSet> rsList = r.getRuleSets(l);
//			assertEquals("Ruleset should be stored for tag correctly", RuleSet.roundRPRuleSet(), rsList.get(0));
//		}  
//	}
//	
//	@Test
//	public void testDefaultTagStoredCorrectly() {
//		RuleSetCollection r = new RuleSetCollection();
//		Landmark customTag = new DefaultLandmark("something", LandmarkType.EXTENDED);
//        r.addRuleSet(customTag, RuleSet.roundRPRuleSet());
//        List<RuleSet> list = r.getRuleSets(customTag);
//        assertEquals("Ruleset should be stored for tag correctly", RuleSet.roundRPRuleSet(), list.get(0));
//	}
//
//}
