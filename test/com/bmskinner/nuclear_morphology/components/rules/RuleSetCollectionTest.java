package com.bmskinner.nuclear_morphology.components.rules;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.generic.DefaultTag;
import com.bmskinner.nuclear_morphology.components.generic.Tag;

/**
 * Test class for rule set collections
 * @author ben
 * @since 1.18.3
 *
 */
public class RuleSetCollectionTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testBorderTagStoredCorrectly() {
		RuleSetCollection r = new RuleSetCollection();
        r.addRuleSet(Tag.REFERENCE_POINT, RuleSet.roundRPRuleSet());
        List<RuleSet> list = r.getRuleSets(Tag.REFERENCE_POINT);
        assertEquals("Ruleset should be stored for tag correctly", RuleSet.roundRPRuleSet(), list.get(0));
	}
	
	@Test
	public void testDefaultTagStoredCorrectly() {
		RuleSetCollection r = new RuleSetCollection();
		Tag customTag = new DefaultTag("something");
        r.addRuleSet(customTag, RuleSet.roundRPRuleSet());
        List<RuleSet> list = r.getRuleSets(customTag);
        assertEquals("Ruleset should be stored for tag correctly", RuleSet.roundRPRuleSet(), list.get(0));
	}

}
