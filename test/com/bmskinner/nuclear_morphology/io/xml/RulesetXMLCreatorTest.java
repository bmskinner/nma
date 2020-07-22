package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.rules.Rule;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;

/**
 * Test XML documents are created correctly describing 
 * Rulesets
 * @author ben
 * @since 1.18.3
 *
 */
public class RulesetXMLCreatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testRulesetMouseSpermRulesetsCreated() throws IOException {
		List<RuleSet> list = new ArrayList<>();
		list.add(RuleSet.mouseSpermRPRuleSet());
		list.add(RuleSet.mouseSpermOPRuleSet());
		list.add(RuleSet.mouseSpermTVRuleSet());
		list.add(RuleSet.mouseSpermBVRuleSet());
		
		for(RuleSet rs : list) {
			testRuleSetCreated(rs);
		}
	}
	
	@Test
	public void testRulesetPigSpermRulesetsCreated() throws IOException {
		List<RuleSet> list = new ArrayList<>();
		list.add(RuleSet.pigSpermRPRuleSet());
		list.add(RuleSet.pigSpermRPBackupRuleSet());
		list.add(RuleSet.pigSpermOPRuleSet());
		
		for(RuleSet rs : list) {
			testRuleSetCreated(rs);
		}
	}
	
	/**
	 * Given a ruleset, create an XML representation
	 * and test if it matches
	 * @param rs the ruleset to test
	 * @throws IOException
	 */
	private void testRuleSetCreated(RuleSet rs) throws IOException {
		RulesetXMLCreator rxc = new RulesetXMLCreator(rs);		
		Document d = rxc.create();
//		printXML(d);
		testRuleSetMatches(rs, d);
	}
	
	/**
	 * Print the given document to console using a standard formatter
	 * @param d
	 * @throws IOException
	 */
	private void printXML(Document d) throws IOException {
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(d, System.out);
	}
	
	/**
	 * Given a ruleset and an XML document, test if
	 * the document contains the elements in the
	 * ruleset
	 * @param rs
	 * @param d
	 */
	private void testRuleSetMatches(RuleSet rs, Document d) {
		
		// Check fields filled in correctly
		assertEquals(rs.getType().toString(), 
				d.getRootElement().getChild(XMLCreator.PROFILE_TYPE_KEY).getValue());
				
		List<Element> elements = d.getRootElement().getChildren(XMLCreator.RULE_KEY);
		for(int i=0; i<elements.size(); i++) {
			Element rElement = elements.get(i);
			Rule r = rs.getRules().get(i);
			assertEquals(r.getType().toString(),
					rElement.getChild(XMLCreator.TYPE_KEY).getValue());
			for(int j=0; j<r.valueCount(); j++) {
				assertEquals(Double.valueOf(r.getValue(j)),
						Double.valueOf(rElement.getChild(XMLCreator.VALUE_KEY+j).getValue().toString()));
			}
		}
	}

}
